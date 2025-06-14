package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.User


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
    val data: List<T>,
    val errors: String? = null
)

data class DefaultRequest<T>(
    val success: Boolean,
    val message: String,
    val data: T,
    val errors: String? = null
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
