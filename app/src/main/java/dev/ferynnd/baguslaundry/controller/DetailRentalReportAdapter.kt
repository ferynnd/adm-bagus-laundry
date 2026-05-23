package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.admbaguslaundry.databinding.CardListDetailReportRentalBinding
import dev.ferynnd.admbaguslaundry.model.ConditionListTransactionRental
import dev.ferynnd.admbaguslaundry.model.ListTransactionRental
import dev.ferynnd.admbaguslaundry.model.ProductRental
import dev.ferynnd.admbaguslaundry.model.StatusListTransactionRental

class DetailRentalReportAdapter : ListAdapter<ListTransactionRental, DetailRentalReportAdapter.DetailRentalReportViewHolder>(DiffCallback()) {

     private var productRentals: List<ProductRental> = emptyList()

    fun setProductRental(productRental: List<ProductRental>) {
        productRentals = productRental
        notifyDataSetChanged()
    }

    inner class DetailRentalReportViewHolder( val binding: CardListDetailReportRentalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailRentalReportViewHolder {
        val binding = CardListDetailReportRentalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailRentalReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailRentalReportViewHolder, position: Int) {
        val detailReportRental = getItem(position)

         val productRentalName = productRentals.find { it.id_rental_item == detailReportRental.id_item_rental }?.name_rental_item ?: "Tanpa Nama"

        holder.binding.apply {
            inputName.text = productRentalName
            inputNumber.text = detailReportRental.id_item_rental.toString()
            inputPrice.text = detailReportRental.count_list_transaction_rental.toString()
            inputWeight.text = detailReportRental.weight_list_transaction_rental.toString()


            val dataCondition = when(detailReportRental.condition_list_transaction_rental) {
                ConditionListTransactionRental.dirty -> "Kotor"
                ConditionListTransactionRental.clean -> "Bersih"
                ConditionListTransactionRental.damaged -> "Rusak"
                else -> "Unknown"
            }

            inputCondition.text = dataCondition

            val dataStatus = when(detailReportRental.status_list_transaction_rental) {
                StatusListTransactionRental.IN -> "Masuk"
                StatusListTransactionRental.OUT -> "Keluar"
                StatusListTransactionRental.CANCELLED -> "Dibatalkan"
                else -> "Unknown"
            }

            inputStatus.text = dataStatus
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ListTransactionRental>() {
        override fun areItemsTheSame(oldItem: ListTransactionRental, newItem: ListTransactionRental): Boolean {
            return oldItem.id_list_transaction_rental == newItem.id_list_transaction_rental
        }

        override fun areContentsTheSame(oldItem: ListTransactionRental, newItem: ListTransactionRental): Boolean {
            return oldItem == newItem
        }
    }

}