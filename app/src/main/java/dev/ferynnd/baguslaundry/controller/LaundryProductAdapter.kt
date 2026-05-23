package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.admbaguslaundry.databinding.CardHeaderBinding
import dev.ferynnd.admbaguslaundry.databinding.CardProductLaundryBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.ProductLaundry
import java.text.NumberFormat
import java.util.Locale

class LaundryProductAdapter: ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()
    private var onEditClickListener: ((ProductLaundry) -> Unit)? = null
    private var onDeleteClickListener: ((ProductLaundry) -> Unit)? = null

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    fun setOnEditClickListener(listener: (ProductLaundry) -> Unit) {
        onEditClickListener = listener
    }

    fun setOnDeleteClickListener(listener: (ProductLaundry) -> Unit) {
        onDeleteClickListener = listener
    }

    inner class ProductLaundryViewHolder(val binding: CardProductLaundryBinding) : RecyclerView.ViewHolder(binding.root)
    inner class HeaderLaundryViewHolder(val binding: CardHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { HEADER, CONTENT }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType) {
            TYPE_VIEW.HEADER.ordinal -> {
                val binding = CardHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return HeaderLaundryViewHolder(binding)
            }

            TYPE_VIEW.CONTENT.ordinal -> {
                val binding = CardProductLaundryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ProductLaundryViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductLaundry -> TYPE_VIEW.CONTENT.ordinal
            is Branch -> TYPE_VIEW.HEADER.ordinal
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ProductLaundryViewHolder -> {
                val laundryItem = item as ProductLaundry
                val branchName = branches.find { it.id_branch == laundryItem.id_branch_laundry_item }?.name_branch ?: "Unknown"

                holder.binding.apply {
                    inputName.text = laundryItem.name_laundry_item
                    inputBranch.text = branchName
                    inputTime.text = laundryItem.time_laundry_item ?: "-"

                    val localeID = Locale("in", "ID")
                    val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
                    val harga = laundryItem.price_laundry_item
                    inputPrice.text = formatRupiah.format(harga)

                    // Set listener untuk tombol Edit
                    btnEdit.setOnClickListener {
                        onEditClickListener?.invoke(laundryItem)
                    }

                    // Set listener untuk tombol Hapus
                    btnDelete.setOnClickListener {
                        onDeleteClickListener?.invoke(laundryItem)
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
                is ProductLaundry -> {
                    if (newItem is ProductLaundry) {
                        (oldItem.id_laundry_item) == (newItem.id_laundry_item)
                    } else {
                        false
                    }
                }
                is Branch -> {
                    if (newItem is Branch) {
                        (oldItem.id_branch) == (newItem.id_branch)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ProductLaundry) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
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
                is Branch -> {
                    if (newItem is Branch) {
                        (oldItem) == (newItem)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ProductLaundry) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }
    }
}