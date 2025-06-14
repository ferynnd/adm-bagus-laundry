package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ExportReportLaundry
import dev.ferynnd.baguslaundry.model.LaundryPrintTransaction
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundryResponse
import dev.ferynnd.baguslaundry.model.TransactionData
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LaundryReportApiService {

    @GET("api/{role}/transaction_laundries")
    suspend fun getReportRental(
        @Path("role") role: String,
    ): ApiResponse<ReportLaundry>

    @POST("api/{role}/create_transaction_laundries")
    suspend fun createReportLaundry(
        @Path("role") role: String,
        @Body laundryTransactionRequest: TransactionData
    ): DefaultRequest<TransactionData>

    @GET("api/{role}/transaction_laundries/{id}")
    suspend fun getReportLaundryById(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ReportLaundry>

    @DELETE("api/{role}/delete_transaction_laundries/{id}")
    suspend fun deleteReportLaundry(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ReportLaundry>

    @PUT("api/{role}/edit_transaction_laundries/{id}")
    suspend fun updateReportLaundry(
        @Path("role") role: String,
        @Path("id") id: Int,
        @Body reportLaundry: ReportLaundry
    ): DefaultResponse // Asumsi mengembalikan DefaultResponse

     @POST("api/admin/export_laundry_monthly")
    suspend fun exportLaundryMonthly(
        @Body exportReportLaundry: ExportReportLaundry
    ): DefaultRequest<ReportLaundryResponse>

    @GET("api/{role}/transaction_laundries/{id}/list")
    suspend fun getLaundryPrint(
        @Path("role") role: String,
        @Path("id") id: Int?
    ) : DefaultRequestPrint<LaundryPrintTransaction>
}