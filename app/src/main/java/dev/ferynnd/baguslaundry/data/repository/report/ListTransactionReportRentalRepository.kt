package dev.ferynnd.baguslaundry.data.repository.report

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ListTransactionRental

class ListTransactionReportRentalRepository  (context: Context) {
    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE").toString()

    fun isLoggedIn(): Boolean {
        val token = sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    private val listTransactionRentalReportApiService =
        retrofitHelper.listTransactionRentalReportApiService

    suspend fun getListTransactionReportRental(): ApiResponse<ListTransactionRental> {
        try {
            val response = listTransactionRentalReportApiService.getListTransactionReportRental(role)
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