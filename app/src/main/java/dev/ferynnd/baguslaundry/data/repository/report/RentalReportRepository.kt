package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.*
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.*

class RentalReportRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val rentalReportApiService = retrofitHelper.rentalReportApiService
    private val sharedPreferences = SharePrefrenceHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

    // Hanya mengambil data mentah dari API
    suspend fun getReportRental(): ApiResponse<ReportRental> {
        return rentalReportApiService.getReportRental(role)
    }

    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        return rentalReportApiService.getReportRentalById(role, id)
    }

    suspend fun createReportRental(rentalTransactionRequest: RentalTransactionRequest): DefaultRequest<RentalTransactionData> {
        return rentalReportApiService.createReportRental(role, rentalTransactionRequest)
    }

    suspend fun exportRentalMonthly(exportReport: ExportReportRental): DefaultRequest<ReportRentalResponse> {
        return rentalReportApiService.exportRentalMonthly(exportReport)
    }

    suspend fun createInvoiceRental(postInvoiceReportRental: PostInvoiceRentalRequest): DefaultRequestInvoice<InvoiceRentalResponse> {
        return rentalReportApiService.createInvoiceRental(postInvoiceReportRental)
    }

    suspend fun exportInvoiceRental(exportInvoiceRental: ExportInvoicePdfRentalRequest): DefaultRequestInvoice<ReportRentalResponse> {
        return rentalReportApiService.exportInvoiceRental(exportInvoiceRental)
    }

    suspend fun getRentalPrint(id: Int?): DefaultRequestPrint<RentalPrintTransaction> {
        return rentalReportApiService.getRentalPrint(role, id)
    }

    suspend fun getInvoiceRental(): ApiResponse<InvoiceRentalResponse> {
        return rentalReportApiService.getInvoiceRental(role)
    }
}