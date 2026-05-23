package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardItemDetailTransactionLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class LaundryTransactionMenuAdapter(
    private val onWeightChanged: (ProductLaundry, BigDecimal) -> Unit
) : ListAdapter<ProductLaundry, LaundryTransactionMenuAdapter.MenuProductLaundryViewHolder>(
    MenuProductLaundryDiffCallback()
) {

    private val numberFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

    inner class MenuProductLaundryViewHolder(
        val binding: CardItemDetailTransactionLaundryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isUpdating = false

        fun bind(product: ProductLaundry) {
            binding.apply {
                textName.text = product.name_laundry_item
                textPrice.text = numberFormatter.format(product.price_laundry_item ?: 0.0)

                // Set weight tanpa trigger listener
                val currentWeight = product.weight?.takeIf { it > BigDecimal.ZERO }?.toString() ?: ""
                if (textWeightItem.text.toString() != currentWeight && !textWeightItem.hasFocus()) {
                    isUpdating = true
                    textWeightItem.setText(currentWeight)
                    isUpdating = false
                }

                // Setup listener untuk weight changes
                textWeightItem.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus && !isUpdating) {
                        validateAndUpdateWeight(product)
                    }
                }
            }
        }

        private fun validateAndUpdateWeight(product: ProductLaundry) {
            val input = binding.textWeightItem.text?.toString()?.trim() ?: ""
            val newWeight = input.toBigDecimalOrNull() ?: BigDecimal.ZERO

            when {
                input.isEmpty() -> {
                    binding.textWeightItem.error = "Berat wajib diisi"
                }
                newWeight <= BigDecimal.ZERO -> {
                    binding.textWeightItem.error = "Berat harus > 0"
                }
                else -> {
                    binding.textWeightItem.error = null
                    if (product.weight != newWeight) {
                        onWeightChanged(product, newWeight)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuProductLaundryViewHolder {
        val binding = CardItemDetailTransactionLaundryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuProductLaundryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuProductLaundryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MenuProductLaundryDiffCallback : DiffUtil.ItemCallback<ProductLaundry>() {
    override fun areItemsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
        return oldItem.id_laundry_item == newItem.id_laundry_item
    }

    override fun areContentsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
        return oldItem.id_laundry_item == newItem.id_laundry_item &&
                oldItem.name_laundry_item == newItem.name_laundry_item &&
                oldItem.price_laundry_item == newItem.price_laundry_item &&
                oldItem.weight == newItem.weight
    }
}