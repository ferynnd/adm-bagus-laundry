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
    suspend fun getProductRental(@Path("role") role : String ): ApiResponse<ProductRental>

    @GET("api/{role}/rental_items/{id}")
    suspend fun getProductRentalById(@Path("role") role : String, @Path("id") id: Int): DefaultRequest<ProductRental>



}