package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardListDetailReportLaundryBinding
import dev.ferynnd.baguslaundry.model.IsActiveListTransactionLaundry
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.StatusListTransactionLaundry
import java.text.NumberFormat
import java.util.Locale

class DetailLaundryReportAdapter :
    ListAdapter<ListTransactionLaundry, DetailLaundryReportAdapter.DetailLaundryReportViewHolder>(
        DiffCallback()
    ) {

    private var productLaundrys: List<ProductLaundry> = emptyList()

    fun setProductLaundry(productLaundry: List<ProductLaundry>) {
        productLaundrys = productLaundry
        notifyDataSetChanged()
    }

    inner class DetailLaundryReportViewHolder(val binding: CardListDetailReportLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetailLaundryReportViewHolder {
        val binding = CardListDetailReportLaundryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetailLaundryReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailLaundryReportViewHolder, position: Int) {
        val detailReportLaundry = getItem(position)

        val productLaundryName =
            productLaundrys.find { it.id_laundry_item == detailReportLaundry.id_item_laundry }?.name_laundry_item
                ?: "Tanpa Nama"

        holder.binding.apply {
            namaTransaksiLaundry.text = productLaundryName
            val localeID = Locale("in", "ID")
            val formatRupiah = NumberFormat.getCurrencyInstance(localeID)

            val harga = detailReportLaundry.price_list_transaction_laundry
            hargaProductLaundry.text = formatRupiah.format(harga)
            val totalHarga = detailReportLaundry.total_price_list_transaction_laundry
            totalHargaProductLaundry.text = formatRupiah.format(totalHarga)

            beratProductLaundry.text =
                "${detailReportLaundry.weight_list_transaction_laundry.toString()} Kg"
//            inputNumber.text = detailReportLaundry.id_transaction_laundry.toString()
//            inputNameItem.text = productLaundryName
            val dataStatus = when (detailReportLaundry.is_active_list_transaction_laundry) {
                IsActiveListTransactionLaundry.active -> "AKTIF"
                IsActiveListTransactionLaundry.inactive -> "TIDAK AKTIF"
                else -> null
            }
//            inputStatus.text = dataStatus
        }
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