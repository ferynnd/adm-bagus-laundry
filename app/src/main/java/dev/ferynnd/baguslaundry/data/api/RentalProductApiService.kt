package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ProductRental
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path


interface RentalProductApiService {

    @GET("api/{role}/rental_items")
    suspend fun getProductRental(
        @Path("role") role: String
    ): ApiResponse<ProductRental>

    @POST("api/{role}/create_rental_items")
    suspend fun createProductRental(
        @Path("role") role: String,
        @Body productRental: ProductRental
    ): DefaultRequest<ProductRental>

    @GET("api/{role}/rental_items/{id}")
    suspend fun getProductRentalById(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ProductRental>

    @DELETE("api/{role}/delete_rental_items/{id}")
    suspend fun deleteProductRental(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ProductRental>

    @PUT("api/{role}/edit_rental_items/{id}")
    suspend fun updateProductRental(
        @Path("role") role: String,
        @Path("id") id: Int,
        @Body productRental: ProductRental
    ): DefaultRequest<ProductRental>


}