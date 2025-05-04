package dev.ferynnd.baguslaundry.data.repository.product

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ProductRental

class RentalProductRepository (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val rentalProductApiService = retrofitHelper.rentalProductApiService

    private val sharedPreferences = SharePrefrenceHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

    fun isLoggedIn(): Boolean {
        val token =
            sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    suspend fun getProductRental(): ApiResponse<ProductRental> {
        try {
            val response = rentalProductApiService.getProductRental(role, )
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }



    suspend fun getProductRentalById(id: Int): DefaultRequest<ProductRental> {
        try {
            val response = rentalProductApiService.getProductRentalById(role, id)
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
