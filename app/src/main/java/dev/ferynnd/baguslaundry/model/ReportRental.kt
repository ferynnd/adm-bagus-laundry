package dev.ferynnd.baguslaundry.model

import com.google.gson.annotations.SerializedName


data class ReportRental(
    val id_transaction_rental: Int = 0,
    val id_branch_transaction_rental: Int = 0,
    val id_client_transaction_rental: Int = 0,
    val id_kurir_transaction_rental: Int = 0,
    val recipient_name_transaction_rental: String? = null,
    val type_rental_transaction : TypeTransactionRental,
    val status_transaction_rental:StatusTransactionRental,
    val total_pcs_transaction_rental: Int? = null,
    val price_weight_transaction_rental: Int? = null,
    val total_weight_transaction_rental: Double? = null,
    val additional_cost_transaction_rental: Double? = null,
    val promo_transaction_rental: Double? = null,
    val total_price_transaction_rental: Double? = null,
    val notes_transaction_rental: String? = null,
    val time_transaction_rental: String? = null,
    val is_active_transaction_rental: IsActiveTransactionRental,
    val list_transaction_rentals : List<ListTransactionRental>,
    val deleted_at: Any? = null,
    val created_at : String? = null,
) {
    override fun hashCode(): Int {
        return id_transaction_rental?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportRental) return false

        return id_transaction_rental == other.id_transaction_rental
    }
}

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


data class ExportReportRental(
    val month: String,
    val location: Int,
    val description: String,
    val initial_stock: Int,
    val notes: List<String>
)

data class ReportRentalResponse(
    val download_url: String,
    val filename: String,
    val path: String
)


enum class StatusInvoiceRental(val value: String) {
    UNPAID("unpaid"),
    PAID("paid"),
    CANCELLED("cancelled");

    fun displayName(): String {
        return when (this) {
            UNPAID -> "Belum Dibayar"
            PAID -> "Sudah Dibayar"
            CANCELLED -> "Dibatalkan"
        }
    }

    companion object {
        fun fromValue(value: String): StatusInvoiceRental? {
            return entries.find { it.value == value }
        }

        fun fromDisplayName(name: String): StatusInvoiceRental? {
            return entries.find { it.displayName() == name }
        }
    }
}
