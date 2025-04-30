package dev.ferynnd.baguslaundry.model

data class Branch(
    val id_branch: Int? = null,
    val name_branch: String? = null,
    val city_branch: String? = null,
    val address_branch: String? = null,
    val is_active_branch: Status,
    val deleted_at: String? = null
){
    override fun toString(): String {
        return name_branch!! // agar yang ditampilkan di Spinner adalah nama
    }
}

enum class Status {
    active, inactive
}
