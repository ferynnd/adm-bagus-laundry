package dev.ferynnd.baguslaundry.data.repository.report
import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ExportReportRental
import dev.ferynnd.baguslaundry.model.InvoiceRentalResponse
import dev.ferynnd.baguslaundry.model.PostInvoiceRentalRequest
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.ReportRentalResponse

class RentalReportRepository  (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val rentalReportApiService = retrofitHelper.rentalReportApiService

    private val sharedPreferences = SharePrefrenceHelper(context)

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"


    fun isLoggedIn(): Boolean {
        val token =
            sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    suspend fun getReportRental(): ApiResponse<ReportRental> {
        try {
            val response = rentalReportApiService.getReportRental(role)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }


    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        try {
            val response = rentalReportApiService.getReportRentalById(role,id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }


    suspend fun exportRentalMonthly(exportReport: ExportReportRental): DefaultRequest<ReportRentalResponse> {
         try {
            val response = rentalReportApiService.exportRentalMonthly(exportReport)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun createInvoiceRental(postInvoiceReportRental: PostInvoiceRentalRequest): DefaultRequest<InvoiceRentalResponse> {
        try {
            val response = rentalReportApiService.createInvoiceRental(postInvoiceReportRental)
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
