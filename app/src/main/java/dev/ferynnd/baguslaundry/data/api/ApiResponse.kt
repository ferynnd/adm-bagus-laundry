package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.User


data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: User?,
    val token: String?,
    val errors: Any?
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: PaginatedData<T>,
    val errors: String? = null
)

data class DefaultRequest<T>(
    val success: Boolean,
    val message: String,
    val data: T,
    val errors: String? = null
)

data class PaginatedData<T>(
    val items: List<T>,
    val pagination: Pagination
)

data class Pagination(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int,
    val from: Int?,
    val to: Int?,
    val next_page_url: String?,
    val prev_page_url: String?
)

data class DefaultRequestPrint<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val errors: String? = null
)

data class DefaultResponse(
    val success: Boolean,
    val message: String,
    val errors: Any? = null
)

data class DefaultRequestInvoice<T>(
    val success: Boolean,
    val message: String,
    val data: T,
    val errors: Any? = null
)
