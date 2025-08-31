package dev.ferynnd.baguslaundry.controller.user

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.KurirCardProductLaundryBinding
import dev.ferynnd.baguslaundry.databinding.KurirCardProductRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.ui.user.CreateItemLaundryFragment
import dev.ferynnd.baguslaundry.ui.user.CreateItemRentalFragment


class KurirProductAdapter(
    private val onDeleteLaundry: (ProductLaundry) -> Unit,
    private val onDeleteRental: (ProductRental) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    inner class ProductRentalViewHolder(val binding: KurirCardProductRentalBinding) : RecyclerView.ViewHolder(binding.root) {
        val id_nama_rental = binding.productName
        val status = binding.branchProductRental
        val price = binding.priceProductRental
    }

    inner class ProductLaundryViewHolder(val binding: KurirCardProductLaundryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val nama_laundry = binding.namaProductLaundry
        val waktu = binding.waktuProductLaundry
        val harga = binding.hargaProductLaundry
    }

    enum class TYPE_VIEW { LAUNDRY , RENTAL }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  RecyclerView.ViewHolder {
        when(viewType) {
            TYPE_VIEW.LAUNDRY.ordinal -> {
                val binding = KurirCardProductLaundryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return ProductLaundryViewHolder(binding)
            }
            TYPE_VIEW.RENTAL.ordinal -> {
                val binding = KurirCardProductRentalBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return ProductRentalViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductLaundry -> TYPE_VIEW.LAUNDRY.ordinal
            is ProductRental -> TYPE_VIEW.RENTAL.ordinal
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onBindViewHolder(holder:RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ProductLaundryViewHolder -> {
                val laundryItem = item as ProductLaundry
                var harga_laundry = laundryItem.price_laundry_item.toString()
                holder.nama_laundry.text = laundryItem.name_laundry_item.toString()
                holder.harga.text = "Rp $harga_laundry"
                holder.waktu.text = laundryItem.time_laundry_item.toString()

                // Add edit button click listener
                holder.binding.layoutButtonEdit.setOnClickListener {
                    val fragment = CreateItemLaundryFragment().apply {
                        arguments = Bundle().apply {
                            putInt("productLaundryID", laundryItem.id_laundry_item!!)
                        }
                    }

                    val fragmentManager = (holder.itemView.context as androidx.fragment.app.FragmentActivity).supportFragmentManager
                    fragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, fragment)
                        .addToBackStack(null)
                        .commit()
                }

                // Add delete button click listener with ViewModel integration
                holder.binding.layoutButtonDelete.setOnClickListener {
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Konfirmasi Hapus")
                        .setMessage("Apakah Anda yakin ingin menghapus item laundry '${laundryItem.name_laundry_item}'?")
                        .setPositiveButton("Ya") { _, _ ->
                            // Call the delete function passed from Fragment
                            onDeleteLaundry(laundryItem)
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }
            is ProductRentalViewHolder -> {
                val rentalItem = item as ProductRental
                val branchName = branches.find { it.id_branch == rentalItem.id_branch_rental_item }?.name_branch ?: "Unknown"
                holder.id_nama_rental.text = rentalItem.name_rental_item.toString()
                holder.status.text = branchName
                holder.price.text = rentalItem.price_rental_item.toString()

                // Add edit button click listener
                holder.binding.layoutButtonEdit.setOnClickListener {
                    val fragment = CreateItemRentalFragment().apply {
                        arguments = Bundle().apply {
                            putInt("productRentalID", rentalItem.id_rental_item!!)
                        }
                    }

                    val fragmentManager = (holder.itemView.context as androidx.fragment.app.FragmentActivity).supportFragmentManager
                    fragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, fragment)
                        .addToBackStack(null)
                        .commit()
                }

                // Add delete button click listener with ViewModel integration
                holder.binding.layoutButtonDelete.setOnClickListener {
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Konfirmasi Hapus")
                        .setMessage("Apakah Anda yakin ingin menghapus item rental '${rentalItem.name_rental_item}'?")
                        .setPositiveButton("Ya") { _, _ ->
                            // Call the delete function passed from Fragment
                            onDeleteRental(rentalItem)
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }
        }
    }

    private var originalList = listOf<Any>()

    override fun submitList(list: List<Any>?) {
        originalList = list ?: emptyList()
        super.submitList(list)
    }

    fun filter(query: String) {
        if (query.isEmpty()) {
            super.submitList(originalList)
            return
        }

        val filteredList = originalList.filter { item ->
            when (item) {
                is ProductLaundry -> item.name_laundry_item?.contains(query, ignoreCase = true) == true
                is ProductRental -> item.name_rental_item?.contains(query, ignoreCase = true) == true
                else -> false
            }
        }
        super.submitList(filteredList)
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is ProductLaundry -> {
                    if (newItem is ProductLaundry) {
                        (oldItem.id_laundry_item) == (newItem.id_laundry_item)
                    } else {
                        false
                    }
                }
                is ProductRental -> {
                    if (newItem is ProductRental) {
                        (oldItem.id_rental_item) == (newItem.id_rental_item)
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is ProductLaundry -> {
                    if (newItem is ProductLaundry) {
                        (oldItem) == (newItem)
                    } else {
                        false
                    }
                }
                is ProductRental -> {
                    if (newItem is ProductRental) {
                        (oldItem) == (newItem)
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }
}