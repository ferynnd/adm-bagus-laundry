package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ReportLaundry
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface LaundryReportApiService {

    @GET("api/transaction_laundries")
    suspend fun getReportRental(): ApiResponse<ReportLaundry>

//    @POST("api/create_transaction_laundries")
//    suspend fun createReportLaundry( @Body reportLaundry: ReportLaundry): DefaultRequest<ReportLaundry>

    @GET("api/transaction_laundries/{id}")
    suspend fun getReportLaundryById(@Path("id") id: Int): DefaultRequest<ReportLaundry>

    @DELETE("api/delete_transaction_laundries/{id}")
    suspend fun deleteReportLaundry(@Path("id") id: Int): DefaultRequest<ReportLaundry>

    @PUT("api/edit_transaction_laundries/{id}")
    suspend fun updateReportLaundry(@Path("id") id: Int, @Body reportLaundry: ReportLaundry): DefaultRequest<ReportLaundry>

}