package dev.ferynnd.admbaguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.admbaguslaundry.databinding.CardListRentalInvoiceBinding
import dev.ferynnd.admbaguslaundry.model.ListInvoiceRentalItem
import dev.ferynnd.admbaguslaundry.model.ProductRental
import java.text.NumberFormat
import java.util.Locale

class RentalInvoiceAdapter :
    ListAdapter<ProductRental, RentalInvoiceAdapter.RentalInvoiceViewHolder>(DiffCallback()) {

    private val selectedItems = mutableSetOf<ProductRental>()

    inner class RentalInvoiceViewHolder(val binding: CardListRentalInvoiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RentalInvoiceViewHolder {
        val binding = CardListRentalInvoiceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RentalInvoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RentalInvoiceViewHolder, position: Int) {
        val rental = getItem(position)

        holder.binding.apply {
            inputIdProduct.text = rental.name_rental_item

            val localeID = Locale("in", "ID")
            val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
            val price = rental.price_rental_item
            inputPrice.text = formatRupiah.format(price)

            iconRadio.setOnCheckedChangeListener(null)
            iconRadio.isChecked = selectedItems.contains(rental)

            iconRadio.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedItems.add(rental)
                else selectedItems.remove(rental)
            }
        }
    }

    fun getSelectedInvoiceData(): List<ListInvoiceRentalItem> {
        return selectedItems.map {
            ListInvoiceRentalItem(
                id_item_rental_invoice = it.id_rental_item!!
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProductRental>() {
        override fun areItemsTheSame(oldItem: ProductRental, newItem: ProductRental): Boolean {
            return oldItem.id_rental_item == newItem.id_rental_item
        }

        override fun areContentsTheSame(oldItem: ProductRental, newItem: ProductRental): Boolean {
            return oldItem == newItem
        }
    }
}
