package dev.ferynnd.baguslaundry.model

import java.math.BigDecimal

data class ListTransactionLaundry(
    val id_list_transaction_laundry: Int? = null,
    val id_transaction_laundry: Int? = null,
    val id_item_laundry: Int? = null,
    val price_list_transaction_laundry: BigDecimal? = null,
    val weight_list_transaction_laundry: BigDecimal? = null,
    val total_price_list_transaction_laundry: BigDecimal? = null,
    val is_active_list_transaction_laundry:IsActiveListTransactionLaundry? = IsActiveListTransactionLaundry.active,
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
