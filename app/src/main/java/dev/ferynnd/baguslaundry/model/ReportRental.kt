package dev.ferynnd.baguslaundry.model

import com.google.gson.annotations.SerializedName


data class ReportRental(
    val id_transaction_rental: Int? = null,
    val id_branch_transaction_rental: Int? = null,
    val id_client_transaction_rental: Int? = null,
    val id_kurir_transaction_rental: Int? = null,
    val recipient_name_transaction_rental: String? = null,
    val type_rental_transaction : TypeTransactionRental,
    val status_transaction_rental:StatusTransactionRental,
    val total_pcs_transaction_rental: Int? = null,
    val total_weight_transaction_rental: Double? = null,
    val additional_cost_transaction_rental: Double? = null,
    val promo_transaction_rental: Double? = null,
    val total_price_transaction_rental: Double? = null,
    val notes_transaction_rental: String? = null,
    val is_active_transaction_rental: IsActiveTransactionRental,
    val list_transaction_rentals : List<ListTransactionRental>,
    val deleted_at: Any? = null,
    val created_at : String? = null,
)

enum class TypeTransactionRental {
    @SerializedName("bath towel")
    BATH_TOWEL,
    @SerializedName("hand towel")
    HAND_TOWEL,
    @SerializedName("gorden")
    GORDEN,
    @SerializedName("keset")
    KESET
}

enum class IsActiveTransactionRental {
    active,
    inactive,
}

enum class StatusTransactionRental {
    @SerializedName("waiting for approval")
    WAITING_FOR_APPROVAL,

    @SerializedName("approved")
    APPROVED,

    @SerializedName("out")
    OUT,

    @SerializedName("in")
    IN,

    @SerializedName("cancelled")
    CANCELLED
}
