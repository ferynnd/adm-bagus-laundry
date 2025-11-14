package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.*
import retrofit2.http.*

interface RentalReportApiService {

    @GET("api/{role}/transaction_rentals")
    suspend fun getReportRental(@Path("role") role: String): ApiResponse<ReportRental>

    @GET("api/{role}/transaction_rentals/{id}")
    suspend fun getReportRentalById(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ReportRental>

    @POST("api/{role}/create_transaction_rentals")
    suspend fun createReportRental(
        @Path("role") role: String,
        @Body rentalTransactionRequest: RentalTransactionRequest
    ): DefaultRequest<RentalTransactionData>

    @PUT("api/{role}/edit_transaction_rentals/{id}")
    suspend fun updateReportRental(
        @Path("role") role: String,
        @Path("id") id: Int,
        @Body updateRequest: UpdateRentalTransactionRequest
    ): DefaultRequest<ReportRental>

    @DELETE("api/{role}/delete_transaction_rentals/{id}")
    suspend fun deleteReportRental(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ReportRental>

    @DELETE("api/{role}/force_destroy_transaction_rentals/{id}")
    suspend fun forceDeleteReportRental(
        @Path("role") role: String,
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

    @GET("api/{role}/invoice_rentals")
    suspend fun getInvoiceRental(@Path("role") role: String): ApiResponse<InvoiceRentalResponse>

    @GET("api/{role}/transaction_rentals/{id}/list")
    suspend fun getRentalPrint(
        @Path("role") role: String,
        @Path("id") id: Int?
    ): DefaultRequestPrint<RentalPrintTransaction>
}