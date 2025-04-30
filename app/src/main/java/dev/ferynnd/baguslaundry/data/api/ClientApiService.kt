package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.Client
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE

import retrofit2.http.Path

interface ClientApiService {

    @GET("api/clients")
    suspend fun getClient(): ApiResponse<Client>

    @POST("api/create_clients")
    suspend fun createClient( @Body client: Client): DefaultRequest<Client>

    @GET("api/clients/{id}")
    suspend fun getClientById(@Path("id") id: Int): DefaultRequest<Client>

    @DELETE("api/delete_clients/{id}")
    suspend fun deleteClient(@Path("id") id: Int): DefaultRequest<Client>

    @PUT("api/edit_clients/{id}")
    suspend fun updateClient(@Path("id") id: Int, @Body client: Client): DefaultRequest<Client>


}