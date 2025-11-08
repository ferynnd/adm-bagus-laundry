package dev.ferynnd.baguslaundry.model


data class ReportRental(
    val id_transaction_rental: Int? = null,
    val id_kurir_transaction_rental: Int? = null,
    val id_branch_transaction_rental: Int? = null,
    val id_client_transaction_rental: Int? = null,
    val recipient_name_transaction_rental: String? = null,
    val number_transaction_rental: Int? = null,
    val total_pcs_transaction_rental: Int? = null,
    val total_weight_transaction_rental: Double? = null,
    val notes_transaction_rental: String? = null,
    val is_active_transaction_rental: IsActiveTransactionRental? = IsActiveTransactionRental.active,
    val time_transaction_rental : String? = null,
    val list_transaction_rentals : List<ListTransactionRental>,
    val deleted_at: Any? = null,
    @Transient
    var formatted_time_transaction_rental: String? = null,
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

enum class IsActiveTransactionRental {
    active,
    inactive,
}


data class ExportReportRental(
    val month: String,
    val location: Int,
    val id_item_rental : Int,
    val notes: List<String>,
    val initial_stock: Int,
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
