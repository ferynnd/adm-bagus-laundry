package dev.ferynnd.baguslaundry.model


data class RentalPrintTransaction(
    val id_transaction_rental: Int? = null,
    val id_kurir_transaction_rental: Int? = null,
    val id_branch_transaction_rental: Int? = null,
    val id_client_transaction_rental: Int? = null,
    val recipient_name_transaction_rental: String? = null,
    val number_transaction_rental: Int? = null,
    val total_weight_transaction_rental: Double? = null,
    val total_pcs_transaction_rental: Int? = null,
    val notes_transaction_rental: String? = null,
    val is_active_transaction_rental: String? = null,
    val time_transaction_rental: String? = null,
    val deleted_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val list_transaction_rentals: List<ListTransactionRentalPrint>
)

data class ListTransactionRentalPrint(
    val id_list_transaction_rental: Int? = null,
    val id_rental_transaction: Int? = null,
    val id_item_rental: Int? = null,
    val name_list_rental_transaction: String? = null,
    val status_list_transaction_rental: String? = null,
    val condition_list_transaction_rental: String? = null,
    val count_list_transaction_rental: Int? = null,
    val weight_list_transaction_rental: Double? = null,
    val is_active_list_transaction_rental: String? = null,
    val deleted_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)