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

    @POST("api/{role}/create_branches")
    suspend fun createBranch(@Path("role") role: String, @Body branch: Branch): DefaultRequest<Branch>

    @GET("api/{role}/branches/{id}")
    suspend fun getBranchById(@Path("role") role: String,@Path("id") id: Int): DefaultRequest<Branch>

    @DELETE("api/{role}/delete_branches/{id}")
    suspend fun deleteBranch(@Path("role") role: String,@Path("id") id: Int): DefaultRequest<Branch>

    @PUT("api/{role}/edit_branches/{id}")
    suspend fun updateBranch(@Path("role") role: String,@Path("id") id: Int, @Body branch: Branch): DefaultRequest<Branch>

}