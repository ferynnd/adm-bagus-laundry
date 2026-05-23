package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.KurirCardListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.ListTransactionRental
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.StatusListTransactionRental
import java.text.NumberFormat
import java.util.Locale


class ListRentalTransaksiAdapter :
    ListAdapter<ListTransactionRental, ListRentalTransaksiAdapter.ListTransactionRentalViewHolder>(
        DiffCallback()
    ) {

    private var rentalItem: List<ProductRental> = emptyList()

    fun setRentalItem(rentalItemList: List<ProductRental>) {
        rentalItem = rentalItemList
        notifyDataSetChanged()
    }

    inner class ListTransactionRentalViewHolder(val binding: KurirCardListTransaksiRentalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val nomor_type = binding.nomorTypeTransaksiLaundry
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
    ): ListTransactionRentalViewHolder {
        val binding = KurirCardListTransaksiRentalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ListTransactionRentalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListTransactionRentalViewHolder, position: Int) {
        val report = getItem(position)
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        // Status Enum
        holder.status.text = when (report.status_list_transaction_rental) {
            StatusListTransactionRental.IN -> "DISEWA"
            StatusListTransactionRental.OUT -> "DIKEMBALIKAN"
            StatusListTransactionRental.CANCELLED -> "DIBATALKAN"
        }

        val nama_rental = rentalItem.find { it.id_rental_item == report.id_item_rental }?.name_rental_item ?: "-"

        holder.nomor_type.text = nama_rental

        // Menampilkan data lainnya
        holder.harga.text =  "${numberFormat.format(report ?: 0)}/Kg"
        holder.berat.text = (report.weight_list_transaction_rental ?: 0.0).toString() + " Kg"

    }

    class DiffCallback : DiffUtil.ItemCallback<ListTransactionRental>() {
        override fun areItemsTheSame(
            oldItem: ListTransactionRental,
            newItem: ListTransactionRental
        ): Boolean {
            return oldItem.id_item_rental == newItem.id_item_rental
        }

        override fun areContentsTheSame(
            oldItem: ListTransactionRental,
            newItem: ListTransactionRental
        ): Boolean {
            return oldItem == newItem
        }
    }
}
