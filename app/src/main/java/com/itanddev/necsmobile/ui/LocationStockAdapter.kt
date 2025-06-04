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
            // 1) When the user types manually, keep quantityDelivery in sync:
            binding.etQuantityDelivery.doAfterTextChanged { text ->
                val value = text.toString().toDoubleOrNull() ?: 0.0
                items[adapterPosition].quantityDelivery = value
            }

            // 2) Decrement button
            binding.btnDecrement.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                val loc = items[pos]
                // Subtract 1.0 but don't go below zero:
                val newVal = (loc.quantityDelivery - 1.0).coerceAtLeast(0.0)
                loc.quantityDelivery = newVal
                // Update the EditText text (this will NOT re–trigger doAfterTextChanged infinitely
                // because we’re setting it programmatically to the same formatted string).
                binding.etQuantityDelivery.setText(
                    if (newVal > 0.0) newVal.toString() else ""
                )
            }

            // 3) Increment button
            binding.btnIncrement.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                val loc = items[pos]
                // Add 1.0
                val newVal = loc.quantityDelivery + 1.0
                loc.quantityDelivery = newVal
                binding.etQuantityDelivery.setText(newVal.toString())
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
            tvWarehouse.text   = "Almacén: ${loc.wharehouseName}"
            tvLocation.text    = "Ubicación: ${loc.locationName}"
            tvProductName.text = loc.productName
            tvStock.text       = "Existencia: ${loc.quantityStock}"
            etQuantityDelivery.setText(
                if (loc.quantityDelivery > 0) loc.quantityDelivery.toString() else ""
            )
        }
    }

    override fun getItemCount() = items.size
}
