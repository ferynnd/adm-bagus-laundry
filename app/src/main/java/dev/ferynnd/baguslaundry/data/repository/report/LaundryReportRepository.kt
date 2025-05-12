package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ExportReportLaundry
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.LaundryTransactionResponse
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundryResponse

class LaundryReportRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val laundryReportApiService = retrofitHelper.laundryReportApiService

    private val sharedPreferences = SharePrefrenceHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

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

    suspend fun createReportLaundry(laundryTransactionRequest: LaundryTransactionRequest): DefaultRequest<LaundryTransactionResponse> {
        try {
            val response =
                laundryReportApiService.createReportLaundry(role, laundryTransactionRequest)
            return response
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun exportLaundryMonthly(exportReport: ExportReportLaundry): DefaultRequest<ReportLaundryResponse> {
        try {
            val response = laundryReportApiService.exportLaundryMonthly(exportReport)
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
