package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import android.util.Log
import dev.ferynnd.admbaguslaundry.data.api.ApiResponse
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequestPrint
import dev.ferynnd.admbaguslaundry.data.api.DefaultResponse
import dev.ferynnd.admbaguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.model.*
import retrofit2.HttpException

class LaundryReportRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val laundryReportApiService = retrofitHelper.laundryReportApiService

    // Hanya mengambil data mentah dari API
    suspend fun getReportLaundry(page : Int = 1): ApiResponse<ReportLaundry> {
        return laundryReportApiService.getReportRental(page)
    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        return laundryReportApiService.getReportLaundryById(id)
    }

    suspend fun createReportLaundry(laundryTransactionRequest: TransactionData): DefaultRequest<TransactionData> {
        return laundryReportApiService.createReportLaundry(laundryTransactionRequest)
    }

  private val TAG = "UPDATE_LAUNDRY_REPO"

suspend fun updateReportLaundry(
    id: Int,
    request: UpdateLaundryFullRequest
): DefaultResponse {

    try {

        Log.d(TAG, "==============================")
        Log.d(TAG, "START UPDATE REPORT LAUNDRY")
        Log.d(TAG, "TRANSACTION ID : $id")
        Log.d(TAG, "REQUEST : $request")

        Log.d(TAG, "TRANSACTION DATA :")
        Log.d(TAG, "KURIR : ${request.transaction.id_kurir_transaction_laundry}")
        Log.d(TAG, "BRANCH : ${request.transaction.id_branch_transaction_laundry}")
        Log.d(TAG, "CUSTOMER : ${request.transaction.name_client_transaction_laundry}")
        Log.d(TAG, "STATUS : ${request.transaction.status_transaction_laundry}")
        Log.d(TAG, "NOTES : ${request.transaction.notes_transaction_laundry}")

        request.list_items.forEachIndexed { index, item ->
            Log.d(
                TAG,
                "ITEM[$index] -> ID_ITEM=${item.id_item_laundry}, WEIGHT=${item.weight_list_transaction_laundry}"
            )
        }

        val response = laundryReportApiService.updateReportLaundry(id, request)

        Log.d(TAG, "SUCCESS UPDATE")
        Log.d(TAG, "RESPONSE : $response")
        Log.d(TAG, "==============================")

        return response

    } catch (e: Exception) {

        Log.e(TAG, "FAILED UPDATE REPORT")
        Log.e(TAG, "ERROR MESSAGE : ${e.message}")
        Log.e(TAG, "STACKTRACE : ", e)

        throw e
    }
}

    suspend fun deleteReportLaundry(id : Int) : DefaultRequest<ReportLaundry> {
        try {
            val response = laundryReportApiService.deleteReportLaundry(id)
            return response
        } catch (e: Exception) {

            throw e
        }
    }

     suspend fun forceDeleteReportLaundry(id: Int): DefaultRequest<ReportLaundry> {
        return laundryReportApiService.forceDeleteReportLaundry( id)
    }

    suspend fun exportLaundryMonthly(exportReport: ExportReportLaundry): DefaultRequest<ReportLaundryResponse> {
        return laundryReportApiService.exportLaundryMonthly(exportReport)
    }

    suspend fun getLaundryPrint(id: Int?): DefaultRequestPrint<LaundryPrintTransaction> {
        return laundryReportApiService.getLaundryPrint(id)
    }
}