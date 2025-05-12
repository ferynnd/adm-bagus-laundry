package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardFilterBranchBinding
import dev.ferynnd.baguslaundry.model.Branch

class FilterBranchAdapter ( private val onClick : (Branch) -> Unit ) : ListAdapter<Branch, FilterBranchAdapter.FilterBranchViewHolder>(DiffCallback()) {

    inner class FilterBranchViewHolder( val binding: CardFilterBranchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterBranchViewHolder {
        val binding = CardFilterBranchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilterBranchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterBranchViewHolder, position: Int) {
        val branch = getItem(position)

        holder.binding.inputName.text = branch.name_branch
        holder.itemView.setOnClickListener {
            onClick(branch)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Branch>() {
        override fun areItemsTheSame(oldItem: Branch, newItem: Branch): Boolean {
            return oldItem.id_branch ==newItem.id_branch
        }

        override fun areContentsTheSame(oldItem: Branch, newItem: Branch): Boolean {
            return oldItem == newItem
        }
    }

}