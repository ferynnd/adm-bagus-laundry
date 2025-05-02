package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ReportLaundry

class LaundryReportRepository (context: Context) {
    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE").toString()

    fun isLoggedIn(): Boolean {
        val token = sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    private val laundryReportApiService = retrofitHelper.laundryReportApiService

    suspend fun getReportLaundry(): ApiResponse<ReportLaundry> {
        try {
            val response = laundryReportApiService.getReportRental(role)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

//    suspend fun createReportLaundry(productLaundry : ReportLaundry): DefaultRequest<ReportLaundry> {
//        try {
//            val response = laundryReportApiService.createReportLaundry(productLaundry )
//            if (response.success) {
//                return response
//            } else {
//                throw Exception("API request failed")
//            }
//        } catch (e: Exception) {
//            throw e
//
//        }
//    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        try {
            val response = laundryReportApiService.getReportLaundryById(role, id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun deleteReportLaundry(id: Int): DefaultRequest<ReportLaundry> {
        try {
            val response = laundryReportApiService.deleteReportLaundry(role, id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateReportLaundry(id: Int, productLaundry : ReportLaundry): DefaultRequest<ReportLaundry> {
        try {
            val response = laundryReportApiService.updateReportLaundry(role, id, productLaundry )
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
