package dev.ferynnd.admbaguslaundry.controller

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.databinding.CardDashboardBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.Client
import dev.ferynnd.admbaguslaundry.model.ReportLaundry
import dev.ferynnd.admbaguslaundry.model.ReportRental
import dev.ferynnd.admbaguslaundry.model.StatusReportLaundry

class DashboardAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var clients: List<Client> = emptyList()
    private var branchList: List<Branch> = emptyList()

    fun setClient(client: List<Client>) {
        clients = client
        notifyDataSetChanged()
    }

    fun setBranch(branch: List<Branch>) {
        branchList = branch
        notifyDataSetChanged()
    }

    inner class ReportLaundryViewHolder(val binding: CardDashboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ReportRentalViewHolder(val binding: CardDashboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { LAUNDRY, RENTAL }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = CardDashboardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return when (viewType) {
            TYPE_VIEW.LAUNDRY.ordinal -> ReportLaundryViewHolder(binding)
            TYPE_VIEW.RENTAL.ordinal -> ReportRentalViewHolder(binding)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReportLaundry -> TYPE_VIEW.LAUNDRY.ordinal
            is ReportRental -> TYPE_VIEW.RENTAL.ordinal
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        when (holder) {
            is ReportLaundryViewHolder -> {
                val data = item as ReportLaundry
                holder.binding.apply {
                    val branch = branchList.find {
                        it.id_branch == data.id_branch_transaction_laundry
                    }
                    val timezoneLabel = getTimezoneLabel(branch?.timezone_branch)

                    header.text = "LAUNDRY"
                    inputId.text = data.number_transaction_laundry.toString()
                    inputDate.text =  "${data.first_date_transaction_laundry ?: "-"} $timezoneLabel"
                }
            }

            is ReportRentalViewHolder -> {
                val data = item as ReportRental
                holder.binding.apply {
                    val branch = branchList.find {
                        it.id_branch == data.id_branch_transaction_rental
                    }
                    val timezoneLabel = getTimezoneLabel(branch?.timezone_branch)
                    header.text = "PENYEWAAN"
                    inputId.text = data.number_transaction_rental.toString()
                    inputDate.text =  "${data.time_transaction_rental ?: "-"} $timezoneLabel"
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ReportLaundry && newItem is ReportLaundry ->
                    oldItem.id_transaction_laundry == newItem.id_transaction_laundry

                oldItem is ReportRental && newItem is ReportRental ->
                    oldItem.id_transaction_rental == newItem.id_transaction_rental

                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }
    }

    private fun getTimezoneLabel(timezone: String?): String {
        return when (timezone) {
            "Asia/Jakarta" -> "WIB"
            "Asia/Makassar" -> "WITA"
            "Asia/Jayapura" -> "WIT"
            else -> "WIB"
        }
    }
}