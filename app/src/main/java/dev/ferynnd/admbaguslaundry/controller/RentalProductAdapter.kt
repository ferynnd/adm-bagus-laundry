package dev.ferynnd.admbaguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.admbaguslaundry.databinding.CardHeaderBinding
import dev.ferynnd.admbaguslaundry.databinding.CardProductRentalBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.ProductRental
import java.text.NumberFormat
import java.util.Locale

class RentalProductAdapter(
    private val onEditClick: (ProductRental) -> Unit,
    private val onDeleteClick: (ProductRental) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    inner class ProductRentalViewHolder(val binding: CardProductRentalBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderLaundryViewHolder(val binding: CardHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { HEADER, CONTENT }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_VIEW.HEADER.ordinal -> {
                val binding = CardHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderLaundryViewHolder(binding)
            }
            TYPE_VIEW.CONTENT.ordinal -> {
                val binding = CardProductRentalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ProductRentalViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductRental -> TYPE_VIEW.CONTENT.ordinal
            is Branch -> TYPE_VIEW.HEADER.ordinal
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ProductRentalViewHolder -> {
                val rentalItem = item as ProductRental
                val branchName = branches.find {
                    it.id_branch == rentalItem.id_branch_rental_item
                }?.name_branch ?: "Unknown"

                holder.binding.apply {
                    inputName.text = rentalItem.name_rental_item
                    inputBranch.text = branchName

                    val localeID = Locale("in", "ID")
                    val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
                    val harga = rentalItem.price_rental_item
                    inputPrice.text = formatRupiah.format(harga)

                    // Handle Edit Click
                    btnEdit.setOnClickListener {
                        onEditClick(rentalItem)
                    }

                    // Handle Delete Click
                    btnDelete.setOnClickListener {
                        onDeleteClick(rentalItem)
                    }
                }
            }
            is HeaderLaundryViewHolder -> {
                val header = item as Branch
                holder.binding.inputNameBranch.text = header.name_branch
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is ProductRental -> {
                    if (newItem is ProductRental) {
                        oldItem.id_rental_item == newItem.id_rental_item
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ProductRental) {
                        false
                    } else {
                        oldItem == newItem
                    }
                }
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is ProductRental -> {
                    if (newItem is ProductRental) {
                        oldItem == newItem
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ProductRental) {
                        false
                    } else {
                        oldItem == newItem
                    }
                }
            }
        }
    }
}