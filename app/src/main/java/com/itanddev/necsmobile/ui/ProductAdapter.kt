package com.itanddev.necsmobile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.itanddev.necsmobile.R
import com.itanddev.necsmobile.data.model.ProductItem

class ProductAdapter(
    private val onDispatchClick: (ProductItem) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    var editingEnabled = false

    private val items = mutableListOf<ProductItem>()
    val itemList: List<ProductItem>
        get() = items

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBarcode: TextView = view.findViewById(R.id.tvProductBarcode)
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvQuantityDelivery: TextView = view.findViewById(R.id.tvQuantityDelivery)
        val tvQuantityPending: TextView  = view.findViewById(R.id.tvQuantityPending)
        val btnDispatch: MaterialButton = view.findViewById(R.id.btnDispatch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvBarcode.text = item.productBarCode
        holder.tvName.text = item.productName
        holder.tvQuantity.text = "Cantidad:       ${item.quantityOrder}"
        holder.tvQuantityDelivery.text = "Despachado:   ${item.quantityDelivery}"

        val pending = (item.quantityOrder - item.quantityDelivery).coerceAtLeast(0.0)
        holder.tvQuantityPending.text = "Pendiente:       $pending"

        holder.btnDispatch.isEnabled = editingEnabled

        holder.btnDispatch.setOnClickListener {
            onDispatchClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<ProductItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}