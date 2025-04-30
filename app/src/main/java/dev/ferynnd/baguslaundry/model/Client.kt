package dev.ferynnd.baguslaundry.model

data class Client(
    val id_client: Int? =null ,
    val id_branch_client: Int? =null ,
    val name_client: String? =null ,
    val address_client: String? =null ,
    val phone_client: String? =null ,
    val is_active_client: IsActiveClient ,
    val delete_at: String? =null
)


enum class IsActiveClient{
    active,
    inactive
}
