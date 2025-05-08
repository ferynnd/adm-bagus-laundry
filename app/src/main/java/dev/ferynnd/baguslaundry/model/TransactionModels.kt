package dev.ferynnd.baguslaundry.model

data class LaundryTransactionRequest(
    val id_user_transaction_laundry: Int,
    val id_branch_transaction_laundry: Int,
    val name_client_transaction_laundry: String,
    val notes_transaction_laundry: String,
    val promo_transaction_laundry: Double = 0.0,
    val additional_cost_transaction_laundry: Double = 0.0,
    val cash_transaction_laundry: Double = 0.0,
    val list_transaction_laundry: List<LaundryTransactionItem>
)

data class LaundryTransactionItem(
    val id_item_laundry: Int?,
    val weight_list_transaction_laundry: Double = 0.0,
    val note_list_transaction_laundry: String = ""
)

data class LaundryTransactionResponse(
    val success: Boolean,
    val message: String,
    val data: TransactionData?,
    val errors: String? = null
)

data class TransactionData(
    val id_transaction_laundry: Int,
    val id_user_transaction_laundry: Int,
    val id_branch_transaction_laundry: Int,
    val name_client_transaction_laundry: String,
    val notes_transaction_laundry: String,
    val promo_transaction_laundry: Double,
    val additional_cost_transaction_laundry: Double,
    val cash_transaction_laundry: Double,
    val total_price_transaction_laundry: Double,
    val timestamp_transaction_laundry: String,
    val list_transaction_laundry: List<TransactionItemDetail>
)

data class TransactionItemDetail(
    val id_list_transaction_laundry: Int,
    val id_transaction_laundry: Int,
    val id_item_laundry: Int,
    val weight_list_transaction_laundry: Double,
    val price_list_transaction_laundry: Double,
    val subtotal_list_transaction_laundry: Double,
    val note_list_transaction_laundry: String,
    val item_detail: LaundryItemDetail
)

data class LaundryItemDetail(
    val id_laundry_item: Int,
    val name_laundry_item: String,
    val time_laundry_item: String,
    val price_laundry_item: Double
)
