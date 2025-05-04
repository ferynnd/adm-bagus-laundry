package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.model.Client

class ClientRepository (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val clientApiService = retrofitHelper.clientApiService

    suspend fun getClient(): ApiResponse<Client> {
        try {
            val response = clientApiService.getClient()
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createClient(client: Client): DefaultRequest<Client> {
        try {
            val response = clientApiService.createClient(client)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun getClientById(id: Int): DefaultRequest<Client> {
        try {
            val response = clientApiService.getClientById(id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun deleteClient(id: Int): DefaultRequest<Client> {
        try {
            val response = clientApiService.deleteClient(id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateClient(id: Int, client: Client): DefaultRequest<Client> {
        try {
            val response = clientApiService.updateClient(id, client)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
