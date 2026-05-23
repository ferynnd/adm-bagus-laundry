package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.ExportReportLaundry
import dev.ferynnd.admbaguslaundry.model.LaundryPrintTransaction
import dev.ferynnd.admbaguslaundry.model.ReportLaundry
import dev.ferynnd.admbaguslaundry.model.ReportLaundryResponse
import dev.ferynnd.admbaguslaundry.model.TransactionData
import dev.ferynnd.admbaguslaundry.model.UpdateLaundryFullRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LaundryReportApiService {

    @GET("api/admin/all_transaction_laundries_list")
    suspend fun getReportRental(
        @Query("page") page: Int
    ): ApiResponse<ReportLaundry>

    @POST("api/admin/create_transaction_laundries")
    suspend fun createReportLaundry(
        @Body laundryTransactionRequest: TransactionData
    ): DefaultRequest<TransactionData>

    @GET("api/admin/transaction_laundries/{id}/list")
    suspend fun getReportLaundryById(
        @Path("id") id: Int
    ): DefaultRequest<ReportLaundry>

    @DELETE("api/admin/delete_transaction_laundries/{id}")
    suspend fun deleteReportLaundry(
        @Path("id") id: Int
    ): DefaultRequest<ReportLaundry>

    @DELETE("api/admin/force_destroy_transaction_laundries/{id}")
    suspend fun forceDeleteReportLaundry(
        @Path("id") id: Int
    ): DefaultRequest<ReportLaundry>

    @PUT("api/admin/edit_transaction_laundries_full/{id}")
    suspend fun updateReportLaundry(
        @Path("id") id: Int,
        @Body request: UpdateLaundryFullRequest
    ): DefaultResponse // Asumsi mengembalikan DefaultResponse

    @POST("api/admin/export_laundry_monthly")
    suspend fun exportLaundryMonthly(
        @Body exportReportLaundry: ExportReportLaundry
    ): DefaultRequest<ReportLaundryResponse>

    @GET("api/admin/transaction_laundries/{id}/list")
    suspend fun getLaundryPrint(
        @Path("id") id: Int?
    ) : DefaultRequestPrint<LaundryPrintTransaction>
}