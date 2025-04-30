package dev.ferynnd.baguslaundry.data.repository.report
import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry

class ListTransactionReportLaundryRepository  (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val listTransactionLaundryReportApiService =
        retrofitHelper.listTransactionLaundryReportApiService

    suspend fun getListTransactionReportLaundry(): ApiResponse<ListTransactionLaundry> {
        try {
            val response = listTransactionLaundryReportApiService.getListTrListTransactionLaundry()
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