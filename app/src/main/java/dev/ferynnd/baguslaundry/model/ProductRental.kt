package dev.ferynnd.baguslaundry.model

data class ProductRental(
    val id_rental_item: Int? = null,
    val id_branch_rental_item: Int? = null,
    val name_rental_item: String? = null,
    val price_rental_item: Int? = null,
    val is_active_rental_item: IsActiveRental? = IsActiveRental.active,
    val deleted_at: Any? = null,
)

enum class IsActiveRental {
    active,
    inactive
}

enum class ConditionRental {
    clean,
    dirty,
    damaged
}

enum class StatusRental {
    available,
    rented,
    maintenance
}
