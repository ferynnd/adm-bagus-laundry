package dev.ferynnd.baguslaundry.data.repository.report
import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry

class ListTransactionReportLaundryRepository  (context: Context) {
    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE").toString()

    fun isLoggedIn(): Boolean {
        val token = sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    private val listTransactionLaundryReportApiService =
        retrofitHelper.listTransactionLaundryReportApiService

    private val sharedPreferences = SharePrefrenceHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"


    suspend fun getListTransactionReportLaundry(): ApiResponse<ListTransactionLaundry> {
        try {
            val response = listTransactionLaundryReportApiService.getListTrListTransactionLaundry(role)
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