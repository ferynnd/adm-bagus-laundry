package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.api.DefaultRequestPrint
import dev.ferynnd.baguslaundry.data.api.DefaultResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.*

class LaundryReportRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val laundryReportApiService = retrofitHelper.laundryReportApiService
    private val sharedPreferences = SharePrefrenceHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

    // Hanya mengambil data mentah dari API
    suspend fun getReportLaundry(): ApiResponse<ReportLaundry> {
        return laundryReportApiService.getReportRental(role)
    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        return laundryReportApiService.getReportLaundryById(role, id)
    }

    suspend fun createReportLaundry(laundryTransactionRequest: TransactionData): DefaultRequest<TransactionData> {
        return laundryReportApiService.createReportLaundry(role, laundryTransactionRequest)
    }

    suspend fun updateReportLaundry(reportLaundry: ReportLaundry): DefaultResponse {
        val idTransaction = reportLaundry.id_transaction_laundry
            ?: throw IllegalArgumentException("Transaction ID cannot be null for update.")
        return laundryReportApiService.updateReportLaundry(role, idTransaction, reportLaundry)
    }

    suspend fun exportLaundryMonthly(exportReport: ExportReportLaundry): DefaultRequest<ReportLaundryResponse> {
        return laundryReportApiService.exportLaundryMonthly(exportReport)
    }

    suspend fun getLaundryPrint(id: Int?): DefaultRequestPrint<LaundryPrintTransaction> {
        return laundryReportApiService.getLaundryPrint(role, id)
    }
}