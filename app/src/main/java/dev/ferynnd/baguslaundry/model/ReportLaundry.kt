package dev.ferynnd.baguslaundry.model

data class ReportLaundry(
    val id_transaction_laundry: Int? = null,
    val id_branch_transaction_laundry: Int? = null,
    val id_user_transaction_laundry: Int? = null,
    val name_client_transaction_laundry: String? = null,
    val status_transaction_laundry: StatusReportLaundry,
    val notes_transaction_laundry: String? = null,
    val total_weight_transaction_laundry: Double? = null,
    val total_price_transaction_laundry: Double? = null,
    val count_item_laundry_transaction_laundry: Int? = null,
    val promo_transaction_laundry: Double? = null,
    val additional_cost_transaction_laundry: Double? = null,
    val total_transaction_laundry: Double? = null,
    val cash_transaction_laundry: Double? = null,
    val change_money_transaction_laundry: Double? = null,
    val is_active_transaction_laundry: IsActiveReportLaundry,
    val first_date_transaction_laundry: String? = null,
    val last_date_transaction_laundry: String? = null,
    val deleted_at: String?,
)

enum class StatusReportLaundry {
    pending,
    in_progress,
    completed,
    cancelled
}
enum class IsActiveReportLaundry {
    active,
    inactive
}

