package dev.ferynnd.baguslaundry.model

import java.math.BigDecimal

data class ReportLaundry(
    val id_transaction_laundry: Int? = null,
    val id_kurir_transaction_laundry: Int? = null,
//    val id_user_transaction_laundry: Int? = null,
    val id_branch_transaction_laundry: Int? = null,
    val name_client_transaction_laundry: String? = null,
    val number_transaction_laundry: Int? = null,
    val status_transaction_laundry: StatusReportLaundry,
    val notes_transaction_laundry: String? = null,
    val total_weight_transaction_laundry: BigDecimal? = null,
    val total_price_transaction_laundry: BigDecimal? = null,
    val count_item_transaction_laundry: Int? = null,
    val promo_transaction_laundry: BigDecimal? = null,
    val additional_cost_transaction_laundry: BigDecimal? = null,
    val cash_transaction_laundry: BigDecimal? = null,
    val change_money_transaction_laundry: BigDecimal? = null,
    val total_transaction_laundry: BigDecimal? = null,
    val is_active_transaction_laundry: IsActiveReportLaundry? = IsActiveReportLaundry.active,
    val first_date_transaction_laundry: String? = null,
    val last_date_transaction_laundry: String? = null,
    val deleted_at: String?,
    @Transient
    val formatted_first_date: String?,
    @Transient
    val formatted_last_date: String?,
)

enum class StatusReportLaundry(val label: String) {
    unpaid("Belum Dibayar"),
    paid("Sudah Dibayar"),
    completed("completed"),
    cancelled("Dibatalkan");

    override fun toString(): String {
        return label
    }
}


enum class IsActiveReportLaundry {
    active,
    inactive
}


data class ExportReportLaundry(
    val month: String,
    val id_branch : Int,
    val notes: List<String>
)

data class ReportLaundryResponse(
    val download_url: String,
    val filename: String,
    val path: String
)



