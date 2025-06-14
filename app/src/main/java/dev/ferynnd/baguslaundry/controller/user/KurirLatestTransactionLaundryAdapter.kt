package dev.ferynnd.baguslaundry.controller.user

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
            isGroupingUsed = true // Untuk pemisah ribuan (titik)
            maximumFractionDigits = 0 // Ini yang menghilangkan ",00"
            minimumFractionDigits = 0 // Pastikan tidak ada desimal minimal
        }

    // Class ViewHolder yang benar
    inner class ReportLaundryViewHolder(val binding: KurirCardListTerbaruLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Interface untuk aksi klik
    interface OnTransactionActionListener {
        fun onCompleteTransactionClicked(reportLaundry: ReportLaundry)
    }

    // Variabel untuk menyimpan listener
    private var transactionActionListener: OnTransactionActionListener? = null

    // Fungsi untuk mengatur listener
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
        holder.binding.numberTransaction.text = report.number_transaction_laundry.toString()
        holder.binding.namaPelangganTransaksiLaundry.text =
            report.name_client_transaction_laundry.toString()
        holder.binding.tanggalMasukTransaksiLaundry.text =
            report.first_date_transaction_laundry.toString()
        holder.binding.tanggalKeluarTransaksiLaundry.text =
            if (report.last_date_transaction_laundry == null) "Tidak ada tanggal keluar" else report.last_date_transaction_laundry.toString()
        holder.binding.pcsTransaksiLaundry.text =
            if (report.count_item_transaction_laundry == null) "0" else report.count_item_transaction_laundry.toString()
        holder.binding.statusTransaksiLaundry.text = report.status_transaction_laundry.toString()
        holder.binding.hargaTransaksiLaundry.text =
            numberFormatter.format(report.total_transaction_laundry?.toDouble() ?: 0.0)
        holder.binding.beratTransaksiLaundry.text =
            report.total_weight_transaction_laundry.toString()
        holder.binding.totalHargaTransaksiLaundry.text =
            numberFormatter.format(report.total_price_transaction_laundry?.toDouble() ?: 0.0)
        holder.binding.tunaiTransaksiLaundry.text =
            numberFormatter.format(report.cash_transaction_laundry?.toDouble() ?: 0.0)

        // --- PERBAIKAN DI SINI ---
        holder.binding.btnCompleteTransaction.setOnClickListener {
            transactionActionListener?.onCompleteTransactionClicked(report) // Menggunakan 'transactionActionListener'
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