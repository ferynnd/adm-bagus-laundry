package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import android.util.Log
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.LaundryTransactionResponse
import dev.ferynnd.baguslaundry.model.RentalTransactionRequest
import dev.ferynnd.baguslaundry.model.RentalTransactionResponse
import dev.ferynnd.baguslaundry.model.ReportRental

class RentalReportRepository  (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val rentalReportApiService = retrofitHelper.rentalReportApiService

    private val sharedPreferences = SharePrefrenceHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

    suspend fun getReportRental(): ApiResponse<ReportRental> {
        try {
            val response = rentalReportApiService.getReportRental(role)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }


    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        try {
            val response = rentalReportApiService.getReportRentalById(role,id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun createReportRental(rentalTransactionRequest: RentalTransactionRequest): DefaultRequest<RentalTransactionResponse> {
        try {
            val response = rentalReportApiService.createReportRental(role, rentalTransactionRequest)
            if (response.success) {
                return response
            } else {
                val errorBody = response.errors
                Log.e("API_ERROR", errorBody ?: "no body")
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
