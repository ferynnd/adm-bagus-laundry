package dev.ferynnd.admbaguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.Client
import retrofit2.http.GET

import retrofit2.http.Path
import retrofit2.http.Query

interface ClientApiService {

    @GET("api/admin/all_clients")
    suspend fun getClient(@Query("page") page: Int): ApiResponse<Client>


    @GET("api/admin/clients/{id}")
    suspend fun getClientById(@Path("id") id: Int): DefaultRequest<Client>


}