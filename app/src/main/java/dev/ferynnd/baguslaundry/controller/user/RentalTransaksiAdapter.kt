package dev.ferynnd.baguslaundry.controller.user

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.KurirCardTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.StatusTransactionRental
import dev.ferynnd.baguslaundry.model.TypeTransactionRental
import dev.ferynnd.baguslaundry.model.User
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class RentalTransaksiAdapter(
    private val onItemClick: (ReportRental) -> Unit
) : ListAdapter<ReportRental, RentalTransaksiAdapter.ReportRentalViewHolder>(DiffCallback()) {

    private var client: List<Client> = emptyList()
    private var kurir: List<User> = emptyList()

    fun setClient(clientList: List<Client>) {
        client = clientList
        notifyDataSetChanged()
    }

    fun setKurir(kurirList: List<User>) {
        kurir = kurirList
        notifyDataSetChanged()
    }

    inner class ReportRentalViewHolder(val binding: KurirCardTransaksiRentalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val wadah_status = binding.wadahStatus
        val status = binding.statusTransaksiRental
        val tipe = binding.tipeTransaksiRental
        val tanggal = binding.tanggalTransaksiRental
        val namaClient = binding.namaClientTransaksiRental
        val namaPenerima = binding.namaPenerimaTransaksiRental
        val namaKurir = binding.namaKurirTransaksiRental
        val jumlahItem = binding.jumlahItemTransaksiRental
        val beratPerKg = binding.hargaPerKgTransaksiRental
        val totalBerat = binding.totalBeratTransaksiRental
        val totalHarga = binding.totalHargaTransaksiRental
        val promo = binding.promoTransaksiRental
        val tambahan = binding.tambahanTransaksiRental
        val note = binding.noteTransaksiRental

        init {
            itemView.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportRentalViewHolder {
        val binding = KurirCardTransaksiRentalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportRentalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportRentalViewHolder, position: Int) {
        val report = getItem(position)
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        val decimalFormat = DecimalFormat("#,##0.##")
        val context = holder.itemView.context

        // Status
        holder.status.text = when (report.status_transaction_rental) {
            StatusTransactionRental.WAITING_FOR_APPROVAL -> "MENUNGGU PERSETUJUAN"
            StatusTransactionRental.APPROVED -> "DISETUJUI"
            StatusTransactionRental.OUT -> "KELUAR"
            StatusTransactionRental.IN -> "MASUK"
            StatusTransactionRental.CANCELLED -> "DIBATALKAN"
        }
        when (report.status_transaction_rental) {
            StatusTransactionRental.WAITING_FOR_APPROVAL -> holder.wadah_status.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.transaksiOuther)
            )

            StatusTransactionRental.APPROVED -> holder.wadah_status.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.transaksiOuther)
            )

            StatusTransactionRental.OUT -> holder.wadah_status.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.transaksiOut)
            )

            StatusTransactionRental.IN -> holder.wadah_status.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.transaksiIn)
            )

            StatusTransactionRental.CANCELLED -> holder.wadah_status.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.transaksiCancelled)
            )
        }

        // Tipe
        holder.tipe.text = when (report.type_rental_transaction) {
            TypeTransactionRental.BATH_TOWEL -> "Bath Towel"
            TypeTransactionRental.HAND_TOWEL -> "Hand Towel"
            TypeTransactionRental.GORDEN -> "Gorden"
            TypeTransactionRental.KESET -> "Keset"
        }

        val namaClient =
            client.find { it.id_client == report.id_client_transaction_rental }?.name_client ?: "-"
        holder.namaClient.text = namaClient.toString()

        val namaKurir =
            kurir.find { it.id_user == report.id_kurir_transaction_rental }?.fullname_user ?: "-"
        holder.namaKurir.text = namaKurir.toString()

        holder.namaPenerima.text = report.recipient_name_transaction_rental ?: "-"

        holder.tanggal.text = report.time_transaction_rental ?: "-"

        holder.jumlahItem.text = "${report.total_pcs_transaction_rental ?: 0} pcs"
        holder.totalBerat.text =
            "${decimalFormat.format(report.total_weight_transaction_rental ?: 0.0)} kg"

        // Harga dan biaya lainnya
        holder.totalHarga.text = numberFormat.format(report.total_price_transaction_rental ?: 0.0)
        holder.promo.text = numberFormat.format(report.promo_transaction_rental ?: 0.0)
        holder.tambahan.text = numberFormat.format(report.additional_cost_transaction_rental ?: 0.0)
        holder.beratPerKg.text = "${numberFormat.format(report.price_weight_transaction_rental)}/kg"
        holder.note.text = report.notes_transaction_rental ?: "-"

        holder.itemView.setOnClickListener {
            onItemClick(report)
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
