package dev.ferynnd.admbaguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.ProductRental
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query


interface RentalProductApiService {

    @GET("api/admin/all_rental_items")
    suspend fun getProductRental(@Query("page") page: Int): ApiResponse<ProductRental>

    @GET("api/admin/rental_items/{id}")
    suspend fun getProductRentalById(
        @Path("id") id: Int
    ): DefaultRequest<ProductRental>

    @POST("api/admin/create_rental_items")
    suspend fun createProductRental(
        @Body productRental: ProductRental
    ): DefaultRequest<ProductRental>

    @DELETE("api/admin/delete_rental_items/{id}")
    suspend fun deleteProductRental(
        @Path("id") id: Int
    ): DefaultRequest<ProductRental>

    @PUT("api/admin/edit_rental_items/{id}")
    suspend fun updateProductRental(
        @Path("id") id: Int,
        @Body productRental: ProductRental
    ): DefaultRequest<ProductRental>
}