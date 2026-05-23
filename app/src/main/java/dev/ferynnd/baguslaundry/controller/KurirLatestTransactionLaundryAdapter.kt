package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.KurirCardListTerbaruLaundryBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import java.text.NumberFormat
import java.util.Locale

class KurirLatestTransactionLaundryAdapter :
    ListAdapter<ReportLaundry, KurirLatestTransactionLaundryAdapter.ReportLaundryViewHolder>(
        DiffCallback()
    ) {

    private val numberFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

    inner class ReportLaundryViewHolder(val binding: KurirCardListTerbaruLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface OnTransactionActionListener {
        fun onCompleteTransactionClicked(reportLaundry: ReportLaundry)
    }

    private var transactionActionListener: OnTransactionActionListener? = null

    fun setOnTransactionActionListener(listener: OnTransactionActionListener) {
        this.transactionActionListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportLaundryViewHolder {
        val binding = KurirCardListTerbaruLaundryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportLaundryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportLaundryViewHolder, position: Int) {
        val report = getItem(position)

        // ===== LANGSUNG PAKAI DATA YANG SUDAH DIFORMAT DARI VIEWMODEL =====
        holder.binding.apply {
            numberTransaction.text = report.number_transaction_laundry.toString()
            namaPelangganTransaksiLaundry.text = report.name_client_transaction_laundry ?: "-"
            
            // Data sudah formatted dari ViewModel, tidak perlu parsing lagi!
            tanggalMasukTransaksiLaundry.text = report.formatted_first_date ?: "-"
            tanggalKeluarTransaksiLaundry.text = report.formatted_last_date ?: "Tidak ada tanggal keluar"
            
            pcsTransaksiLaundry.text = report.count_item_transaction_laundry?.toString() ?: "0"
            statusTransaksiLaundry.text = report.status_transaction_laundry.toString()
            hargaTransaksiLaundry.text = numberFormatter.format(report.total_transaction_laundry?.toDouble() ?: 0.0)
            beratTransaksiLaundry.text = report.total_weight_transaction_laundry.toString()
            totalHargaTransaksiLaundry.text = numberFormatter.format(report.total_price_transaction_laundry?.toDouble() ?: 0.0)
            tunaiTransaksiLaundry.text = numberFormatter.format(report.cash_transaction_laundry?.toDouble() ?: 0.0)

            btnCompleteTransaction.setOnClickListener {
                transactionActionListener?.onCompleteTransactionClicked(report)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReportLaundry>() {
        override fun areItemsTheSame(oldItem: ReportLaundry, newItem: ReportLaundry): Boolean {
            return oldItem.id_transaction_laundry == newItem.id_transaction_laundry
        }

        override fun areContentsTheSame(oldItem: ReportLaundry, newItem: ReportLaundry): Boolean {
            return oldItem == newItem
        }
    }
}
