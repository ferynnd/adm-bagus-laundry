package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.api.DefaultRequestPrint
import dev.ferynnd.baguslaundry.data.api.DefaultResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ExportReportLaundry
import dev.ferynnd.baguslaundry.model.LaundryPrintTransaction
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundryResponse
import dev.ferynnd.baguslaundry.model.TransactionData

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

    suspend fun createReportLaundry(laundryTransactionRequest: TransactionData): DefaultRequest<TransactionData> {
        try {
            val response =
                laundryReportApiService.createReportLaundry(role, laundryTransactionRequest)
            return response
        } catch (e: Exception) {
            throw e
        }
    }

    // Fungsi updateReportLaundry yang baru
    suspend fun updateReportLaundry(reportLaundry: ReportLaundry): DefaultResponse {
        return try {
            val idTransaction = reportLaundry.id_transaction_laundry
                ?: throw IllegalArgumentException("Transaction ID cannot be null for update.")

            laundryReportApiService.updateReportLaundry(role, idTransaction, reportLaundry)
        } catch (e: Exception) {
            DefaultResponse(success = false, message = e.message ?: "Kesalahan jaringan")
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

    suspend fun getLaundryPrint(id: Int?): DefaultRequestPrint<LaundryPrintTransaction> {
        return laundryReportApiService.getLaundryPrint(role, id)
    }


}
