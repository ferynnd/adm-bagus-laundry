package dev.ferynnd.baguslaundry.model

data class User(
    val id_user: Int? = null,
    val id_branch_user: Int? = null,
    val username: String? = null,
    val password: String? = null,
    val fullname_user: String? = null,
    val role_user: UserRole,
    val gender_user: UserGender,
    val phone_user: String? = null,
    val address_user: String? = null,
    val is_active_user: IsActiveUser? = IsActiveUser.active,
    val deleted_at: String? = null,
)


enum class UserRole {
    admin,
    owner,
    kurir
}

enum class UserGender {
    male,
    female
}

enum class IsActiveUser{
    active,
    inactive
}
