package dev.ferynnd.baguslaundry.controller.user

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.KurirCardListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.databinding.KurirCardListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.databinding.KurirCardTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.databinding.KurirCardTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.User


class KurirTransactionListAdapter() : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()){

    private var clients : List<Client> = emptyList()
    private var users : List<User> = emptyList()

    fun setClients(clientList : List<Client>) {
        clients = clientList
        notifyDataSetChanged()
    }

    fun setUsers(userList : List<User>) {
        users = userList
        notifyDataSetChanged()
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
                holder.binding.numberTransaction.text = laundryReport.number_transaction_laundry.toString()
                holder.binding.namaPelangganTransaksiLaundry.text = laundryReport.name_client_transaction_laundry.toString()
                holder.binding.tanggalMasukTransaksiLaundry.text = laundryReport.first_date_transaction_laundry.toString()
                holder.binding.tanggalKeluarTransaksiLaundry.text = laundryReport.last_date_transaction_laundry.toString()
                holder.binding.pcsTransaksiLaundry.text =  if ( laundryReport.count_item_laundry_transaction_laundry == null ) "0" else laundryReport.count_item_laundry_transaction_laundry.toString()
                holder.binding.statusTransaksiLaundry.text = laundryReport.status_transaction_laundry.toString()
                holder.binding.hargaTransaksiLaundry.text = laundryReport.total_transaction_laundry.toString()
                holder.binding.beratTransaksiLaundry.text = laundryReport.total_weight_transaction_laundry.toString()
                holder.binding.totalHargaTransaksiLaundry.text = laundryReport.total_price_transaction_laundry.toString()
                holder.binding.tunaiTransaksiLaundry.text = laundryReport.cash_transaction_laundry.toString()
                holder.binding.kembalianTransaksiLaundry.text = laundryReport.change_money_transaction_laundry.toString()
                holder.binding.noteTransaksiLaundry.text = laundryReport.notes_transaction_laundry.toString()
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
                holder.binding.noteTransaksiRental.text = rentalReport.notes_transaction_rental.toString()
            }
        }
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