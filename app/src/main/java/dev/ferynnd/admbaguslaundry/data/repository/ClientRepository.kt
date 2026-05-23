package dev.ferynnd.admbaguslaundry.data.repository

import android.content.Context
import dev.ferynnd.admbaguslaundry.data.api.ApiResponse
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.model.Client

class ClientRepository (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val clientApiService = retrofitHelper.clientApiService

    suspend fun getClient(page : Int = 1): ApiResponse<Client> {
        try {
            val response = clientApiService.getClient(page)
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

}
