package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ChangePasswordRequest
import dev.ferynnd.baguslaundry.model.User
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApiService {

    @GET("api/{role}/users")
    suspend fun getUser(@Path("role") role: String): ApiResponse<User>

    @GET("api/{role}/users/{id}")
    suspend fun getUserById(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<User>

    @POST("api/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): DefaultRequest<Any>


}