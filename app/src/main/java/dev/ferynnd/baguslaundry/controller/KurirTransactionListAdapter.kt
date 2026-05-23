package dev.ferynnd.baguslaundry.controller

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.KurirCardTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.databinding.KurirCardTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.User
import dev.ferynnd.baguslaundry.ui.user.PrintPreviewFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.PrintPreviewRentalFragment
import java.text.NumberFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class KurirTransactionListAdapter(
    private val fragmentManager: FragmentManager
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var clients: List<Client> = emptyList()
    private var users: List<User> = emptyList()
    private var originalList: List<Any> = emptyList()

    fun setClients(clientList: List<Client>) {
        clients = clientList
        notifyDataSetChanged()
    }

    fun setUsers(userList: List<User>) {
        users = userList
        notifyDataSetChanged()
    }

    private val numberFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

    inner class TransactionRentalViewHolder(val binding: KurirCardTransaksiRentalBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class TransactionLaundryViewHolder(val binding: KurirCardTransaksiLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { LAUNDRY, RENTAL }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_VIEW.LAUNDRY.ordinal -> {
                val binding = KurirCardTransaksiLaundryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                TransactionLaundryViewHolder(binding)
            }
            TYPE_VIEW.RENTAL.ordinal -> {
                val binding = KurirCardTransaksiRentalBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                TransactionRentalViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReportLaundry -> TYPE_VIEW.LAUNDRY.ordinal
            is ReportRental -> TYPE_VIEW.RENTAL.ordinal
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TransactionLaundryViewHolder -> {
                bindLaundryTransaction(holder, item as ReportLaundry)
            }
            is TransactionRentalViewHolder -> {
                bindRentalTransaction(holder, item as ReportRental)
            }
        }
    }

    private fun bindLaundryTransaction(
        holder: TransactionLaundryViewHolder,
        laundryReport: ReportLaundry
    ) {
        val context = holder.binding.root.context

        holder.binding.apply {
            numberTransaction.text = laundryReport.number_transaction_laundry.toString()

            if (laundryReport.status_transaction_laundry == StatusReportLaundry.completed) {
                wadahStatus.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.greenBlueLight)
                )
            }

            namaPelangganTransaksiLaundry.text = laundryReport.name_client_transaction_laundry ?: "-"

            tanggalMasukTransaksiLaundry.text = laundryReport.formatted_first_date ?: "-"
            tanggalKeluarTransaksiLaundry.text = laundryReport.formatted_last_date ?: "Tidak ada tanggal keluar"

            pcsTransaksiLaundry.text = laundryReport.count_item_transaction_laundry?.toString() ?: "0"
            statusTransaksiLaundry.text = laundryReport.status_transaction_laundry.toString()
            beratTransaksiLaundry.text = "${laundryReport.total_weight_transaction_laundry} Kg"
            totalHargaTransaksiLaundry.text = numberFormatter.format(laundryReport.total_price_transaction_laundry?.toDouble() ?: 0.0)
            tunaiTransaksiLaundry.text = numberFormatter.format(laundryReport.cash_transaction_laundry?.toDouble() ?: 0.0)
            kembalianTransaksiLaundry.text = numberFormatter.format(laundryReport.change_money_transaction_laundry?.toDouble() ?: 0.0)
            noteTransaksiLaundry.text = laundryReport.notes_transaction_laundry ?: "Tidak ada catatan"

            wadahButtonCetak.setOnClickListener {
                val bundle = Bundle().apply {
                    putInt("transactionId", laundryReport.id_transaction_laundry ?: 0)
                }
                Log.d("CreateTransaction", "Transaction ID: ${laundryReport.id_transaction_laundry}")

                val fragment = PrintPreviewFragment().apply {
                    arguments = bundle
                }

                fragmentManager.beginTransaction()
                    .replace(R.id.host_fragment_user, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun bindRentalTransaction(
        holder: TransactionRentalViewHolder,
        rentalReport: ReportRental
    ) {
        val clientName = clients.find {
            it.id_client == rentalReport.id_client_transaction_rental
        }?.name_client ?: "Unknown"

        val userName = users.find {
            it.id_user == rentalReport.id_kurir_transaction_rental
        }?.fullname_user ?: "Unknown"

        holder.binding.apply {
            noResiTransaksiRental.text = rentalReport.number_transaction_rental.toString()

            // ===== LANGSUNG PAKAI DATA YANG SUDAH DIFORMAT DARI VIEWMODEL =====
            tanggalTransaksiRental.text = rentalReport.formatted_time_transaction_rental ?: "-"

            namaClientTransaksiRental.text = clientName
            namaPenerimaTransaksiRental.text = rentalReport.recipient_name_transaction_rental ?: "-"
            namaKurirTransaksiRental.text = userName
            jumlahItemTransaksiRental.text = rentalReport.total_pcs_transaction_rental.toString()
            totalBeratTransaksiRental.text = "${rentalReport.total_weight_transaction_rental} Kg"
            noteTransaksiRental.text = rentalReport.notes_transaction_rental ?: "Tidak ada catatan"

            wadahCetak.setOnClickListener {
                val bundle = Bundle().apply {
                    putInt("transactionId", rentalReport.id_transaction_rental ?: 0)
                }
                Log.d("CreateTransaction", "Transaction ID: ${rentalReport.id_transaction_rental}")

                val fragment = PrintPreviewRentalFragment().apply {
                    arguments = bundle
                }

                fragmentManager.beginTransaction()
                    .replace(R.id.host_fragment_user, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun submitList(list: List<Any>?) {
        originalList = list ?: emptyList()
        super.submitList(list)
    }

    fun filter(query: String) {
        if (query.isEmpty()) {
            super.submitList(originalList)
            return
        }
        val filteredList = originalList.filter { item ->
            when (item) {
                is ReportLaundry -> {
                    item.name_client_transaction_laundry?.contains(query, ignoreCase = true) == true
                }
                is ReportRental -> {
                    val clientName = clients.find {
                        it.id_client == item.id_client_transaction_rental
                    }?.name_client ?: ""
                    (item.recipient_name_transaction_rental?.contains(query, ignoreCase = true) == true) ||
                    (clientName.contains(query, ignoreCase = true))
                }
                else -> false
            }
        }
        super.submitList(filteredList)
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is ReportLaundry -> {
                    if (newItem is ReportLaundry) {
                        oldItem.id_transaction_laundry == newItem.id_transaction_laundry
                    } else false
                }
                is ReportRental -> {
                    if (newItem is ReportRental) {
                        oldItem.id_transaction_rental == newItem.id_transaction_rental
                    } else false
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is ReportLaundry -> {
                    if (newItem is ReportLaundry) {
                        oldItem == newItem
                    } else false
                }
                is ReportRental -> {
                    if (newItem is ReportRental) {
                        oldItem == newItem
                    } else false
                }
                else -> false
            }
        }
    }
}