package dev.ferynnd.admbaguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.ChangePasswordRequest
import dev.ferynnd.admbaguslaundry.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApiService {

    @GET("api/admin/all_users")
    suspend fun getUser(@Query("page") page: Int): ApiResponse<User>

    @GET("api/admin/users/{id}")
    suspend fun getUserById(
        @Path("id") id: Int
    ): DefaultRequest<User>

    @POST("api/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): DefaultRequest<Any>


}