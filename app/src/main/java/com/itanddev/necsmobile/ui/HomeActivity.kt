package com.itanddev.necsmobile.ui

import android.R
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.itanddev.necsmobile.databinding.ActivityHomeBinding
import com.itanddev.necsmobile.data.scanner.BarcodeScanner
import com.opticon.scannersdk.scanner.BarcodeEventListener
import com.opticon.scannersdk.scanner.ReadData
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.itanddev.necsmobile.data.api.RetrofitClient.necsApiService
import com.itanddev.necsmobile.data.model.ConfirmDeliveryRequest
import com.itanddev.necsmobile.data.model.DeliveryHeader
import com.itanddev.necsmobile.data.model.LocationStock
import com.itanddev.necsmobile.data.model.ProductItem
import com.itanddev.necsmobile.data.model.SalesDeliveryDetailLocationsModel
import com.itanddev.necsmobile.data.model.SalesDeliveryDetailModel
import com.itanddev.necsmobile.data.model.SalesDeliveryOrderModel
import com.itanddev.necsmobile.data.model.SaveDeliveryResponse
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

    private lateinit var fabEdit: FloatingActionButton

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
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnConfirm: MaterialButton

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

        fabEdit           = binding.fabEdit

        // ─── FindViewById / Binding ─────────────────────────────────────
        toolbar           = binding.toolbar
        searchContainer   = binding.searchContainer
        dispatchContainer = binding.dispatchContainer

        etManualInvoiceId = binding.etManualInvoiceId
        btnManualSearch   = binding.btnManualSearch
        tvSelectWarehouse = binding.tvSelectWarehouse
        btnSaveDispatch   = binding.btnSaveDispatch
        btnCancel         = binding.btnCancel
        btnConfirm        = binding.btnConfirm

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
                lookupDeliveries(invoiceId)
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
            onSaveDispatch()
        }

        btnConfirm.setOnClickListener {
            onConfirmDispatch()
        }

        btnCancel.setOnClickListener {
            enableEditing(false)
        }

        fabEdit.setOnClickListener {
            val status = currentDeliveryHeader?.status
            if (status == "Nuevo") {
                // Enter edit mode
                enableEditing(true)
            } else {
                Toast.makeText(this,
                    "Solo puede editar cuando el estado es “Nuevo”",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSaveDispatch() {
        // Make sure we have a loaded header
        val header = currentDeliveryHeader
        if (header == null) {
            Toast.makeText(this, "No hay entrega para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) Build the full detail+location lists
        val toSaveDetails = productAdapter.itemList
            .filter { it.quantityDelivery > 0 }
            .map { item ->
                SalesDeliveryDetailModel(
                    salesDeliveryDetailid = item.salesDeliveryDetailid,
                    salesOrderDeliveryId = header.salesDeliveryOrderId,
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

        val toSaveLocs = allLocationEntries.filter { it.quantity > 0 }

        // FULL dispatch: everyone > 0
        AlertDialog.Builder(this)
            .setTitle("Confirmación")
            .setMessage("¿Está seguro que desea guardar los cambios?")
            .setPositiveButton("Sí") { dlg, _ ->
                dlg.dismiss()
                val req = buildSalesDeliveryModel(header, toSaveDetails, toSaveLocs)

//                val gson = GsonBuilder().setPrettyPrinting().create()
//                val prettyJson = gson.toJson(req)

                callSave(req)
            }
            .setNegativeButton("No") { dlg, _ -> dlg.dismiss() }
            .show()
    }

    private fun onConfirmDispatch() {
        val header = currentDeliveryHeader
        if (header == null) {
            showError("No hay entrega para guardar")
            return
        }

        // request1: ALL products (regardless of pending)
        val allDetails = productAdapter.itemList.map { item ->
            SalesDeliveryDetailModel(
                salesDeliveryDetailid  = item.salesDeliveryDetailid,
                salesOrderDeliveryId   = header.salesDeliveryOrderId,
                productId              = item.productId,
                quantityOrder          = item.quantityOrder,
                quantityInvoice        = 0.0,
                quantityDelivery       = item.quantityDelivery,
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
        // include **all** location entries here (including saved and new)
        val allLocs = allLocationEntries.toList()

        // request2: ONLY pending > 0
        val pendingDetails = productAdapter.itemList
            .map { item ->
                // compute pending as (order - delivered)
                val pending = (item.quantityOrder - item.quantityDelivery).coerceAtLeast(0.0)
                item to pending
            }
            .filter { it.second > 0 }
            .map { (item, pending) ->
                SalesDeliveryDetailModel(
                    salesDeliveryDetailid  = item.salesDeliveryDetailid,
                    salesOrderDeliveryId   = header.salesDeliveryOrderId,
                    productId              = item.productId,
                    quantityOrder          = pending,
                    quantityInvoice        = 0.0,
                    quantityDelivery       = 0.0,
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

        // request2 has NO detailLocations
//        val emptyLocs: List<SalesDeliveryDetailLocationsModel>? = null
//        if (pendingDetails.isEmpty()) emptyList() else pendingDetails

        val (title, message) = if (pendingDetails.isEmpty()) {
            "Confirmar despacho" to "¿Está seguro que desea confirmar el despacho?"
        } else {
            "Despacho parcial"    to "Hay productos sin despachar. ¿Continuar con despacho parcial?"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sí") { dlg,_ ->
                dlg.dismiss()
                val req1 = buildSalesDeliveryModel(header, allDetails, allLocs)
                val req2 = buildSalesDeliveryModel(header, pendingDetails, emptyList())

                callConfirmDelivery(req1, req2)
            }
            .setNegativeButton("No", null)
            .show()
    }


    private fun callSave(request: SalesDeliveryOrderModel) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val resp = necsApiService.saveDelivery(request)
                runOnUiThread { handleSaveResponse(resp) }
            } catch (e: Exception) {
                runOnUiThread { showError("Network error") }
            } finally {
                runOnUiThread { showLoading(false) }
            }
        }
    }

    private fun callConfirmDelivery(req1: SalesDeliveryOrderModel, req2: SalesDeliveryOrderModel) {
        val wrapper = ConfirmDeliveryRequest(model = req1, model2 = req2)

        val gson = GsonBuilder().setPrettyPrinting().create()
        val prettyJson1 = gson.toJson(req1)
        val prettyJson2 = gson.toJson(req2)
        val prettyJson3 = gson.toJson(wrapper)

        showLoading(true)
        lifecycleScope.launch {
            try {
                val resp = necsApiService.confirmDelivery(wrapper)
                runOnUiThread { handleSaveResponse(resp) }
            } catch (e: Exception) {
                runOnUiThread { showError("Network error") }
            } finally {
                runOnUiThread { showLoading(false) }
            }
        }
    }

    private fun handleSaveResponse(response: Response<SaveDeliveryResponse>) {
        if (response.isSuccessful) {
            response.body()?.let {
                if (it.type == "success") {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG)
                        .show()

                    // 1) Re-fetch this same delivery’s details
                    currentDeliveryHeader?.salesDeliveryOrderId?.let { id ->
                        fetchDeliveryDetailById(id)
                    }
                    // 2) Ensure all controls are locked again
                    enableEditing(false)
                }
                else {
                    Toast.makeText(this, "${it.type} ${it.message}", Toast.LENGTH_LONG)
                        .show()
                }
            } ?: showError("Respuesta vacía")
        } else {
            showError("Error: ${response.code()}")
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

        fabEdit.visibility = View.GONE
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

        enableEditing(false)
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

    private fun enableEditing(on: Boolean) {
        // If we’re *entering* edit mode:
        if (on) {
            fabEdit.visibility         = View.GONE
            btnSaveDispatch.visibility = View.VISIBLE
            btnCancel.visibility       = View.VISIBLE
            btnConfirm.visibility      = View.GONE
        } else {
            // Exiting edit mode (or initial load)
            fabEdit.visibility         = View.VISIBLE
            btnSaveDispatch.visibility = View.GONE
            btnCancel.visibility       = View.GONE

            val isNuevo = currentDeliveryHeader?.status == "Nuevo"
            btnConfirm.visibility = if (isNuevo) View.VISIBLE else View.GONE
        }

        // Enable/disable controls
//        binding.btnScan.isEnabled         = on
//        binding.btnManualSearch.isEnabled = on
        tvSelectWarehouse.isEnabled       = on

        // Tell the adapter whether its “Despachar” buttons are live:
        productAdapter.editingEnabled = on
        productAdapter.notifyDataSetChanged()
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
                    val raw: List<LocationStock> = Gson().fromJson(
                        listJson,
                        object : TypeToken<List<LocationStock>>() {}.type
                    )
                    raw.forEach { it.salesDeliveryDetailid = product.salesDeliveryDetailid }

                    val existing = perProductLocationMap[product.productId]  // might be null
                    val locations = raw.map { stock ->
                        val prev = existing?.firstOrNull { it.locationId == stock.locationId }
                        if (prev != null) {
                            stock.copy(
                                deliveryLocationId = prev.deliveryLocationId,
                                quantityDelivery   = prev.quantityDelivery
                            )
                        } else stock
                    }

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

        bsBinding.bsBtnAccept.setOnClickListener {

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
                        deliveryLocationId      = locStock.deliveryLocationId,
                        salesOrderDeliveryId    = currentDeliveryHeader!!.salesDeliveryOrderId,
                        salesDeliveryDetailid   = locStock.salesDeliveryDetailid,
                        wharehouseId            = locStock.wharehouseId,
                        locationId              = locStock.locationId,
                        productId               = locStock.productId,
                        quantity                = locStock.quantityDelivery
                    )
                }

            val sumForThisProduct = newLocationEntries.sumOf { it.quantity }
            // find the ProductItem’s order quantity
            val originalOrderQty = productAdapter.itemList
                .first { it.productId == productId }
                .quantityOrder

            if (sumForThisProduct > originalOrderQty) {
                Toast.makeText(
                    this@HomeActivity,
                    "No puede despachar más de la cantidad ordenada ($originalOrderQty)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
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

    private fun buildSalesDeliveryModel(
        header: DeliveryHeader,
        details: List<SalesDeliveryDetailModel>,
        locations: List<SalesDeliveryDetailLocationsModel>?
    ): SalesDeliveryOrderModel {
        return SalesDeliveryOrderModel(
            salesDeliveryOrderId = header.salesDeliveryOrderId,
            deliveryNumber       = header.deliveryNumber,
            salesQuoteId         = 0,
            salesinInvoceId      = 0,
            dateCreated          = header.dateCreated,
            createdBy            = 0,
            deliveryDate         = "",
            customerId           = 0,
            enterpriseId         = 0,
            branchId             = header.branchId,
            docType              = "",
            customerName         = header.customerName,
            soNumber             = header.soNumber,
            status               = header.status,
            soStatus             = "",
            salesInvoiceID       = header.salesInvoiceID,
            wharehouseId         = 0,
            hasBackOrder         = false,
            detail               = details,
            detailLocations      = locations
        )
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

    private fun lookupDeliveries(invoiceId: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // ① Fetch all deliveries for this invoice
                val resp = necsApiService.getDeliveriesByInvoice(invoiceId)
                if (!resp.isSuccessful) {
                    runOnUiThread {
                        showLoading(false)
                        showError("Error fetching deliveries: ${resp.code()}")
                    }
                    return@launch
                }

                val json = resp.body()?.string() ?: "[]"
                val deliveries: List<SalesDeliveryOrderModel> = Gson().fromJson(
                    json, object : TypeToken<List<SalesDeliveryOrderModel>>() {}.type
                )

                runOnUiThread {
                    showLoading(false)
                    when {
                        deliveries.isEmpty() -> {
                            showError("No se encontraron entregas para esa factura")
                        }
                        deliveries.size == 1 -> {
                            // Only one: jump straight to detail
                            fetchDeliveryDetailById(deliveries[0].salesDeliveryOrderId)
                        }
                        else -> {
                            // Many: show dialog to choose
                            showDeliverySelectionDialog(deliveries)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    showError("Network error")
                }
            }
        }
    }

    private fun showDeliverySelectionDialog(list: List<SalesDeliveryOrderModel>) {
        // Build array of strings “DELIVER_NUMBER (status)”
        val labels = list.map { "${it.deliveryNumber} - (${it.status})" }.toTypedArray()
        var selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Seleccione entrega")
            .setSingleChoiceItems(labels, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val chosen = list[selectedIndex]
                fetchDeliveryDetailById(chosen.salesDeliveryOrderId)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun fetchDeliveryDetailById(deliveryId: Int) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // This hits getDeliveryById, returns the same full DeliveryHeader JSON
                val resp = necsApiService.getDeliveryById(deliveryId.toString())
                runOnUiThread {
                    showLoading(false)
                    if (resp.isSuccessful) {
                        val body = resp.body()?.string()
                        body?.let {
                            val delivery = Gson().fromJson(it, DeliveryHeader::class.java)
                            currentDeliveryHeader = delivery

                            allLocationEntries.clear()
                            allLocationEntries.addAll(delivery.detailLocations)

                            perProductLocationMap.clear()
                            delivery.detailLocations
                                .groupBy { it.productId }
                                .forEach { (pid, locs) ->
                                    perProductLocationMap[pid] = locs.map { loc ->
                                        // convert the back‐end DTO into a LocationStock so you can pre-fill the sheet
                                        LocationStock(
                                            productId               = loc.productId,
                                            wharehouseId            = loc.wharehouseId,
                                            locationId              = loc.locationId,
                                            deliveryLocationId      = loc.deliveryLocationId,
                                            salesDeliveryDetailid   = loc.salesDeliveryDetailid,
                                            wharehouseName          = "",
                                            locationName            = "",
                                            productName             = "",
                                            quantityStock           = 0.0,
                                            quantityDelivery        = loc.quantity
                                        )
                                    }
                                }

                            updateDeliveryUI(delivery)
                            productAdapter.submitList(delivery.detail)
                            showDispatchView()
                        }
                    } else {
                        showError("Error fetching detalle: ${resp.code()}")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    showError("Network error")
                }
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
            tvCustomerName.text = delivery.customerName
            tvSONumber.text = "Orden: ${delivery.soNumber}"
            tvDeliveryDate.text = formatDate(delivery.dateCreated)

            delivery.branchId?.let { branchId ->
                loadWarehouses(branchId.toString())
            }
        }

        val chip = binding.chipStatus
        chip.text = delivery.status

        when (delivery.status) {
            "Confirmado" -> {
                chip.chipIconTint   = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.holo_green_light))
                chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.holo_green_dark))
            }
            "Nuevo" -> {
                chip.chipIconTint   = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.holo_blue_light))
                chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.holo_blue_dark))
            }
//            else -> {
//                chip.chipIconTint   = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.darker_gray)
//                chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.darker_gray))
//            }
        }

        val isNuevo = delivery.status == "Nuevo"
        btnConfirm.visibility     = if (isNuevo) View.VISIBLE else View.GONE
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
            lookupDeliveries(readData.text)
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