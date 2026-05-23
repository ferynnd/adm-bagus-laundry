package dev.ferynnd.baguslaundry.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

data class TransactionData(
    val id_transaction_laundry: Int? = null,
    val id_kurir_transaction_laundry: Int ? = null,
    val id_branch_transaction_laundry: Int ? = null,
    val name_client_transaction_laundry: String ? = null,
    val number_transaction_laundry : String ? = null,
    val status_transaction_laundry: StatusReportLaundry? = null,
    val notes_transaction_laundry: String ? = null,
    val total_weight_transaction_laundry: BigDecimal ? = null,
    val total_price_transaction_laundry: BigDecimal ? = null,
    val count_item_laundry_transaction_laundry: Int ? = null,
    val promo_transaction_laundry: BigDecimal ? = null,
    val additional_cost_transaction_laundry: BigDecimal ? = null,
    val cash_transaction_laundry: BigDecimal ? = null,
    val change_money_transaction_laundry: BigDecimal ? = null,
    val total_transaction_laundry: BigDecimal ? = null,
    val is_active_transaction_laundry: String ? = null,
    val first_date_transaction_laundry: String ? = null,
    val last_date_transaction_laundry: String ? = null,
    val list_transaction_laundry: List<ListTransactionLaundry>? = null
)

@Parcelize
data class LaundryTransactionState(
    val clientName: String = "",
    val cashAmount: BigDecimal = BigDecimal.ZERO,
    val notes: String = "",
    val additionalCost: BigDecimal = BigDecimal.ZERO,
    val promoAmount: BigDecimal = BigDecimal.ZERO,
     val selectedItems: List<ProductLaundry> = emptyList()
) : Parcelable


data class UpdateLaundryFullRequest(
    val cash_transaction_laundry: Int,
    val transaction: UpdateLaundryTransactionData,
    val list_items: List<UpdateLaundryItem>
)

data class UpdateLaundryTransactionData(
    val id_kurir_transaction_laundry: Int?,
    val id_branch_transaction_laundry: Int?,
    val name_client_transaction_laundry: String,
    val status_transaction_laundry: String,
    val notes_transaction_laundry: String?
)

data class UpdateLaundryItem(
    val id_item_laundry: Int,
    val weight_list_transaction_laundry: Int
)