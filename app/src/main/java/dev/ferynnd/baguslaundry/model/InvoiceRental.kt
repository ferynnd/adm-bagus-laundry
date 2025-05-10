package dev.ferynnd.baguslaundry.model

data class InvoiceRentalResponse(
    val id_invoice_rental: Int,
    val id_branch_invoice: Int,
    val id_client_invoice: Int,
    val number_invoice: String,
    val notes_invoice_rental: String,
    val time_invoice_rental: String,
    val total_weight_invoice_rental: Double,
    val price_invoice_rental: Double,
    val promo_invoice_rental: Double?,
    val additional_cost_invoice_rental: Double?,
    val total_price_invoice_rental: Double?,
    val is_active_invoice_rental: String,
    val deleted_at: String?,
)

data class ListInvoiceRentalResponse(
    val id_list_invoice_rental: Int,
    val id_rental_invoice: Int,
    val id_rental_transaction: Int,
    val type_invoice_rental: String,
    val status_list_invoice_rental: String,
    val note_list_invoice_rental: String,
    val price_list_invoice_rental: Double,
    val weight_list_invoice_rental: Double,
    val total_price_invoice_rental: Double,
    val is_active_list_invoice_rental: String,
    val deleted_at: String?,
)

data class PostInvoiceRentalRequest(
    val id_branch_invoice: Int,
    val id_client_invoice: Int,
    val notes_invoice_rental: String,
    val total_weight_invoice_rental: Double,
    val price_invoice_rental: Double,
    val promo_invoice_rental: Double?,
    val additional_cost_invoice_rental: Double?,
    val list_invoice_rentals: List<ListInvoiceRentalItem>
)

data class ListInvoiceRentalItem(
    val id_rental_transaction: Int,
    val status_list_invoice_rental: String,
    val note_list_invoice_rental: String
)

data class ExportInvoicePdfRentalRequest(
    val id_invoice_rental: Int,
    val note: String,
    val payment: String
)



