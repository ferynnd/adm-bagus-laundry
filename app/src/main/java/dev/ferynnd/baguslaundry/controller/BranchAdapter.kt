package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardBranchBinding
import dev.ferynnd.baguslaundry.model.Branch

class BranchAdapter  : ListAdapter<Branch, BranchAdapter.BranchViewHolder>(BranchDiffCallback()) {

    inner class BranchViewHolder(val binding: CardBranchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchViewHolder {
        val binding = CardBranchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BranchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BranchViewHolder, position: Int) {
        val branch = getItem(position)

        holder.binding.apply {
            inputName.text = branch.name_branch
            inputCity.text = branch.city_branch
            inputStatus.text = branch.is_active_branch.toString()
            inputTextAddress.text = branch.full_address_branch

        }

    }

    class BranchDiffCallback : DiffUtil.ItemCallback<Branch>() {
        override fun areItemsTheSame(oldItem: Branch, newItem: Branch): Boolean {
            return oldItem.id_branch == newItem.id_branch // Assuming username is unique
        }

        override fun areContentsTheSame(oldItem: Branch, newItem: Branch): Boolean {
            return oldItem == newItem
        }
    }
}