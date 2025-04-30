package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.Branch
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BranchApiService {

    @GET("api/branches")
    suspend fun getBranch(): ApiResponse<Branch>

    @POST("api/create_branches")
    suspend fun createBranch( @Body branch: Branch): DefaultRequest<Branch>

    @GET("api/branches/{id}")
    suspend fun getBranchById(@Path("id") id: Int): DefaultRequest<Branch>

    @DELETE("api/delete_branches/{id}")
    suspend fun deleteBranch(@Path("id") id: Int): DefaultRequest<Branch>

    @PUT("api/edit_branches/{id}")
    suspend fun updateBranch(@Path("id") id: Int, @Body branch: Branch): DefaultRequest<Branch>

}