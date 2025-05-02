package dev.ferynnd.baguslaundry.controller.user


import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardProductRentalBinding
import dev.ferynnd.baguslaundry.model.ProductRental
import java.text.NumberFormat
import java.util.Locale

class RentalProductAdapter (
    private val onItemClick: (ProductRental) -> Unit
) : ListAdapter<ProductRental, RentalProductAdapter.ProductRentalViewHolder>(DiffCallback()) {

    inner class ProductRentalViewHolder(val binding: CardProductRentalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val id_nama_rental = binding.numberNamaProductRental
        val status = binding.statusProductRental
        val condition = binding.conditionProductRental
        val deskripsi = binding.deskirpiProductLaundry
        val price = binding.priceProductLaundry

        init {
            itemView.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductRentalViewHolder {
        val binding = CardProductRentalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductRentalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductRentalViewHolder, position: Int) {
        val laundryItem = getItem(position)
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        val nama_rental = laundryItem.name_rental_item.toString()
        val number_rental = laundryItem.number_rental_item.toString()
        val status = laundryItem.status_rental_item.toString()
        val kondisi = laundryItem.condition_rental_item.toString()

        holder.id_nama_rental.text = "$number_rental - $nama_rental"
        holder.price.text = numberFormat.format(laundryItem.price_rental_item)

        when (status) {
            "available" -> {
                holder.status.text = "TERSEDIA"
            }
            "rented" -> {
                holder.status.text = "DISEWA"
            }
            "maintenance" -> {
                holder.status.text = "PEMELIHARAAN"
            }
        }

        when (kondisi) {
            "clean" -> {
                holder.condition.text = "BERSIH"
            }
            "dirty" -> {
                holder.condition.text = "KOTOR"
            }
            "damaged" -> {
                holder.condition.text = "RUSAK"
            }
        }

        holder.deskripsi.text = laundryItem.description_rental_item.toString()

        holder.itemView.setOnClickListener {
            onItemClick(laundryItem)
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