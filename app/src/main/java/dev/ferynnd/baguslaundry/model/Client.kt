package dev.ferynnd.baguslaundry.model

data class Client(
    val id_client: Int? = null,
    val id_branch_client: Int? = null,
    val name_client: String? = null,
    val phone_client: String? = null,
    val full_address_client: String? = null,
    val prov_client: String? = null,
    val kode_pos_client: Int? = null,
    val city_client: String? = null,
    val subdistrict_client: String? = null,
    val village_client: String? = null,
    val house_number_client: String? = null,
    val street_client: String? = null,
    val is_active_client: IsActiveClient? = IsActiveClient.active,
    val deleted_at: String? = null
)


enum class IsActiveClient{
    active,
    inactive
}
