package dev.ferynnd.baguslaundry.controller.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.KurirCardListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import dev.ferynnd.baguslaundry.model.StatusListTransactionLaundry
import java.text.NumberFormat
import java.util.Locale


class ListLaundryTransaksiAdapter :
    ListAdapter<ListTransactionLaundry, ListLaundryTransaksiAdapter.ListTransactionLaundryViewHolder>(
        DiffCallback()
    ) {

    inner class ListTransactionLaundryViewHolder(val binding: KurirCardListTransaksiLaundryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val status = binding.statusTransaksiLaundry
        val harga = binding.hargaProductLaundry
        val berat = binding.beratProductLaundry
        val total = binding.totalHargaProductLaundry
        val note = binding.noteTransaksiLaundry

//        init {
//            itemView.setOnClickListener {
//                onItemClick(getItem(adapterPosition))
//            }
//        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListTransactionLaundryViewHolder {
        val binding = KurirCardListTransaksiLaundryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ListTransactionLaundryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListTransactionLaundryViewHolder, position: Int) {
        val report = getItem(position)
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        // Status Enum
        holder.status.text = when (report.status_list_transaction_laundry) {
            StatusListTransactionLaundry.pending -> "SEDANG DIPROSES"
            StatusListTransactionLaundry.completed -> "SELESAI"
            StatusListTransactionLaundry.cancelled -> "DIBATALKAN"
        }

        // Menampilkan data lainnya
        holder.harga.text = numberFormat.format(report.price_list_transaction_laundry ?: 0)
        holder.berat.text = (report.weight_list_transaction_laundry ?: 0.0).toString() + " Kg"

        val totalHarga = report.price_list_transaction_laundry?.let { price ->
            val weight = report.weight_list_transaction_laundry?.toDouble() ?: 0.0
            price * weight
        } ?: 0.0

        holder.total.text = numberFormat.format(totalHarga)
        holder.note.text = report.note_list_transaction_laundry ?: "-"

//        holder.itemView.setOnClickListener {
//            onItemClick(report)
//        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ListTransactionLaundry>() {
        override fun areItemsTheSame(
            oldItem: ListTransactionLaundry,
            newItem: ListTransactionLaundry
        ): Boolean {
            return oldItem.id_list_transaction_laundry == newItem.id_list_transaction_laundry
        }

        override fun areContentsTheSame(
            oldItem: ListTransactionLaundry,
            newItem: ListTransactionLaundry
        ): Boolean {
            return oldItem == newItem
        }
    }
}
