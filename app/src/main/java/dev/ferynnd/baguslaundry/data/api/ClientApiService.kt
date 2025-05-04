package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.Client
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE

import retrofit2.http.Path

interface ClientApiService {

    @GET("api/{role}/clients")
    suspend fun getClient(@Path("role") role : String,): ApiResponse<Client>


    @GET("api/{role}/clients/{id}")
    suspend fun getClientById(@Path("role") role : String,@Path("id") id: Int): DefaultRequest<Client>


}