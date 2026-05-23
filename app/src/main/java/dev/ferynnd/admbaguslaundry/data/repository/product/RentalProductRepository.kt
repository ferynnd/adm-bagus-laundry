package dev.ferynnd.admbaguslaundry.data.repository.product

import android.content.Context
import dev.ferynnd.admbaguslaundry.data.api.ApiResponse
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.model.ProductRental

class RentalProductRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val rentalProductApiService = retrofitHelper.rentalProductApiService

    suspend fun getProductRental(page : Int = 1): ApiResponse<ProductRental> {
        try {
            val response = rentalProductApiService.getProductRental(page)
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
            val response = rentalProductApiService.getProductRentalById(id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun createProductRental(productRental: ProductRental): DefaultRequest<ProductRental> {
        try {
            val response = rentalProductApiService.createProductRental(productRental)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun updateProductRental(id: Int, productRental : ProductRental): DefaultRequest<ProductRental> {
        try {
            val response = rentalProductApiService.updateProductRental(id, productRental )
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteProductRental(id: Int): DefaultRequest<ProductRental> {
        try {
            val response = rentalProductApiService.deleteProductRental(id)
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
