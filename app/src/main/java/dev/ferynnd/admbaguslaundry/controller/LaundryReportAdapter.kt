package dev.ferynnd.admbaguslaundry.controller

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.databinding.CardHeaderBinding
import dev.ferynnd.admbaguslaundry.databinding.CardReportLaundryBinding
import dev.ferynnd.admbaguslaundry.databinding.ItemLaundryServiceBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.ProductLaundry
import dev.ferynnd.admbaguslaundry.model.ReportLaundry
import dev.ferynnd.admbaguslaundry.model.StatusReportLaundry
import dev.ferynnd.admbaguslaundry.model.User
import java.text.NumberFormat
import java.util.Locale

class LaundryReportAdapter(
    private val onPrint: (ReportLaundry) -> Unit,
    private val onDetail: (ReportLaundry) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()
    private var users: List<User> = emptyList()
    private var laundryProducts: List<ProductLaundry> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    fun setUsers(userList: List<User>) {
        users = userList
        notifyDataSetChanged()
    }

    fun setLaundryProducts(productList: List<ProductLaundry>) {
        laundryProducts = productList
        notifyDataSetChanged()
    }

    inner class ReportLaundryViewHolder(val binding: CardReportLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderViewHolder(val binding: CardHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW {
        HEADER,
        CONTENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_VIEW.HEADER.ordinal -> {
                val binding = CardHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }

            TYPE_VIEW.CONTENT.ordinal -> {
                val binding = CardReportLaundryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReportLaundryViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReportLaundry -> TYPE_VIEW.CONTENT.ordinal
            is String -> TYPE_VIEW.HEADER.ordinal
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ReportLaundryViewHolder -> {
                val transactionReportLaundry = getItem(position) as ReportLaundry

                val branchName = branches.find {
                    it.id_branch == transactionReportLaundry.id_branch_transaction_laundry
                }?.name_branch ?: "Unknown"

                val userName = users.find {
                    it.id_user == transactionReportLaundry.id_kurir_transaction_laundry
                }?.fullname_user ?: "Unknown"

                val dataStatus = when (transactionReportLaundry.status_transaction_laundry) {
                    StatusReportLaundry.paid -> "Sudah Dibayar"
                    StatusReportLaundry.unpaid -> "Belum Dibayar"
                    StatusReportLaundry.completed -> "Selesai"
                    StatusReportLaundry.cancelled -> "Dibatalkan"
                }

                val localeID = Locale("in", "ID")
                val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
                val hargaTotal = transactionReportLaundry.total_price_transaction_laundry ?: 0

                holder.binding.apply {
                    idTransactionLaundry.text =
                        transactionReportLaundry.number_transaction_laundry.toString()

                    inputBranch.text = branchName
                    inputStatus.text = dataStatus
                    inputEmployment.text = userName
                    inputCustommer.text =
                        transactionReportLaundry.name_client_transaction_laundry ?: "-"

                    inputWeight.text =
                        "${transactionReportLaundry.total_weight_transaction_laundry ?: 0} Kg"

                    inputCountItem.text =
                        transactionReportLaundry.count_item_transaction_laundry.toString()

                    inputTotalPrice.text = formatRupiah.format(hargaTotal)

                    inputNotes.text =
                        transactionReportLaundry.notes_transaction_laundry ?: "-"

                    setupItemsContainer(holder, transactionReportLaundry)

                    buttonDetail.setOnClickListener {
                        onDetail(transactionReportLaundry)
                    }

                    buttonPrint.setOnClickListener {
                        onPrint(transactionReportLaundry)
                    }
                }
            }

            is HeaderViewHolder -> {
                val header = getItem(position) as String
                holder.binding.inputNameBranch.text = header
            }
        }
    }

    private fun setupItemsContainer(
        holder: ReportLaundryViewHolder,
        report: ReportLaundry
    ) {
        val container = holder.binding.layoutItemsContainer
        container.removeAllViews()

        val items = report.list_transaction_laundry ?: emptyList()

        if (items.isEmpty()) {
            val emptyView = TextView(holder.itemView.context).apply {
                text = "Tidak ada item"
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.darker_gray
                    )
                )
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }

            container.addView(emptyView)
            return
        }

        val inflater = LayoutInflater.from(holder.itemView.context)
        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)

        items.forEach { item ->
            val itemView = inflater.inflate(
                R.layout.item_laundry_service,
                container,
                false
            )

            val bindingItem = ItemLaundryServiceBinding.bind(itemView)

            Log.d("LAUNDRY_ITEM", "====================")
            Log.d("LAUNDRY_ITEM", "ITEM ID : ${item.id_item_laundry}")

            laundryProducts.forEach {
                Log.d(
                    "LAUNDRY_ITEM",
                    "PRODUCT -> ID=${it.id_laundry_item}, NAME=${it.name_laundry_item}"
                )
            }

            val productName = laundryProducts.find {
                it.id_laundry_item == item.id_item_laundry
            }?.name_laundry_item ?: "Layanan Tidak Dikenal"

            Log.d("LAUNDRY_ITEM", "MATCH RESULT : $productName")
            Log.d("LAUNDRY_ITEM", "====================")

            Log.d("NAME", productName)

            bindingItem.tvServiceName.text = productName

            bindingItem.tvPrice.text =
                "${formatRupiah.format(item.price_list_transaction_laundry ?: 0)} / Kg"

            bindingItem.tvWeight.text =
                "${item.weight_list_transaction_laundry ?: 0} Kg"

            bindingItem.tvTotalPrice.text =
                formatRupiah.format(item.total_price_list_transaction_laundry ?: 0)

            container.addView(itemView)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ReportLaundry && newItem is ReportLaundry ->
                    oldItem.id_transaction_laundry == newItem.id_transaction_laundry

                oldItem is String && newItem is String ->
                    oldItem == newItem

                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }
    }
}