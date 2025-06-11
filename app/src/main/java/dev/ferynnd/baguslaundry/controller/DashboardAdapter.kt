package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.CardDashboardBinding
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.StatusReportLaundry

class DashboardAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

      private var clients: List<Client> = emptyList()

    fun setClient(client: List<Client>) {
        clients = client
        notifyDataSetChanged()
    }

    inner class ReportLaundryViewHolder(val binding: CardDashboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ReportRentalViewHolder(val binding: CardDashboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { LAUNDRY, RENTAL }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            CardDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ReportLaundryViewHolder -> {
                val data = item as ReportLaundry
                holder.binding.apply {
                    header.text = "LAUNDRY"
                    inputId.text = data.number_transaction_laundry.toString()
                    inputDate.text = data.first_date_transaction_laundry
                    val dataStatus = when (data.status_transaction_laundry) {
                        StatusReportLaundry.paid -> "Sudah Bayar"
                        StatusReportLaundry.unpaid -> "Belum Bayar"
                        StatusReportLaundry.completed -> "Selesai"
                        StatusReportLaundry.cancelled -> "DiBatalkan"
                    }

                    inputStatus.text = dataStatus

                    val context = holder.binding.root.context

                    val dataStatusColor = when (data.status_transaction_laundry) {
                        StatusReportLaundry.paid -> ContextCompat.getColor(
                            context,
                            R.color.blue500
                        )

                        StatusReportLaundry.unpaid -> ContextCompat.getColor(
                            context,
                            R.color.secondary
                        )

                        StatusReportLaundry.completed -> ContextCompat.getColor(
                            context,
                            R.color.baseActive
                        )

                        StatusReportLaundry.cancelled -> ContextCompat.getColor(
                            context,
                            R.color.red
                        )
                    }
                    layoutStatus.setCardBackgroundColor(dataStatusColor)
                }
            }

            is ReportRentalViewHolder -> {
                val data = item as ReportRental
                 val nameClient = clients.find { it.id_client == data.id_client_transaction_rental }?.name_client ?: "Tanpa Nama"
                holder.binding.apply {
                    header.text = "PENYEWAAN"
                    inputId.text = data.number_transaction_rental.toString()
                    inputDate.text = data.time_transaction_rental
                    inputStatus.text = nameClient
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

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return false
        }
    }
}
