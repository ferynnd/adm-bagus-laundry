package dev.ferynnd.baguslaundry.controller.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
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
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.User
import dev.ferynnd.baguslaundry.ui.user.PrintPreviewFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.PrintPreviewRentalFragment
import java.text.NumberFormat
import java.util.Locale


class KurirTransactionListAdapter(
    private val fragmentManager: FragmentManager
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()){

    private var clients : List<Client> = emptyList()
    private var users : List<User> = emptyList()

    private var originalList: List<Any> = emptyList()

    fun setClients(clientList : List<Client>) {
        clients = clientList
        notifyDataSetChanged()
    }

    fun setUsers(userList : List<User>) {
        users = userList
        notifyDataSetChanged()
    }

    // Formatter untuk mata uang (sama seperti di Fragment)
    private val numberFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true // Untuk pemisah ribuan (titik)
            maximumFractionDigits = 0 // Ini yang menghilangkan ",00"
            minimumFractionDigits = 0 // Pastikan tidak ada desimal minimal
        }


    inner class TransactionRentalViewHolder(val binding: KurirCardTransaksiRentalBinding) :
        RecyclerView.ViewHolder(binding.root)
    inner class TransactionLaundryViewHolder(val binding: KurirCardTransaksiLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)
    enum class TYPE_VIEW { LAUNDRY , RENTAL }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  RecyclerView.ViewHolder {
        when(viewType) {
            TYPE_VIEW.LAUNDRY.ordinal -> {
                val binding = KurirCardTransaksiLaundryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return TransactionLaundryViewHolder(binding)
            }
            TYPE_VIEW.RENTAL.ordinal -> {
                val binding = KurirCardTransaksiRentalBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return TransactionRentalViewHolder(binding)
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

    override fun onBindViewHolder(holder:RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TransactionLaundryViewHolder -> {
                val laundryReport = item as ReportLaundry
                val context = holder.binding.root.context
                holder.binding.numberTransaction.text = laundryReport.number_transaction_laundry.toString()
                if (laundryReport.status_transaction_laundry == StatusReportLaundry.completed) {
                    holder.binding.wadahStatus.setCardBackgroundColor(ContextCompat.getColor(
                        context,
                        R.color.greenBlueLight
                    ))
                }
                holder.binding.namaPelangganTransaksiLaundry.text = laundryReport.name_client_transaction_laundry.toString()
                holder.binding.tanggalMasukTransaksiLaundry.text = laundryReport.first_date_transaction_laundry.toString()
                holder.binding.tanggalKeluarTransaksiLaundry.text = if( laundryReport.last_date_transaction_laundry == null ) "Tidak ada tanggal keluar" else laundryReport.last_date_transaction_laundry.toString()
                holder.binding.pcsTransaksiLaundry.text =  if ( laundryReport.count_item_transaction_laundry == null ) "0" else laundryReport.count_item_transaction_laundry.toString()
                holder.binding.statusTransaksiLaundry.text = laundryReport.status_transaction_laundry.toString()
                holder.binding.beratTransaksiLaundry.text = "${laundryReport.total_weight_transaction_laundry.toString()} + Kg"
                holder.binding.totalHargaTransaksiLaundry.text =  numberFormatter.format(laundryReport.total_price_transaction_laundry?.toDouble() ?: 0.0)
                holder.binding.tunaiTransaksiLaundry.text =  numberFormatter.format(laundryReport.cash_transaction_laundry?.toDouble() ?: 0.0)
                holder.binding.kembalianTransaksiLaundry.text =  numberFormatter.format(laundryReport.change_money_transaction_laundry?.toDouble() ?: 0.0)
                holder.binding.noteTransaksiLaundry.text = if ( laundryReport.notes_transaction_laundry == null ) "Tidak ada catatan" else laundryReport.notes_transaction_laundry.toString()

                holder.binding.wadahButtonCetak.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putInt("transactionId", laundryReport.id_transaction_laundry?.toInt() ?: 0)
                    Log.d("CreateTransaction", "Transaction ID: ${laundryReport.id_transaction_laundry}")

                    val fragment = PrintPreviewFragment()
                    fragment.arguments = bundle

                    fragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, fragment)
                        .addToBackStack(null) // opsional, jika ingin bisa kembali
                        .commit()
                }
            }
            is TransactionRentalViewHolder -> {
                val rentalReport = item as ReportRental
                val clientName = clients.find { it.id_client == rentalReport.id_client_transaction_rental }?.name_client ?: "Unknown"
                val userName = users.find { it.id_user == rentalReport.id_kurir_transaction_rental }?.fullname_user ?: "Unknown"
                holder.binding.noResiTransaksiRental.text = rentalReport.number_transaction_rental.toString()
                holder.binding.tanggalTransaksiRental.text = rentalReport.time_transaction_rental.toString()
                holder.binding.namaClientTransaksiRental.text = clientName
                holder.binding.namaPenerimaTransaksiRental.text = rentalReport.recipient_name_transaction_rental.toString()
                holder.binding.namaKurirTransaksiRental.text = userName
                holder.binding.jumlahItemTransaksiRental.text = rentalReport.total_pcs_transaction_rental.toString()
                holder.binding.totalBeratTransaksiRental.text = rentalReport.total_weight_transaction_rental.toString()
                holder.binding.noteTransaksiRental.text = if( rentalReport.notes_transaction_rental == null ) "Tidak ada catatan" else rentalReport.notes_transaction_rental.toString()

                holder.binding.wadahCetak.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putInt("transactionId", rentalReport.id_transaction_rental ?: 0)
                    Log.d("CreateTransaction", "Transaction ID: ${rentalReport.id_transaction_rental}")

                    val fragment = PrintPreviewRentalFragment()
                    fragment.arguments = bundle

                    fragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, fragment)
                        .addToBackStack(null) // opsional, jika ingin bisa kembali
                        .commit()
                }
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
                    val clientName = clients.find { it.id_client == item.id_client_transaction_rental }?.name_client ?: ""
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
            return when ( oldItem) {
                is ReportLaundry -> {
                    if (newItem is ReportLaundry) {
                        (oldItem.id_transaction_laundry) == (newItem.id_transaction_laundry)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ReportLaundry) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when ( oldItem) {
                is ReportLaundry -> {
                    if (newItem is ReportLaundry) {
                        (oldItem) == (newItem)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ReportLaundry) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }
    }
}