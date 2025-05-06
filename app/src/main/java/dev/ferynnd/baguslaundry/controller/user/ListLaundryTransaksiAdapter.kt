package dev.ferynnd.baguslaundry.controller.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.KurirCardListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.StatusListTransactionLaundry
import dev.ferynnd.baguslaundry.model.StatusTransactionRental
import java.text.NumberFormat
import java.util.Locale


class ListLaundryTransaksiAdapter :
    ListAdapter<ListTransactionLaundry, ListLaundryTransaksiAdapter.ListTransactionLaundryViewHolder>(
        DiffCallback()
    ) {

    private var laundryTransaksiItem: List<ProductLaundry> = emptyList()

    fun setLaundryTransaksitem(laundryTransaksiItemList: List<ProductLaundry>) {
        laundryTransaksiItem = laundryTransaksiItemList
        notifyDataSetChanged()
    }

    inner class ListTransactionLaundryViewHolder(val binding: KurirCardListTransaksiLaundryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val nama = binding.namaTransaksiLaundry
        val wadah_status = binding.wadahStatus
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
        val context = holder.itemView.context

        val nama_list_transaksi_laundry =
            laundryTransaksiItem.find { it.id_laundry_item == report.id_item_laundry }?.name_laundry_item
                ?: "-"
        holder.nama.text = nama_list_transaksi_laundry

        // Status Enum
        val status = when (report.status_list_transaction_laundry) {
            StatusListTransactionLaundry.pending -> "MENUNGGU"
            StatusListTransactionLaundry.completed -> "SELESAI"
            StatusListTransactionLaundry.cancelled -> "DIBATALKAN"
        }
        holder.status.text = status
        when (report.status_list_transaction_laundry) {
            StatusListTransactionLaundry.pending -> holder.wadah_status.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.transaksiOuther)
            )

            StatusListTransactionLaundry.completed ->
                holder.wadah_status.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.transaksiIn)
                )

            StatusListTransactionLaundry.cancelled ->
                holder.wadah_status.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.transaksiCancelled)
                )
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
