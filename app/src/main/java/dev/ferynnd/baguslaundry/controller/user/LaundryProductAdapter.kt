package dev.ferynnd.baguslaundry.controller.user

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.KurirCardProductLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry

class LaundryProductAdapter(
    private val onItemClick: (ProductLaundry) -> Unit
) : ListAdapter<ProductLaundry, LaundryProductAdapter.ProductLaundryViewHolder>(DiffCallback()) {

    inner class ProductLaundryViewHolder(val binding: KurirCardProductLaundryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val nama_laundry = binding.namaProductLaundry
        val harga_waktu = binding.hargaWaktuProductLaundry
        val deskripsi = binding.deskripsiProductLaundry

        init {
            itemView.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductLaundryViewHolder {
        val binding = KurirCardProductLaundryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductLaundryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductLaundryViewHolder, position: Int) {
        val laundryItem = getItem(position)
        var harga_laundry = laundryItem.price_laundry_item.toString()
        var waktu_laundry = laundryItem.time_laundry_item.toString()

        holder.nama_laundry.text = laundryItem.name_laundry_item.toString()
        holder.harga_waktu.text = "$harga_laundry - $waktu_laundry"
        holder.deskripsi.text = laundryItem.description_laundry_item.toString()

        holder.itemView.setOnClickListener {
            onItemClick(laundryItem)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProductLaundry>() {
        override fun areItemsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
            return oldItem.id_laundry_item == newItem.id_laundry_item
        }

        override fun areContentsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
            return oldItem == newItem
        }
    }


}