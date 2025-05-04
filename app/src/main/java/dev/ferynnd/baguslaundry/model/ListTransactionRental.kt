package dev.ferynnd.baguslaundry.model

import com.google.gson.annotations.SerializedName

data class ListTransactionRental(
    val id_list_transaction_rental: Int? = null,
    val id_rental_transaction: Int? = null,
    val id_item_rental: Int? = null,
    val type_list_rental_transaction: TypeListTransactionRental,
    val status_list_transaction_rental: StatusListTransactionRental,
    val condition_list_transaction_rental: ConditionListTransactionRental,
    val note_list_transaction_rental: String? = null,
    val price_list_transaction_rental: Double? = null,
    val weight_list_transaction_rental: Double? = null,
    val is_active_list_transaction_rental: IsActiveListTransactionRental,
    val deleted_at: Any?,
)

enum class TypeListTransactionRental {
    @SerializedName("bath towel")
    BATH_TOWEL,
    @SerializedName("hand towel")
    HAND_TOWEL,
    @SerializedName("gorden")
    GORDEN,
    @SerializedName("keset")
    KESET
}

enum class IsActiveListTransactionRental {
    active,
    inactive
}

enum class ConditionListTransactionRental {
    clean,
    dirty,
    demaged
}

enum class StatusListTransactionRental {
    rented,
    returned,
    cancelled
}
