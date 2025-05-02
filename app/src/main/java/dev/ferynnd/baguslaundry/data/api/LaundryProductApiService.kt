package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ProductLaundry
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path

interface LaundryProductApiService {

    @GET("api/{role}/laundry_items")
    suspend fun getProductLaundry(@Path("role") role : String ): ApiResponse<ProductLaundry>

    @GET("api/{role}/laundry_items/{id}")
    suspend fun getProductLaundryById(@Path("role") role : String, @Path("id") id: Int): DefaultRequest<ProductLaundry>


}