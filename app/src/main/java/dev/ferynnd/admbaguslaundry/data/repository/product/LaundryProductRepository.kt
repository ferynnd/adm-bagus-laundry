package dev.ferynnd.admbaguslaundry.data.repository.product

import android.content.Context
import dev.ferynnd.admbaguslaundry.data.api.ApiResponse
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.model.ProductLaundry

class LaundryProductRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val laundryProductApiService = retrofitHelper.laundryProductApiService

    suspend fun getProductLaundry(page : Int = 1): ApiResponse<ProductLaundry> {
        try {
            val response = laundryProductApiService.getProductLaundry(page)
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
            val response = laundryProductApiService.getProductLaundryById(id)
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
            val response = laundryProductApiService.createProductLaundry(productLaundry)
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
            val response = laundryProductApiService.updateProductLaundry(id, productLaundry )
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
            val response = laundryProductApiService.deleteProductLaundry(id)
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
