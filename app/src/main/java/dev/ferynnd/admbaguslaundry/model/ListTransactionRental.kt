package dev.ferynnd.admbaguslaundry.model

import com.google.gson.annotations.SerializedName

data class ListTransactionRental(
    val id_list_transaction_rental: Int? = null,
    val id_rental_transaction: Int? = null,
    val id_item_rental: Int? = null,
    val status_list_transaction_rental: StatusListTransactionRental,
    val condition_list_transaction_rental: ConditionListTransactionRental,
    val count_list_transaction_rental: Int? = null,
    val weight_list_transaction_rental: Double? = null,
    val is_active_list_transaction_rental: IsActiveListTransactionRental? = IsActiveListTransactionRental.active,
    val deleted_at: Any?,
)

enum class IsActiveListTransactionRental {
    active,
    inactive
}

enum class ConditionListTransactionRental {
    clean,
    dirty,
    damaged
}


enum class StatusListTransactionRental {
    @SerializedName("in")
    IN,
    @SerializedName("out")
    OUT,
    @SerializedName("cancelled")
    CANCELLED
}