package dev.ferynnd.baguslaundry.controller

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardInvoiceRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.InvoiceRentalResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class InvoiceAdapter(private val onPrint: (InvoiceRentalResponse) -> Unit) :
    ListAdapter<InvoiceRentalResponse, InvoiceAdapter.InvoiceRentalResponseViewHolder>(
        InvoiceRentalResponseDiffCallback()
    ) {

    private var branches: List<Branch> = emptyList()
    private var clients: List<Client> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    fun setClients(clientList: List<Client>) {
        clients = clientList
        notifyDataSetChanged()
    }

    inner class InvoiceRentalResponseViewHolder(val binding: CardInvoiceRentalBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InvoiceRentalResponseViewHolder {
        val binding =
            CardInvoiceRentalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvoiceRentalResponseViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: InvoiceRentalResponseViewHolder, position: Int) {
        val invoice = getItem(position)

        holder.binding.apply {
            numberInvoice.text = invoice.number_invoice_rental
            inputDate.text = formatMonthYear(invoice.time_invoice_rental)


            val branchName =
                branches.find { it.id_branch == invoice.id_branch_invoice }?.name_branch
                    ?: "Unknown"
            val clientName =
                clients.find { it.id_client == invoice.id_client_invoice }?.name_client ?: "Unknown"
            InputClient.text = clientName
            InputBranch.text = branchName

            btnDownload.setOnClickListener {
                onPrint(invoice)
            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatMonthYear(dateString: String?): String {
        return try {
            if (dateString == null) return "-"
            val formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val formatterOutput = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID")) // contoh: Mei 2025
            val date = LocalDate.parse(
                dateString.substring(0, 10),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
            )
            date.format(formatterOutput)
        } catch (e: Exception) {
            Log.e("InvoiceAdapter", "Format error: ${e.message}")
            "-"
        }
    }


    class InvoiceRentalResponseDiffCallback : DiffUtil.ItemCallback<InvoiceRentalResponse>() {
        override fun areItemsTheSame(
            oldItem: InvoiceRentalResponse,
            newItem: InvoiceRentalResponse
        ): Boolean {
            return oldItem.id_invoice_rental == newItem.id_invoice_rental // Assuming username is unique
        }

        override fun areContentsTheSame(
            oldItem: InvoiceRentalResponse,
            newItem: InvoiceRentalResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}