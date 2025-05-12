package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.CardHeaderBinding
import dev.ferynnd.baguslaundry.databinding.CardProductRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ConditionRental
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.StatusRental

class RentalProductAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    inner class ProductRentalViewHolder( val binding: CardProductRentalBinding) : RecyclerView.ViewHolder(binding.root)
    inner class HeaderLaundryViewHolder( val binding: CardHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { HEADER , CONTENT }

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
                val binding = CardProductRentalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ProductRentalViewHolder(binding)
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
                val laundryItem = item as ProductRental
                val branchName = branches.find { it.id_branch == laundryItem.id_branch_rental_item }?.name_branch ?: "Unknown"
                holder.binding.apply {
                    inputName.text = "${laundryItem.number_rental_item} - ${laundryItem.name_rental_item}"
                    inputBranch.text = branchName
                    val dataStatus = when(laundryItem.status_rental_item) {
                        StatusRental.available -> "Tersedia"
                        StatusRental.rented -> "Di Pinjam"
                        StatusRental.maintenance -> "Perawatan"
                    }
                    inputStatus.text = dataStatus
                    val dataCondition = when(laundryItem.condition_rental_item) {
                        ConditionRental.clean -> "Bersih"
                        ConditionRental.dirty -> "Kotor"
                        ConditionRental.damaged -> "Rusak"
                    }
                    inputCondition.text = dataCondition
                    inputDescription.text = laundryItem.description_rental_item
                    inputIsActive.text = laundryItem.is_active_rental_item.toString()
                }
            }
            is HeaderLaundryViewHolder -> {
                val header = item as Branch
                holder.binding.inputNameBranch.text = header.name_branch
                val context = holder.binding.root.context
                val color = ContextCompat.getColor(context, R.color.blueGray) // pastikan 'orange' benar ada di colors.xml
                holder.binding.root.setCardBackgroundColor(color)
            }

        }

    }

      class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when ( oldItem) {
                is ProductRental -> {
                    if (newItem is ProductRental) {
                        (oldItem.id_rental_item) == (newItem.id_rental_item)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ProductRental) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when ( oldItem) {
                is ProductRental -> {
                    if (newItem is ProductRental) {
                        (oldItem) == (newItem)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ProductRental) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }
    }
}

