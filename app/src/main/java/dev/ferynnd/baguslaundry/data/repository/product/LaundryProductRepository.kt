package dev.ferynnd.baguslaundry.data.repository.product

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ProductLaundry

class LaundryProductRepository (context: Context) {
    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE").toString()

    fun isLoggedIn(): Boolean {
        val token = sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    private val laundryProductApiService = retrofitHelper.laundryProductApiService

    suspend fun getProductLaundry(): ApiResponse<ProductLaundry> {
        try {
            val response = laundryProductApiService.getProductLaundry(role)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createProductLaundry(productLaundry : ProductLaundry): DefaultRequest<ProductLaundry> {
        try {
            val response = laundryProductApiService.createProductLaundry(role, productLaundry )
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun getProductLaundryById(id: Int): DefaultRequest<ProductLaundry> {
        try {
            val response = laundryProductApiService.getProductLaundryById(role, id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun deleteProductLaundry(id: Int): DefaultRequest<ProductLaundry> {
        try {
            val response = laundryProductApiService.deleteProductLaundry(role, id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateProductLaundry(id: Int, productLaundry : ProductLaundry): DefaultRequest<ProductLaundry> {
        try {
            val response = laundryProductApiService.updateProductLaundry(role, id, productLaundry )
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
