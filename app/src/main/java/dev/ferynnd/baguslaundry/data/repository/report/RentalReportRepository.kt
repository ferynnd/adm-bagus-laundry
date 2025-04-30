package dev.ferynnd.baguslaundry.data.repository.report
import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.model.ReportRental

class RentalReportRepository  (context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val rentalReportApiService = retrofitHelper.rentalReportApiService

    suspend fun getReportRental(): ApiResponse<ReportRental> {
        try {
            val response = rentalReportApiService.getReportRental()
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

//    suspend fun createReportRental(productRental : ReportRental): DefaultRequest<ReportRental> {
//        try {
//            val response = rentalReportApiService.createReportRental(productRental )
//            if (response.success) {
//                return response
//            } else {
//                throw Exception("API request failed")
//            }
//        } catch (e: Exception) {
//            throw e
//
//        }
//    }

    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        try {
            val response = rentalReportApiService.getReportRentalById(id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun deleteReportRental(id: Int): DefaultRequest<ReportRental> {
        try {
            val response = rentalReportApiService.deleteReportRental(id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateReportRental(id: Int, productRental : ReportRental): DefaultRequest<ReportRental> {
        try {
            val response = rentalReportApiService.updateReportRental(id, productRental )
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
