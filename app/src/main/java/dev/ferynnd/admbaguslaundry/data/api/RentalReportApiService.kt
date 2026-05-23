package dev.ferynnd.admbaguslaundry.data.api

import dev.ferynnd.admbaguslaundry.model.*
import retrofit2.http.*

interface RentalReportApiService {

    @GET("api/admin/all_transaction_rentals_list")
    suspend fun getReportRental(@Query("page") page: Int): ApiResponse<ReportRental>

    @GET("api/admin/transaction_rentals/{id}/list")
    suspend fun getReportRentalById(
        @Path("id") id: Int
    ): DefaultRequest<ReportRental>

    @POST("api/admin/create_transaction_rentals")
    suspend fun createReportRental(
        @Body rentalTransactionRequest: RentalTransactionRequest
    ): DefaultRequest<RentalTransactionData>

    @PUT("api/admin/edit_transaction_rentals_full/{id}")
    suspend fun updateReportRental(
        @Path("id") id: Int,
        @Body updateRequest: UpdateRentalFullRequest
    ): DefaultRequest<ReportRental>

    @DELETE("api/admin/delete_transaction_rentals/{id}")
    suspend fun deleteReportRental(
        @Path("id") id: Int
    ): DefaultRequest<ReportRental>

    @DELETE("api/admin/force_destroy_transaction_rentals/{id}")
    suspend fun forceDeleteReportRental(
        @Path("id") id: Int
    ): DefaultRequest<ReportRental>

    @POST("api/admin/export_transaction_monthly")
    suspend fun exportRentalMonthly(
        @Body exportReportRental: ExportReportRental
    ): DefaultRequest<ReportRentalResponse>

    @POST("api/admin/create_invoice_rentals")
    suspend fun createInvoiceRental(
        @Body postInvoiceReportRental: PostInvoiceRentalRequest
    ): DefaultRequestInvoice<InvoiceRentalResponse>

    @POST("api/admin/export_invoice_rental")
    suspend fun exportInvoiceRental(
        @Body exportInvoiceRental: ExportInvoicePdfRentalRequest
    ): DefaultRequestInvoice<ReportRentalResponse>

    @GET("api/admin/all_invoice_rentals_list")
    suspend fun getInvoiceRental(
        @Query("page") page: Int = 1
    ): ApiResponse<InvoiceRentalResponse>

    @GET("api/admin/transaction_rentals/{id}/list")
    suspend fun getRentalPrint(
        @Path("id") id: Int?
    ): DefaultRequestPrint<RentalPrintTransaction>
}