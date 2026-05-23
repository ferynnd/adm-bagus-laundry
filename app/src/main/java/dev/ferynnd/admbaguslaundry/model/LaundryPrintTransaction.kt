package dev.ferynnd.admbaguslaundry.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LaundryPrintTransaction (
    val id_transaction_laundry: Int,
    val id_kurir_transaction_laundry: Int,
    val id_branch_transaction_laundry: Int,
    val name_client_transaction_laundry: String,
    val number_transaction_laundry: Int,
    val status_transaction_laundry: String,
    val notes_transaction_laundry: String?, // Nullable
    val total_weight_transaction_laundry: Double,
    val total_price_transaction_laundry: Int,
    val count_item_transaction_laundry: Int,
    val promo_transaction_laundry: Int,
    val additional_cost_transaction_laundry: Int,
    val cash_transaction_laundry: Int,
    val change_money_transaction_laundry: Int,
    val total_transaction_laundry: Int,
    val is_active_transaction_laundry: String,
    val first_date_transaction_laundry: String,
    val last_date_transaction_laundry: String,
    val deleted_at: String?, // Nullable
    val created_at: String,
    val updated_at: String,
    val list_transaction_laundry: List<LaundryListItem>
) : Parcelable

@Parcelize
data class LaundryListItem(
    val id_list_transaction_laundry: Int,
    val id_transaction_laundry: Int,
    val id_item_laundry: Int,
    val price_list_transaction_laundry: Int,
    val weight_list_transaction_laundry: Double,
    val total_price_list_transaction_laundry: Int,
    val is_active_list_transaction_laundry: String,
    val deleted_at: String?, // Nullable
    val created_at: String,
    val updated_at: String
) : Parcelable