package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClientRepository(context: Context) {
    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE").toString()

    fun isLoggedIn(): Boolean {
        val token = sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    private val clientApiService = retrofitHelper.clientApiService

    suspend fun getClient(): ApiResponse<Client> {
        try {
            val response = clientApiService.getClient(role)
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
            val response = clientApiService.createClient(role, client)
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
            val response = clientApiService.getClientById(role, id)
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
            val response = clientApiService.deleteClient(role, id)
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
            val response = clientApiService.updateClient(role, id, client)
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
