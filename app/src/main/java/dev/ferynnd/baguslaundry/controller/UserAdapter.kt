package dev.ferynnd.baguslaundry.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.admbaguslaundry.databinding.CardUserBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.IsActiveUser
import dev.ferynnd.admbaguslaundry.model.User
import dev.ferynnd.admbaguslaundry.model.UserGender

class UserAdapter : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    private val TAG = "USER_ADAPTER"

    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        Log.d(TAG, "Branch size: ${branches.size}")
        notifyDataSetChanged()
    }

    inner class UserViewHolder(val binding: CardUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = CardUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)

        Log.d(TAG, "Bind user[$position]: $user")

        val branchName = branches.find {
            it.id_branch == user.id_branch_user
        }?.name_branch ?: "Cabang tidak ditemukan"

        holder.binding.apply {
            inputName.text = user.fullname_user ?: "-"
            inputUsername.text = user.username ?: "-"
            inputRole.text = user.role_user?.toString() ?: "-"
            val statusUser = when (user.is_active_user) {
                IsActiveUser.active -> "Aktif"
                IsActiveUser.inactive -> "Nonaktif"
                else -> "-"
            }

            inputStatus.text = statusUser
            inputBranch.text = branchName
            inputContact.text = user.phone_user ?: "-"
            inputTextAddress.text = user.address_user ?: "-"

            val dataUser = when (user.gender_user) {
                UserGender.male -> "Laki-laki"
                UserGender.female -> "Perempuan"
                else -> "-"
            }

            inputGender.text = dataUser
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id_user == newItem.id_user
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}