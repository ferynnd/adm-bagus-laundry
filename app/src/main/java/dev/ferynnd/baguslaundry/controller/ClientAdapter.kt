package dev.ferynnd.baguslaundry.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardClientBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client

class ClientAdapter : ListAdapter<Client, ClientAdapter.ClientViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    inner class ClientViewHolder( val binding: CardClientBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = CardClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        val client = getItem(position)


        holder.binding.apply {
            inputName.text = client.name_client
            inputStatus.text = client.is_active_client.toString()
            inputTelephon.text = client.phone_client
            inputAddress.text = client.address_client

            val branchName = branches.find { it.id_branch == client.id_branch_client}?.name_branch ?: "Unknown"
            inputBranch.text = branchName

        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Client>() {
        override fun areItemsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem.id_client ==newItem.id_client
        }

        override fun areContentsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem == newItem
        }
    }

}