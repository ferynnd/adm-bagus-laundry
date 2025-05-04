package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.Branch
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BranchApiService {

    @GET("api/{role}/branches")
    suspend fun getBranch(@Path("role") role: String): ApiResponse<Branch>

    @GET("api/{role}/branches/{id}")
    suspend fun getBranchById(@Path("role") role: String,@Path("id") id: Int): DefaultRequest<Branch>

}