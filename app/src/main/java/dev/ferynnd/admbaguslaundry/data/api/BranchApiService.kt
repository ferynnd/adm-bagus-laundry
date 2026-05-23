package dev.ferynnd.admbaguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.Branch
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BranchApiService {

    @GET("api/admin/all_branches")
    suspend fun getBranch(@Query("page") page: Int): ApiResponse<Branch>

    @GET("api/admin/branches/{id}")
    suspend fun getBranchById(@Path("id") id: Int): DefaultRequest<Branch>

}