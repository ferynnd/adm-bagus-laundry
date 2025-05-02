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
    suspend fun getClient(@Path("role") role: String): ApiResponse<Client>

    @POST("api/{role}/create_clients")
    suspend fun createClient(@Path("role") role: String, @Body client: Client): DefaultRequest<Client>

    @GET("api/{role}/clients/{id}")
    suspend fun getClientById(@Path("role") role: String,@Path("id") id: Int): DefaultRequest<Client>

    @DELETE("api/{role}/delete_clients/{id}")
    suspend fun deleteClient(@Path("role") role: String,@Path("id") id: Int): DefaultRequest<Client>

    @PUT("api/{role}/edit_clients/{id}")
    suspend fun updateClient(@Path("role") role: String,@Path("id") id: Int, @Body client: Client): DefaultRequest<Client>


}