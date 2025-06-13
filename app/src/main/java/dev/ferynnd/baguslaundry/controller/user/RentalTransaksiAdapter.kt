package dev.ferynnd.baguslaundry.controller.user

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.KurirCardTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.User
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class RentalTransaksiAdapter(
    private val onItemClick: (ReportRental) -> Unit
) : ListAdapter<ReportRental, RentalTransaksiAdapter.ReportRentalViewHolder>(DiffCallback()) {

    private var client: List<Client> = emptyList()
    private var kurir: List<User> = emptyList()

    fun setClient(clientList: List<Client>) {
        client = clientList
        notifyDataSetChanged()
    }

    fun setKurir(kurirList: List<User>) {
        kurir = kurirList
        notifyDataSetChanged()
    }

    inner class ReportRentalViewHolder(val binding: KurirCardTransaksiRentalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportRentalViewHolder {
        val binding = KurirCardTransaksiRentalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportRentalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportRentalViewHolder, position: Int) {
        val report = getItem(position)
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        val decimalFormat = DecimalFormat("#,##0.##")
        val context = holder.itemView.context

        holder.itemView.setOnClickListener {
            onItemClick(report)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReportRental>() {
        override fun areItemsTheSame(oldItem: ReportRental, newItem: ReportRental): Boolean {
            return oldItem.id_transaction_rental == newItem.id_transaction_rental
        }

        override fun areContentsTheSame(oldItem: ReportRental, newItem: ReportRental): Boolean {
            return oldItem == newItem
        }
    }
}
