package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardListRentalInvoiceBinding
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.StatusInvoiceRental
import dev.ferynnd.baguslaundry.model.StatusTransactionRental

class RentalInvoiceAdapter :
    ListAdapter<ReportRental, RentalInvoiceAdapter.RentalInvoiceViewHolder>(DiffCallback()) {

    private val inputNotes = mutableMapOf<Int, String>()
    private val selectedStatuses = mutableMapOf<Int, StatusInvoiceRental>()
    val selectedItems = mutableSetOf<ReportRental>()

    fun getSelectedItems(): List<ReportRental> = selectedItems.toList()


    inner class RentalInvoiceViewHolder(val binding: CardListRentalInvoiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RentalInvoiceViewHolder {
        val binding = CardListRentalInvoiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RentalInvoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RentalInvoiceViewHolder, position: Int) {
        val rentalReport = getItem(position)
        val statusEnumValues = listOf(
            StatusInvoiceRental.UNPAID,
            StatusInvoiceRental.PAID,
            StatusInvoiceRental.CANCELLED
        )

        holder.binding.apply {
            inputIdTransaction.text = rentalReport.id_transaction_rental.toString()
            val dataStatus = when (rentalReport.status_transaction_rental) {
                StatusTransactionRental.WAITING_FOR_APPROVAL -> "Menunggu Persetujuan"
                StatusTransactionRental.APPROVED -> "Disetujui"
                StatusTransactionRental.OUT -> "Keluar"
                StatusTransactionRental.IN -> "Masuk"
                StatusTransactionRental.CANCELLED -> "Dibatalkan"
            }
            inputStatus.text = dataStatus
            inputDate.text = rentalReport.time_transaction_rental.toString()

            // Spinner adapter dengan label Indonesia
            val adapterSpinner = ArrayAdapter(
                holder.itemView.context,
                android.R.layout.simple_spinner_item,
                statusEnumValues.map { it.displayName() }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            selectStatus.adapter = adapterSpinner

            // Ambil status yang sedang dipilih, fallback ke default dari data
            val selected = selectedStatuses[position]
                ?: StatusInvoiceRental.fromValue(rentalReport.status_transaction_rental.toString()) // gunakan field yang sesuai

            selected?.let {
                selectStatus.setSelection(statusEnumValues.indexOf(it))
            }

            // Spinner listener
            selectStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    val currentPos = holder.adapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        selectedStatuses[currentPos] = statusEnumValues[pos]
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            // Notes handling
            inputTextNotes.setText(inputNotes[position] ?: "")
            inputTextNotes.addTextChangedListener {
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    inputNotes[currentPos] = it.toString()
                }
            }

            iconRadio.isChecked = selectedItems.contains(rentalReport)

            iconRadio.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(rentalReport)
                } else {
                    selectedItems.remove(rentalReport)
                }
            }
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
