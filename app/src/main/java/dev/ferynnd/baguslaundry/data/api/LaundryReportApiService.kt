package dev.ferynnd.baguslaundry.data.api

import dev.ferynnd.baguslaundry.model.ReportLaundry
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface LaundryReportApiService {

    @GET("api/{role}/transaction_laundries")
    suspend fun getReportRental(
        @Path("role") role: String,
    ): ApiResponse<ReportLaundry>

//    @POST("api/{role}/create_transaction_laundries")
//    suspend fun createReportLaundry(@Path("role") role: String, @Body reportLaundry: ReportLaundry): DefaultRequest<ReportLaundry>

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
    ): DefaultRequest<ReportLaundry>

}