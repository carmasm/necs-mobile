package com.itanddev.necsmobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.itanddev.necsmobile.data.model.LocationStock
import com.itanddev.necsmobile.databinding.ItemLocationStockBinding

class LocationStockAdapter(
    private val items: List<LocationStock>
) : RecyclerView.Adapter<LocationStockAdapter.VH>() {

    inner class VH(val binding: ItemLocationStockBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            // listen for input changes
            binding.etQuantityDelivery.doAfterTextChanged { text ->
                val value = text.toString().toDoubleOrNull() ?: 0.0
                items[adapterPosition].quantityDelivery = value
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLocationStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        ))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val loc = items[position]
        with(holder.binding) {
            tvWarehouse.text   = loc.wharehouseName
            tvLocation.text    = loc.locationName
            tvProductName.text = loc.productName
            tvStock.text       = loc.quantityStock.toString()
            etQuantityDelivery.setText(
                if (loc.quantityDelivery > 0) loc.quantityDelivery.toString() else ""
            )
        }
    }

    override fun getItemCount() = items.size
}
