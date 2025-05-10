package dev.ferynnd.baguslaundry.model

data class ListTransactionLaundry(
    val id_list_transaction_laundry: Int? = null,
    val id_transaction_laundry: Int? = null,
    val id_item_laundry: Int? = null,
    val status_list_transaction_laundry: StatusListTransactionLaundry,
    val weight_list_transaction_laundry: Double? = null,
    val price_list_transaction_laundry: Double? = null,
    val note_list_transaction_laundry: String? = null,
    val is_active_list_transaction_laundry:IsActiveListTransactionLaundry,
    val deleted_at: Any?,
)
enum class StatusListTransactionLaundry {
    pending,
    completed,
    cancelled
}

enum class IsActiveListTransactionLaundry {
    active,
    inactive
}
