package dev.ferynnd.baguslaundry.model

data class RentalTransactionRequest(
    val id_transaction_rental: Int? = null,
    val id_kurir_transaction_rental: Int,
    val id_branch_transaction_rental: Int,
    val id_client_transaction_rental: Int,
    val recipient_name_transaction_rental: String,
    val number_transaction_rental: Int?,
    val notes_transaction_rental: String? = null,
    val list_transaction_rentals: List<RentalTransactionItem>
)

data class RentalTransactionItem(
    val id_item_rental: Int?,
    val status_list_transaction_rental: String, // "in", "out", "cancelled"
    val condition_list_transaction_rental: String, // "clean", "dirty", "damaged"
    val count_list_transaction_rental: Int,
    val weight_list_transaction_rental: Double? = null
)

data class RentalTransactionData(
    val id_transaction_rental: Int,
    val id_kurir_transaction_rental: Int,
    val id_branch_transaction_rental: Int,
    val id_client_transaction_rental: Int,
    val recipient_name_transaction_rental: String,
    val number_transaction_rental: Int,
    val total_weight_transaction_rental: Double,
    val total_pcs_transaction_rental: Int,
    val notes_transaction_rental: String? = null,
    val is_active_transaction_rental: String, // "active", "inactive"
    val time_transaction_rental: String? = null,
    val list_transaction_rentals: List<RentalTransactionItemDetail>
)

data class RentalTransactionItemDetail(
    val id_list_transaction_rental: Int,
    val id_rental_transaction: Int,
    val id_item_rental: Int,
    val name_list_rental_transaction: String,
    val status_list_transaction_rental: String, // "in", "out", "cancelled"
    val condition_list_transaction_rental: String, // "clean", "dirty", "damaged"
    val count_list_transaction_rental: Int,
    val weight_list_transaction_rental: Double,
    val is_active_list_transaction_rental: String, // "active", "inactive"
)


data class UpdateRentalFullRequest(
    val transaction: UpdateRentalTransactionData,
    val list_items: List<RentalTransactionItem>
)

data class UpdateRentalTransactionData(
    val id_kurir_transaction_rental: Int,
    val id_branch_transaction_rental: Int,
    val id_client_transaction_rental: Int,
    val recipient_name_transaction_rental: String,
    val number_transaction_rental: Int,
    val notes_transaction_rental: String?
)
