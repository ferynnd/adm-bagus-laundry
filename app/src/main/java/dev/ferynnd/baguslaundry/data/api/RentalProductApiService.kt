package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ProductRental
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path


interface RentalProductApiService {

    @GET("api/rental_items")
    suspend fun getProductRental(): ApiResponse<ProductRental>

    @POST("api/create_rental_items")
    suspend fun createProductRental( @Body productRental: ProductRental): DefaultRequest<ProductRental>

    @GET("api/rental_items/{id}")
    suspend fun getProductRentalById(@Path("id") id: Int): DefaultRequest<ProductRental>

    @DELETE("api/delete_rental_items/{id}")
    suspend fun deleteProductRental(@Path("id") id: Int): DefaultRequest<ProductRental>

    @PUT("api/edit_rental_items/{id}")
    suspend fun updateProductRental(@Path("id") id: Int, @Body productRental: ProductRental): DefaultRequest<ProductRental>



}