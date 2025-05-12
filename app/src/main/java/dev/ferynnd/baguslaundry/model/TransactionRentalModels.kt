package dev.ferynnd.baguslaundry.model

data class RentalTransactionRequest(
    val id_kurir_transaction_rental: Int,
    val id_branch_transaction_rental: Int,
    val id_client_transaction_rental: Int,
    val recipient_name_transaction_rental: String,
    val type_rental_transaction: String,
    val status_transaction_rental: String,
    val price_weight_transaction_rental: Int = 0,
    val promo_transaction_rental: Double = 0.0,
    val additional_cost_transaction_rental: Double = 0.0,
    val notes_transaction_laundry: String,
    val list_transaction_rentals: List<RentalTransactionItem>
)

data class RentalTransactionItem(
    val id_item_rental: Int?,
    val type_list_rental_transaction: String = "",
    val condition_list_transaction_rental: String = "",
    val note_list_transaction_rental: String = "",
    val weight_list_transaction_rental: Double = 0.0
)

// Updated to handle errors as a Map of String to Any
data class RentalTransactionResponse(
    val success: Boolean,
    val message: String = "",
    val data: RentalTransactionData?,
    val errors: Map<String, Any>? = null
)

data class RentalTransactionData(
    val id_transaction_rental: Int,
    val id_kurir_transaction_rental: Int,
    val id_branch_transaction_rental: Int,
    val id_client_transaction_rental: Int,
    val recipient_name_transaction_rental: String,
    val type_rental_transaction: String,
    val status_transaction_rental: String,
    val price_weight_transaction_rental: Double = 0.0,
    val total_weight_transaction_rental: Double = 0.0,
    val sub_total_weight_transaction_rental: Double = 0.0,
    val total_pcs_transaction_rental: Int = 0,
    val promo_transaction_rental: Double = 0.0,
    val additional_cost_transaction_rental: Double = 0.0,
    val total_price_transaction_rental: Double = 0.0,
    val time_transaction_rental: String,
    val is_active_transaction_rental: String,
    val updated_at: String,
    val created_at: String,
    val list_transaction_laundry: List<RentalTransactionItemDetail>
)

data class RentalTransactionItemDetail(
    val id_list_transaction_rental: Int,
    val id_rental_transaction: Int,
    val id_item_rental: Int,
    val type_list_rental_transaction: String,
    val status_list_transaction_rental: String,
    val condition_list_transaction_rental: String,
    val note_list_transaction_rental: String,
    val price_list_transaction_rental: Double = 0.0,
    val weight_list_transaction_rental: Double = 0.0,
    val is_active_list_transaction_rental: String,
    val deleted_at: String?,
    val created_at: String,
    val updated_at: String
)