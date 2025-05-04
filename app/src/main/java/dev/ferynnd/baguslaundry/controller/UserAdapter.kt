package dev.ferynnd.baguslaundry.controller


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardUserBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.User
import dev.ferynnd.baguslaundry.model.UserGender

class UserAdapter : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    inner class UserViewHolder(val binding: CardUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = CardUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)

         val branchName = branches.find { it.id_branch == user.id_branch_user }?.name_branch ?: "Unknown"

        holder.binding.apply {
            inputName.text = user.fullname_user
            inputUsername.text = user.username
            inputRole.text = user.role_user.toString()
            inputStatus.text = user.is_active_user.toString()
            inputBranch.text = branchName // seharusnya mengambil data nama barnch
            inputContact.text = user.phone_user
            inputTextAddress.text = user.address_user

            val dataUser = when(user.gender_user) {
                UserGender.male -> "Laki-laki"
                UserGender.female -> "Perempuan"
            }

            inputGender.text = dataUser


        }

    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id_user == newItem.id_user // Assuming username is unique
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}