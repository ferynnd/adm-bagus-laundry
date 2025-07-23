package dev.ferynnd.baguslaundry.data.repository.product

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ProductLaundry

class LaundryProductRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val laundryProductApiService = retrofitHelper.laundryProductApiService

    private val sharedPreferences = SharePrefrenceHelper(context)


    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"


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

    suspend fun createProductLaundry(productLaundry: ProductLaundry): DefaultRequest<ProductLaundry> {
        try {
            val response = laundryProductApiService.createProductLaundry(role, productLaundry)
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
