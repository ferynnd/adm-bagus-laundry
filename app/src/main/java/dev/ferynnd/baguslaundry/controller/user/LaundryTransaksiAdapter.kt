package dev.ferynnd.baguslaundry.controller.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.KurirCardTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import java.text.NumberFormat
import java.util.Locale


class LaundryTransaksiAdapter(
    private val onItemClick: (ReportLaundry) -> Unit
) : ListAdapter<ReportLaundry, LaundryTransaksiAdapter.ReportLaundryViewHolder>(DiffCallback()) {

    inner class ReportLaundryViewHolder(val binding: KurirCardTransaksiLaundryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val status = binding.statusTransaksiLaundry
        val harga = binding.hargaTransaksiLaundry
        val tanggalMasuk = binding.tanggalMasukTransaksiLaundry
        val tanggalKeluar = binding.tanggalKeluarTransaksiLaundry
        val namaPelanggan = binding.namaPelangganTransaksiLaundry
        val berat = binding.beratTransaksiLaundry
        val total = binding.totalHargaTransaksiLaundry
        val tunai = binding.tunaiTransaksiLaundry
        val kembalian = binding.kembalianTransaksiLaundry
        val deskripsi = binding.noteTransaksiLaundry

        init {
            itemView.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportLaundryViewHolder {
        val binding = KurirCardTransaksiLaundryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportLaundryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportLaundryViewHolder, position: Int) {
        val report = getItem(position)
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        // Status Enum
        holder.status.text = when (report.status_transaction_laundry) {
            StatusReportLaundry.pending -> "MENUNGGU"
            StatusReportLaundry.in_progress -> "SEDANG DIPROSES"
            StatusReportLaundry.completed -> "SELESAI"
            StatusReportLaundry.cancelled -> "DIBATALKAN"
        }

        // Menampilkan data lainnya
        holder.harga.text = numberFormat.format(report.total_transaction_laundry ?: 0.0)
        holder.total.text = numberFormat.format(report.total_transaction_laundry ?: 0.0)
        holder.tunai.text = numberFormat.format(report.cash_transaction_laundry ?: 0.0)
        holder.kembalian.text = numberFormat.format(report.change_money_transaction_laundry ?: 0.0)
        holder.tanggalMasuk.text = report.first_date_transaction_laundry ?: "-"
        holder.tanggalKeluar.text = report.last_date_transaction_laundry ?: "-"
        holder.namaPelanggan.text = report.name_client_transaction_laundry ?: "-"
        holder.berat.text = (report.total_weight_transaction_laundry ?: 0.0).toString()
        holder.deskripsi.text = report.notes_transaction_laundry ?: "-"

        holder.itemView.setOnClickListener {
            onItemClick(report)
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
