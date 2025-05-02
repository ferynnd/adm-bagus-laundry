package dev.ferynnd.baguslaundry.controller

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardHeaderBinding
import dev.ferynnd.baguslaundry.databinding.CardReportRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ReportRental
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class RentalReportAdapter ( private val onDetail : (ReportRental) -> Unit) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {
//class RentalReportAdapter (private val onEdit : (ReportRental) -> Unit, private val onDelete : (ReportRental) -> Unit ) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {


    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    inner class ReportRentalViewHolder( val binding: CardReportRentalBinding) : RecyclerView.ViewHolder(binding.root)
    inner class HeaderViewHolder( val binding: CardHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { HEADER , CONTENT }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType) {
            TYPE_VIEW.HEADER.ordinal -> {
                val binding = CardHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return HeaderViewHolder(binding)
            }

            TYPE_VIEW.CONTENT.ordinal -> {
                val binding = CardReportRentalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ReportRentalViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")

        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReportRental -> TYPE_VIEW.CONTENT.ordinal
              is String -> TYPE_VIEW.HEADER.ordinal
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            when (holder) {
                is ReportRentalViewHolder -> {
                    val transactionReportRental = item as ReportRental
                     val branchName = branches.find { it.id_branch == transactionReportRental.id_branch_transaction_rental }?.name_branch ?: "Unknown"
                    holder.binding.apply {
                        inputBranch.text = branchName
                        inputCLient.text = transactionReportRental.id_client_transaction_rental.toString()
                        inputRecipient.text = transactionReportRental.recipient_name_transaction_rental
                        inputSender.text = transactionReportRental.id_kurir_transaction_rental.toString()
                        inputStatus.text = transactionReportRental.status_transaction_rental.toString()
                        inputWeight.text = transactionReportRental.total_weight_transaction_rental.toString()
                        inputPcs.text = transactionReportRental.total_pcs_transaction_rental.toString()
                        inputAditionalCost.text = transactionReportRental.additional_cost_transaction_rental.toString()
                        inputTotalPrice.text = transactionReportRental.total_price_transaction_rental.toString()
                        inputNotes.text = transactionReportRental.notes_transaction_rental


                        val waktu = transactionReportRental.created_at


                        // Format output yang diinginkan
                        val formatterOutput = DateTimeFormatter.ofPattern("HH:mm:ss - EEEE, dd MMMM yyyy", Locale("id", "ID"))

                        // Parse string waktu menjadi Instant
                        val instant = if (!waktu.isNullOrEmpty()) {
                            try {
                                Instant.parse(waktu)  // Parsing string ke Instant (tanggal dengan zona waktu)
                            } catch (e: Exception) {
                                null // Menangani kesalahan jika parsing gagal
                            }
                        } else {
                            null
                        }

                        // Format tanggal jika berhasil diparse
                        val formattedDate = if (instant != null) {
                            formatterOutput.format(instant.atZone(ZoneId.systemDefault()))  // Konversi Instant ke zona waktu lokal
                        } else {
                            "Tanggal tidak valid"  // Tampilkan pesan error atau nilai default
                        }

                        inputTime.text = formattedDate

                        buttonDetail.setOnClickListener {
                            onDetail(transactionReportRental)
                        }
                    }
                }
                is HeaderViewHolder -> {
                    val header = item as String
                    holder.binding.inputNameBranch.text = header
                }
            }
        }


      class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when ( oldItem) {
                is ReportRental -> {
                    if (newItem is ReportRental) {
                        (oldItem.id_transaction_rental) == (newItem.id_transaction_rental)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ReportRental) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when ( oldItem) {
                is ReportRental -> {
                    if (newItem is ReportRental) {
                        (oldItem) == (newItem)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ReportRental) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }
    }
}