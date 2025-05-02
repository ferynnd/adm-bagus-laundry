package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ProductLaundry
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path

interface LaundryProductApiService {

    @GET("api/laundry_items")
    suspend fun getProductLaundry(): ApiResponse<ProductLaundry>

    @POST("api/create_laundry_items")
    suspend fun createProductLaundry( @Body productLaundry: ProductLaundry): DefaultRequest<ProductLaundry>

    @GET("api/laundry_items/{id}")
    suspend fun getProductLaundryById(@Path("id") id: Int): DefaultRequest<ProductLaundry>

    @DELETE("api/delete_laundry_items/{id}")
    suspend fun deleteProductLaundry(@Path("id") id: Int): DefaultRequest<ProductLaundry>

    @PUT("api/edit_laundry_items/{id}")
    suspend fun updateProductLaundry(@Path("id") id: Int, @Body productLaundry: ProductLaundry): DefaultRequest<ProductLaundry>


}