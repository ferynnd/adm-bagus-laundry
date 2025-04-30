package dev.ferynnd.baguslaundry.model

data class ProductRental(
    val id_rental_item: Int? = null,
    val id_branch_rental_item: Int? = null,
    val name_rental_item: String? = null,
    val number_rental_item: String? = null,
    val price_rental_item: Int? = null,
    val status_rental_item: StatusRental,
    val condition_rental_item: ConditionRental,
    val is_active_rental_item: IsActiveRental,
    val description_rental_item: String? = null,
    val deleted_at: Any? = null,
)

enum class IsActiveRental {
    active,
    inactive
}

enum class ConditionRental {
    clean,
    dirty,
    demaged
}

enum class StatusRental {
    available,
    rented,
    maintenance
}
