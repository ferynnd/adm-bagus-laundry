package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.model.ListTransactionRental

class ListTransactionReportRentalRepository  (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val listTransactionRentalReportApiService =
        retrofitHelper.listTransactionRentalReportApiService

    suspend fun getListTransactionReportRental(): ApiResponse<ListTransactionRental> {
        try {
            val response = listTransactionRentalReportApiService.getListTransactionReportRental()
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