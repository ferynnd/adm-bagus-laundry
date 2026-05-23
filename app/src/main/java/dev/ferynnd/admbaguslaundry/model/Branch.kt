package dev.ferynnd.admbaguslaundry.model

data class Branch(
    val id_branch: Int? = null,
    val name_branch: String? = null,
    val no_telpon_branch: String? = null,
    val full_address_branch: String? = null,
    val prov_branch: String? = null,
    val kode_pos_branch: Int? = null,
    val city_branch: String? = null,
    val subdistrict_branch: String? = null,
    val village_branch: String? = null,
    val house_number_branch: String? = null,
    val street_branch: String? = null,
    val is_active_branch: Status? = Status.active,
    val timezone_branch: String? = null
){
    override fun toString(): String {
        return name_branch!! // agar yang ditampilkan di Spinner adalah nama
    }
}

enum class Status {
    active, inactive
}
