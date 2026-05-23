package dev.ferynnd.baguslaundry.model

data class InvoiceRentalResponse(
    val id_invoice_rental: Int? = null,
    val id_branch_invoice: Int? = null,
    val id_client_invoice: Int? = null,
    val number_invoice_rental: String? = null,
    val notes_invoice_rental: String? = null,
    val time_invoice_rental: String? = null,
    val price_invoice_rental: Double? = null,
    val promo_invoice_rental: Double? = null,
    val additional_cost_invoice_rental: Double? = null,
    val total_price_invoice_rental: Double? = null,
    val is_active_invoice_rental: String? = null,
    val list_invoice_rentals: List<ListInvoiceRentalResponse>? = null
)

data class ListInvoiceRentalResponse(
    val id_list_invoice_rental: Int? = null,
    val id_rental_invoice: Int? = null,
    val id_item_rental_invoice: Int? = null,
    val name_list_invoice_rental: String? = null,
    val weight_price_list_invoice_rental: Int? = null,
    val weight_list_invoice_rental: Double? = null,
    val total_price_invoice_rental: Double? = null,
    val is_active_list_invoice_rental: String? = null,
)

data class PostInvoiceRentalRequest(
    val id_branch_invoice: Int,
    val id_client_invoice: Int,
    val month_invoice_rental: String,
    val notes_invoice_rental: String? = null,
    val promo_invoice_rental: Double? = null,
    val additional_cost_invoice_rental: Double? = null,
    val list_invoice_rentals: List<ListInvoiceRentalItem>
)

data class ListInvoiceRentalItem(
    val id_item_rental_invoice: Int
)

data class ExportInvoicePdfRentalRequest(
    val id_invoice_rental: Int? = null,
    val note: String? = null
)
