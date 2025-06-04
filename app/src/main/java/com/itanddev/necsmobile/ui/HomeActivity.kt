package com.itanddev.necsmobile.ui

import android.R
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.itanddev.necsmobile.databinding.ActivityHomeBinding
import com.itanddev.necsmobile.data.scanner.BarcodeScanner
import com.opticon.scannersdk.scanner.BarcodeEventListener
import com.opticon.scannersdk.scanner.ReadData
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.itanddev.necsmobile.data.api.RetrofitClient.necsApiService
import com.itanddev.necsmobile.data.model.DeliveryHeader
import com.itanddev.necsmobile.data.model.Invoice
import com.itanddev.necsmobile.data.model.LocationStock
import com.itanddev.necsmobile.data.model.ProductItem
import com.itanddev.necsmobile.data.model.SalesDeliveryDetailLocationsModel
import com.itanddev.necsmobile.data.model.SalesDeliveryDetailModel
import com.itanddev.necsmobile.data.model.SalesDeliveryOrderModel
import com.itanddev.necsmobile.data.model.Warehouse
import com.itanddev.necsmobile.databinding.BottomSheetLocationsBinding
import kotlinx.coroutines.launch
import retrofit2.Response
import okhttp3.ResponseBody
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class HomeActivity : AppCompatActivity(), BarcodeEventListener {
    private lateinit var binding: ActivityHomeBinding

    // Containers
    private lateinit var searchContainer: View
    private lateinit var dispatchContainer: View

    // Toolbar
    private lateinit var toolbar: Toolbar

    // Search/Scan UI
    private lateinit var etManualInvoiceId: TextInputEditText
    private lateinit var btnManualSearch: MaterialButton

    // Dispatch UI
    private lateinit var tvSelectWarehouse: TextView
    private lateinit var btnSaveDispatch: MaterialButton

    // RecyclerView adapter
    private lateinit var productAdapter: ProductAdapter

    // Data
    private lateinit var warehouses: List<Warehouse>
    private var selectedWarehouseIndex: Int = -1
    private var currentDeliveryId: Int? = null
    private var currentDeliveryHeader: DeliveryHeader? = null // Keep a reference to the entire DeliveryHeader (so we can read branchId, etc.)
    private val perProductLocationMap = mutableMapOf<Int, List<LocationStock>>() // Map each productId → LocationStock list that was entered in the bottom sheet
    private val allLocationEntries = mutableListOf<SalesDeliveryDetailLocationsModel>() // Also keep a flattened list of SaveLocationEntry for *all* products

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ─── FindViewById / Binding ─────────────────────────────────────
        toolbar           = binding.toolbar
        searchContainer   = binding.searchContainer
        dispatchContainer = binding.dispatchContainer

        etManualInvoiceId = binding.etManualInvoiceId
        btnManualSearch   = binding.btnManualSearch
        tvSelectWarehouse = binding.tvSelectWarehouse
        btnSaveDispatch   = binding.btnSaveDispatch

        // Set up Toolbar as ActionBar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Buscar / Escanear"
            setDisplayHomeAsUpEnabled(false)  // initially no Up arrow
        }

        setupRecyclerView()
        setupUi()
        setupScanner()

        // Show search container by default
        showSearchView()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            handleDispatchClick(product)
        }
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = productAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupUi() {
        // ─── SCAN + SEARCH UI ────────────────────────────────────────
        binding.btnScan.setOnClickListener {
            startScanning()
        }

        btnManualSearch.setOnClickListener {
            val invoiceId = etManualInvoiceId.text.toString().trim()
            if (invoiceId.isNotEmpty()) {
                callNecsApi(invoiceId)
            } else {
                etManualInvoiceId.error = "Por favor ingrese un ID de factura"
            }
        }

        // ─── WAREHOUSE SELECT (in dispatchContainer) ──────────────
        tvSelectWarehouse.setOnClickListener {
            if (::warehouses.isInitialized && warehouses.isNotEmpty()) {
                showWarehouseDialog()
            } else {
                Toast.makeText(this, "Cargando almacenes...", Toast.LENGTH_SHORT).show()
            }
        }

        // ─── BACK (Up) BUTTON in Toolbar ───────────────────────────
        // We override onOptionsItemSelected() to catch android.R.id.home

        // ─── SALVAR (in dispatchContainer) ───────────────────────
        btnSaveDispatch.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Confirmación")
                .setMessage("¿Está seguro que desea salvar los datos?")
                .setPositiveButton("Sí") { dialog, _ ->
                    dialog.dismiss()
                    saveDelivery()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun saveDelivery() {
        // Make sure we have a loaded header
        val header = currentDeliveryHeader
        if (header == null) {
            Toast.makeText(this, "No hay entrega para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        // Build the “detail” array from productAdapter.currentList
        val detailList = productAdapter.itemList.map { item ->
            SalesDeliveryDetailModel(
                salesDeliveryDetailid = item.salesDeliveryDetailid,
                salesOrderDeliveryId = 0,
                productId = item.productId,
                quantityOrder = item.quantityOrder,
                quantityInvoice = 0.0,
                quantityDelivery = item.quantityDelivery,
                wharehouseName = "",
                locationName = "",
                deteDelivery = "",
                wharehouseId = 0,
                locationId = 0,
                productName = item.productName,
                productBarCode = item.productBarCode,
                cost = 0.0,
                quantityCheck = 0.0,
                image = "",
                isLoan = 0,
                quantityStock = 0.0
            )
        }

        // Now build the whole SaveDeliveryRequest
        val saveRequest = SalesDeliveryOrderModel(
            salesDeliveryOrderId = header.salesDeliveryOrderId,
            deliveryNumber = header.deliveryNumber,
            salesQuoteId = 0,
            salesinInvoceId = 0,
            dateCreated = header.dateCreated,
            createdBy = 0,
            deliveryDate = "",
            customerId = 0,
            enterpriseId = 0,
            branchId = header.branchId,
            docType = "",
            customerName = header.customerName,
            soNumber = header.soNumber,
            status = header.status,
            soStatus = "",
            salesInvoiceID = 0,
            wharehouseId = 0,
            hasBackOrder = false,
            detail = detailList,
            detailLocations = allLocationEntries
        )

        // Turn it into nicely‐formatted JSON with Gson’s PrettyPrinter:
        val gson = GsonBuilder().setPrettyPrinting().create()
        val prettyJson = gson.toJson(saveRequest)

        // Call PUT in a coroutine
        showLoading(true)   // reuse your existing showLoading to block UI
        lifecycleScope.launch {
            try {
                val response = necsApiService.saveDelivery(saveRequest)
                runOnUiThread {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            // Show whatever message comes back
                            if (body.type == "success") {
                                Toast.makeText(this@HomeActivity, body.message, Toast.LENGTH_LONG)
                                    .show()

                                resetToSearchState()
                            }
                            else {
                                Toast.makeText(this@HomeActivity, "${body.type} ${body.message}", Toast.LENGTH_LONG)
                                    .show()
                            }
                        } else {
                            Toast.makeText(this@HomeActivity, "Respuesta vacía", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            } finally {
                runOnUiThread { showLoading(false) }
            }
        }
    }

    // Show only the search screen, hide dispatch details, disable Up arrow
    private fun showSearchView() {
        searchContainer.visibility   = View.VISIBLE
        dispatchContainer.visibility = View.GONE
        binding.tvScanPrompt.visibility = View.VISIBLE

        supportActionBar?.apply {
            title = "Buscar / Escanear"
            setDisplayHomeAsUpEnabled(false)
        }
    }

    // Show only the dispatch details screen, hide search, enable Up arrow
    private fun showDispatchView() {
        searchContainer.visibility   = View.GONE
        dispatchContainer.visibility = View.VISIBLE
        binding.tvScanPrompt.visibility = View.GONE

        supportActionBar?.apply {
            title = "Resumen de Despacho"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun showWarehouseDialog() {
        // Build an array of names from your warehouses list:
        val names = warehouses.map { it.name }.toTypedArray()

        // Pre‐select the last chosen index, or 0 if none yet
        val initialIndex = if (selectedWarehouseIndex in names.indices) selectedWarehouseIndex else 0

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Seleccione Almacén")
            .setSingleChoiceItems(names, initialIndex) { dialog, which ->
                // User tapped on “which” line
                selectedWarehouseIndex = which
            }
            .setPositiveButton("OK") { dialog, _ ->
                // When they press OK, update the TextView to show the chosen name
                if (selectedWarehouseIndex in names.indices) {
                    tvSelectWarehouse.text = names[selectedWarehouseIndex]
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }


    private fun handleDispatchClick(product: ProductItem) {
        // 1️⃣ Gather parameters
        val pid        = product.productId.toString()
        val warehouse  = getSelectedWarehouse()
        if (warehouse == null) {
            Toast.makeText(this, "Seleccione un almacén primero", Toast.LENGTH_SHORT).show()
            return
        }
        val wId        = warehouse?.warehouseId?.toString() ?: return
        val deliveryId = currentDeliveryId  // save this in a field when you load header

        showLoading(true)
        lifecycleScope.launch {
            try {
                val resp = necsApiService.getProductLocationStock(pid, wId, deliveryId)
                if (resp.isSuccessful) {
                    val listJson = resp.body()?.string() ?: "[]"
                    val locations: List<LocationStock> = Gson().fromJson(
                        listJson,
                        object : TypeToken<List<LocationStock>>() {}.type
                    )
                    showLocationBottomSheet(locations)
                } else {
                    showError("Error: ${resp.code()}")
                }
            } catch(e: Exception) {
                showError("Network error")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLocationBottomSheet(locations: List<LocationStock>) {
        val sheet = BottomSheetDialog(this)
        val bsBinding = BottomSheetLocationsBinding.inflate(layoutInflater)
        sheet.setContentView(bsBinding.root)

        val adapter = LocationStockAdapter(locations)
        bsBinding.bsRvLocations.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            this.adapter = adapter
        }

        bsBinding.bsBtnSave.setOnClickListener {

            // Remove all previous entries for this product from allLocationEntries
            // (so we don’t double-count if the user reopened the sheet)
            val productId = locations.firstOrNull()?.productId
            if (productId != null) {
                allLocationEntries.removeAll { it.productId == productId }
            }

            // Collect only those locations where user entered quantityDelivery > 0
            // and map into SaveLocationEntry
            val newLocationEntries = locations
                .filter { it.quantityDelivery > 0.0 }
                .map { locStock ->
                    SalesDeliveryDetailLocationsModel(
                        deliveryLocationId      = -1, // new entry
                        salesOrderDeliveryId    = currentDeliveryHeader!!.salesDeliveryOrderId,
                        salesDeliveryDetailid   = locStock.salesDeliveryDetailid,
                        wharehouseId            = locStock.wharehouseId,
                        locationId              = locStock.locationId,
                        productId               = locStock.productId,
                        quantity                = locStock.quantityDelivery
                    )
                }

            // Add them to the global list
            allLocationEntries.addAll(newLocationEntries)

            // Update the ProductItem’s `quantityDelivery` to the sum of all location entries
            val totalForProduct = newLocationEntries.sumOf { it.quantity }

            //    Find the matching ProductItem in productAdapter’s list and update it
            val pos = productAdapter.itemList.indexOfFirst { it.productId == productId }
            if (pos >= 0) {
                productAdapter.itemList[pos].quantityDelivery = totalForProduct
                productAdapter.notifyItemChanged(pos)
            }

            // Also store this location list in perProductLocationMap
            productId?.let {
                perProductLocationMap[productId] = locations
            }

            sheet.dismiss()
        }

        sheet.show()
    }


    private fun startScanning() {
        if (BarcodeScanner.scanner?.isConnected == true) {
            BarcodeScanner.startScanning()
            binding.btnScan.text = "Scanning..."
        } else {
            Toast.makeText(this, "Scanner not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupScanner() {
        BarcodeScanner.initialize(this)
        BarcodeScanner.scanner?.addBarcodeEventListener(this)
    }

    private fun callNecsApi(invoiceId: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = necsApiService.getDeliveryDetailByInvoice(invoiceId)
                handleApiResponse(response)
            } catch (e: Exception) {
                handleApiError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleApiResponse(response: Response<ResponseBody>) {
        runOnUiThread {
            if (response.isSuccessful) {
                val jsonString = response.body()?.string()
                jsonString?.let {
                    try {
                        val delivery = Gson().fromJson(it, DeliveryHeader::class.java)

                        currentDeliveryHeader = delivery

                        updateDeliveryUI(delivery)
                        productAdapter.submitList(delivery.detail)

                        // Now switch to the dispatch screen
                        showDispatchView()
                    } catch (e: Exception) {
                        showError("Error parsing delivery data")
                    }
                }
            } else {
                showError("API Error: ${response.code()}")
            }
        }
    }

    private fun updateDeliveryUI(delivery: DeliveryHeader) {
        currentDeliveryId = delivery.salesDeliveryOrderId // Make sure this field exists in your model

        with(binding) {
            tvDeliveryNumber.text = "Entrega #${delivery.deliveryNumber}"
            tvStatus.text = delivery.status
            tvCustomerName.text = delivery.customerName
            tvSONumber.text = "Orden: ${delivery.soNumber}"
            tvDeliveryDate.text = formatDate(delivery.dateCreated)

            delivery.branchId?.let { branchId ->
                loadWarehouses(branchId.toString())
            }
        }
    }

    private fun resetToSearchState() {
        // 1) Clear the “currentDeliveryHeader”
        currentDeliveryHeader = null

        // 2) Clear the product list in the adapter
        productAdapter.submitList(emptyList())

        // 3) Clear all-location entries
        allLocationEntries.clear()
        perProductLocationMap.clear()

        // 4) Clear the “Invoice ID” input
        etManualInvoiceId.text?.clear()

        // 5) Show the scan prompt again
        binding.tvScanPrompt.visibility = View.VISIBLE

        // 6) Switch back to the search container (hides dispatch container & toolbar Up arrow)
        showSearchView()
    }

    private fun loadWarehouses(branchId: String) {
        lifecycleScope.launch {
            try {
                val response = necsApiService.getWarehouses(branchId)
                if (response.isSuccessful) {
                    response.body()?.let { warehousesJson ->
                        warehouses = Gson().fromJson(
                            warehousesJson.string(),
                            object : TypeToken<List<Warehouse>>() {}.type
                        )

                        // Default to first warehouse if available
                        if (warehouses.isNotEmpty()) {
                            selectedWarehouseIndex = 0
                            tvSelectWarehouse.text = warehouses[0].name
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity,
                    "Error loading warehouses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSelectedWarehouse(): Warehouse? {
        return if (::warehouses.isInitialized
            && selectedWarehouseIndex in warehouses.indices
        ) {
            warehouses[selectedWarehouseIndex]
        } else {
            null
        }
    }

//    private fun displayInvoiceInfo(jsonString: String) {
//        try {
//            val invoice = Gson().fromJson(jsonString, Invoice::class.java)
//
//            with(binding) {
//                tvCustomerName.text = invoice.customerName
//                tvTotal.text = "Total: ${formatCurrency(invoice.total)}"
//                tvStatus.text = "Status: ${invoice.status}"
//                tvInvoiceDate.text = "Date: ${formatDate(invoice.invoiceDate)}"
//            }
//        } catch (e: Exception) {
//            showError("Error parsing response")
//        }
//    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun handleApiError(e: Exception) {
        runOnUiThread {
            showError("Error: ${e.localizedMessage}")
//            binding.tvApiResponse.text = e.stackTraceToString()
        }
    }

    private fun showLoading(show: Boolean) {
        runOnUiThread {
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            binding.btnScan.isEnabled = !show
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ─── Handle Toolbar Up arrow press ─────────────────────────────────
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // User tapped the Up arrow
                showSearchView()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@HomeActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    // ─── BarcodeEventListener implementations ─────────────────────────
    override fun onResume() {
        super.onResume()
        BarcodeScanner.scanner?.init()
    }

    override fun onPause() {
        super.onPause()
        BarcodeScanner.scanner?.deinit()
    }

    override fun onDestroy() {
        super.onDestroy()
        BarcodeScanner.release()
    }

    // BarcodeEventListener implementation
    override fun onReadData(readData: ReadData) {
        val scannedText = readData.text

        runOnUiThread {
//            binding.tvScanResult.text = "Scanned: ${readData.text}"
            binding.btnScan.text = "Start Scanning"
            callNecsApi(scannedText)
        }
    }

    override fun onTimeout() {
        runOnUiThread {
            binding.btnScan.text = "Start Scanning"
            Toast.makeText(this, "Scan timed out", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDecodeStart() {
        runOnUiThread {
            binding.btnScan.text = "Scanning..."
        }
    }

    override fun onDecodeStop() {
        runOnUiThread {
            binding.btnScan.text = "Start Scanning"
        }
    }

    // Other required interface methods (empty implementations)
    override fun onConnect() {}
    override fun onDisconnect() {}
    override fun onImageData(bitmap: Bitmap?, byteArray: ByteArray?, i: Int) {}
}