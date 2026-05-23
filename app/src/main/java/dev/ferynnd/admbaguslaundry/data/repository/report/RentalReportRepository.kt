package dev.ferynnd.admbaguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.admbaguslaundry.data.api.*
import dev.ferynnd.admbaguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.model.*

    import android.util.Log
    import retrofit2.HttpException

class RentalReportRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val rentalReportApiService = retrofitHelper.rentalReportApiService

    suspend fun getReportRental(page : Int = 1): ApiResponse<ReportRental> {
        return rentalReportApiService.getReportRental(page)
    }

    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        return rentalReportApiService.getReportRentalById( id)
    }

    suspend fun createReportRental(rentalTransactionRequest: RentalTransactionRequest): DefaultRequest<RentalTransactionData> {
        return rentalReportApiService.createReportRental( rentalTransactionRequest)
    }


    suspend fun updateReportRental(
        id: Int,
        updateRequest: UpdateRentalFullRequest
    ): DefaultRequest<ReportRental> {
        return try {
            Log.d("UPDATE_RENTAL", "ID: $id")
            Log.d("UPDATE_RENTAL", "BODY: $updateRequest")

            val response = rentalReportApiService.updateReportRental(id, updateRequest)

            Log.d("UPDATE_RENTAL", "SUCCESS: ${response.success}")
            Log.d("UPDATE_RENTAL", "MESSAGE: ${response.message}")
            Log.d("UPDATE_RENTAL", "ERRORS: ${response.errors}")

            response
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()

            Log.e("UPDATE_RENTAL", "HTTP CODE: ${e.code()}")
            Log.e("UPDATE_RENTAL", "ERROR BODY: $errorBody", e)

            throw Exception("HTTP ${e.code()}: $errorBody")
        } catch (e: Exception) {
            Log.e("UPDATE_RENTAL", "ERROR: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteReportRental(id: Int): DefaultRequest<ReportRental> {
        return rentalReportApiService.deleteReportRental( id)
    }

    suspend fun forceDeleteReportRental(id: Int): DefaultRequest<ReportRental> {
        return rentalReportApiService.forceDeleteReportRental( id)
    }

    suspend fun exportRentalMonthly(exportReport: ExportReportRental): DefaultRequest<ReportRentalResponse> {
        return rentalReportApiService.exportRentalMonthly(exportReport)
    }

    suspend fun createInvoiceRental(
        request: PostInvoiceRentalRequest
    ): DefaultRequestInvoice<InvoiceRentalResponse> {
        return rentalReportApiService.createInvoiceRental(request)
    }

    suspend fun exportInvoiceRental(
        request: ExportInvoicePdfRentalRequest
    ): DefaultRequestInvoice<ReportRentalResponse> {
        return rentalReportApiService.exportInvoiceRental(request)
    }

    suspend fun getInvoiceRental(
        page: Int = 1
    ): ApiResponse<InvoiceRentalResponse> {
        return rentalReportApiService.getInvoiceRental(page)
    }

    suspend fun getRentalPrint(id: Int?): DefaultRequestPrint<RentalPrintTransaction> {
        return rentalReportApiService.getRentalPrint( id)
    }


}