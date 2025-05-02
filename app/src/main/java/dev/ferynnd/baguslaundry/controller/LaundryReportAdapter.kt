package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardHeaderBinding
import dev.ferynnd.baguslaundry.databinding.CardReportLaundryBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ReportLaundry

class LaundryReportAdapter ( private val onDetail : (ReportLaundry) -> Unit) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {
//ReportLaundryReportAdapter (private val onEdit : (ReportLaundry) -> Unit, private val onDelete : (ReportLaundry) -> Unit ) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private var branches: List<Branch> = emptyList()

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }

    inner class ReportLaundryViewHolder( val binding: CardReportLaundryBinding) : RecyclerView.ViewHolder(binding.root)
    inner class HeaderViewHolder( val binding: CardHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    enum class TYPE_VIEW { HEADER , CONTENT }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType) {
            TYPE_VIEW.HEADER.ordinal -> {
                val binding = CardHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return HeaderViewHolder(binding)
            }

            TYPE_VIEW.CONTENT.ordinal -> {
                val binding = CardReportLaundryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ReportLaundryViewHolder(binding)
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
            val item = getItem(position)
            when (holder) {
                is ReportLaundryViewHolder -> {
                    val transactionReportLaundry = item as ReportLaundry

                    val branchName = branches.find { it.id_branch == transactionReportLaundry.id_branch_transaction_laundry }?.name_branch ?: "Unknown"

                    holder.binding.apply {
                        inputBranch.text = branchName
                        inputCustommer.text = transactionReportLaundry.name_client_transaction_laundry.toString()
                        inputEmployment.text = transactionReportLaundry.id_user_transaction_laundry.toString()
                        inputStatus.text = transactionReportLaundry.status_transaction_laundry.toString()
                        inputWeight.text = transactionReportLaundry.total_weight_transaction_laundry.toString()
                        inputTotalPrice.text = transactionReportLaundry.total_price_transaction_laundry.toString()
                        inputCash.text = transactionReportLaundry.cash_transaction_laundry.toString()
                        inputChangeMoney.text = transactionReportLaundry.change_money_transaction_laundry.toString()
                        inputNotes.text = transactionReportLaundry.notes_transaction_laundry

                        buttonDetail.setOnClickListener {
                            onDetail(transactionReportLaundry)
                        }
                    }
                }
                is HeaderViewHolder -> {
                    val header = item as String
                    holder.binding.inputNameBranch.text = header
                }
            }
        }


      class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when ( oldItem) {
                is ReportLaundry -> {
                    if (newItem is ReportLaundry) {
                        (oldItem.id_transaction_laundry) == (newItem.id_transaction_laundry)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ReportLaundry) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when ( oldItem) {
                is ReportLaundry -> {
                    if (newItem is ReportLaundry) {
                        (oldItem) == (newItem)
                    } else {
                        false
                    }
                }
                else -> {
                    if (newItem is ReportLaundry) {
                        false
                    } else {
                        (oldItem) == (newItem)
                    }
                }
            }
        }
    }
}