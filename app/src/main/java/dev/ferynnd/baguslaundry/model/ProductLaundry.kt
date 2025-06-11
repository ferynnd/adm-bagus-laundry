package dev.ferynnd.baguslaundry.model

data class ProductLaundry(
    val id_laundry_item: Int? = null,
    val id_branch_laundry_item: Int? = null,
    val name_laundry_item: String?= null,
    val price_laundry_item: Int? = null,
    val time_laundry_item: String? = null,
    val is_active_laundry_item: IsActiveLaundryItem? = IsActiveLaundryItem.active,
    val deleted_at: Any? = null,
    var isSelected: Boolean? = false,
    var weight: Float? = null
)

enum class IsActiveLaundryItem {
    active,
    inactive
}