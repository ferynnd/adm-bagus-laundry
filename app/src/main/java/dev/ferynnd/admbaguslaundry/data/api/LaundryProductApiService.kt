package dev.ferynnd.admbaguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.ProductLaundry
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface LaundryProductApiService {

    @GET("api/admin/all_laundry_items")
    suspend fun getProductLaundry(@Query("page") page: Int): ApiResponse<ProductLaundry>

    @GET("api/admin/laundry_items/{id}")
    suspend fun getProductLaundryById(
        @Path("id") id: Int
    ): DefaultRequest<ProductLaundry>

    @POST("api/admin/create_laundry_items")
    suspend fun createProductLaundry(
        @Body productLaundry: ProductLaundry
    ): DefaultRequest<ProductLaundry>

    @DELETE("api/admin/delete_laundry_items/{id}")
    suspend fun deleteProductLaundry(
        @Path("id") id: Int
    ): DefaultRequest<ProductLaundry>

    @PUT("api/admin/edit_laundry_items/{id}")
    suspend fun updateProductLaundry(
        @Path("id") id: Int,
        @Body productLaundry: ProductLaundry
    ): DefaultRequest<ProductLaundry>
}