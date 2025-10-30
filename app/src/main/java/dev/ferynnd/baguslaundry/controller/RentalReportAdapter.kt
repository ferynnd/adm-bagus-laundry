package dev.ferynnd.baguslaundry.controller

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.CardHeaderBinding
import dev.ferynnd.baguslaundry.databinding.CardReportRentalBinding
import dev.ferynnd.baguslaundry.databinding.ItemRentalServiceBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ListTransactionRental
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.User
import java.util.Locale

class RentalReportAdapter(
    private val onDetail: (ReportRental) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()
    private var sender: List<User> = emptyList()
    private var client: List<Client> = emptyList()
    private var rentalProducts: List<ProductRental> = emptyList()
    private var rentalListItem: List<ListTransactionRental> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    fun setSender(senderList: List<User>) {
        sender = senderList
        notifyDataSetChanged()
    }

    fun setClient(clientList: List<Client>) {
        client = clientList
        notifyDataSetChanged()
    }

    fun setRentalProducts(productList: List<ProductRental>) {
        rentalProducts = productList
        notifyDataSetChanged()
    }

    fun setRentalList(list: List<ListTransactionRental>) {
        rentalListItem = list
        notifyDataSetChanged()
    }

    inner class ReportRentalViewHolder(val binding: CardReportRentalBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderViewHolder(val binding: CardHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { HEADER, CONTENT }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_VIEW.HEADER.ordinal -> {
                val binding = CardHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return HeaderViewHolder(binding)
            }

            TYPE_VIEW.CONTENT.ordinal -> {
                val binding = CardReportRentalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ReportRentalViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReportRental -> TYPE_VIEW.CONTENT.ordinal
            is String -> TYPE_VIEW.HEADER.ordinal
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ReportRentalViewHolder -> {
                val transactionReportRental = item as ReportRental
                val branchName = branches.find {
                    it.id_branch == transactionReportRental.id_branch_transaction_rental
                }?.name_branch ?: "Unknown"

                val senderName = sender.find {
                    it.id_user == transactionReportRental.id_kurir_transaction_rental
                }?.fullname_user ?: "Unknown"

                val clientName = client.find {
                    it.id_client == transactionReportRental.id_client_transaction_rental
                }?.name_client ?: "Unknown"

                holder.binding.apply {
                    inputBranch.text = branchName
                    inputCLient.text = clientName
                    inputSender.text = senderName
                    inputRecipient.text = transactionReportRental.recipient_name_transaction_rental
                    inputNotes.text = transactionReportRental.notes_transaction_rental.takeIf {
                        !it.isNullOrBlank()
                    } ?: "-"
                    inputTime.text = transactionReportRental.time_transaction_rental
                    idTransactionRental.text = transactionReportRental.number_transaction_rental.toString()

                    // Setup RecyclerView untuk list items dengan null check
                    setupItemsRecyclerView(holder, transactionReportRental)

                    buttonDetail.setOnClickListener {
                        onDetail(transactionReportRental)
                    }
                }
            }

            is HeaderViewHolder -> {
                val header = item as String
                holder.binding.inputNameBranch.text = header
                val context = holder.binding.root.context
                val color = ContextCompat.getColor(context, R.color.blueGray)
                holder.binding.root.setCardBackgroundColor(color)
            }
        }
    }

    private fun setupItemsRecyclerView(holder: ReportRentalViewHolder, transaction: ReportRental) {
        val container = holder.binding.layoutItemsContainer
        container.removeAllViews()

        // Ambil semua item transaksi dari rentalListItem yang sesuai dengan transaksi ini
        val matchingItems = rentalListItem.filter {
            it.id_rental_transaction == transaction.id_transaction_rental
        }

        Log.d("RentalAdapter", "Transaction ID: ${transaction.id_transaction_rental}, Matching items: ${matchingItems.size}")

        if (matchingItems.isEmpty()) {
            val emptyView = TextView(holder.itemView.context).apply {
                text = "Tidak ada item"
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            container.addView(emptyView)
            return
        }

        val context = holder.itemView.context
        val inflater = LayoutInflater.from(context)

        matchingItems.forEach { item ->
            val itemId = item.id_item_rental ?: return@forEach
            Log.d("RentalAdapter", "Matching: itemId=$itemId, ProductListSize=${rentalProducts.size}")
            val productName = rentalProducts.find {
                it.id_rental_item == itemId
            }?.name_rental_item ?: "Layanan Tidak Dikenal"

            val itemView = inflater.inflate(R.layout.item_rental_service, container, false)
            val bindingItem = ItemRentalServiceBinding.bind(itemView)

            bindingItem.apply {
                tvServiceName.text = productName
                tvConditionStatus.text =
                    "${item.condition_list_transaction_rental ?: "-"} - ${item.status_list_transaction_rental ?: "-"}"
                tvWeight.text = "${item.weight_list_transaction_rental ?: 0.0} Kg"
                tvQuantity.text = "${item.count_list_transaction_rental ?: 0} PCS"
            }

            container.addView(itemView)
        }
    }


    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ReportRental && newItem is ReportRental -> {
                    oldItem.id_transaction_rental == newItem.id_transaction_rental
                }
                oldItem is String && newItem is String -> {
                    oldItem == newItem
                }
                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ReportRental && newItem is ReportRental -> {
                    oldItem.id_transaction_rental == newItem.id_transaction_rental &&
                            oldItem.number_transaction_rental == newItem.number_transaction_rental &&
                            oldItem.time_transaction_rental == newItem.time_transaction_rental &&
                            oldItem.id_branch_transaction_rental == newItem.id_branch_transaction_rental &&
                            oldItem.id_client_transaction_rental == newItem.id_client_transaction_rental &&
                            oldItem.id_kurir_transaction_rental == newItem.id_kurir_transaction_rental &&
                            oldItem.recipient_name_transaction_rental == newItem.recipient_name_transaction_rental &&
                            oldItem.notes_transaction_rental == newItem.notes_transaction_rental &&
                            oldItem.list_transaction_rentals == newItem.list_transaction_rentals
                }
                oldItem is String && newItem is String -> {
                    oldItem == newItem
                }
                else -> false
            }
        }
    }
}