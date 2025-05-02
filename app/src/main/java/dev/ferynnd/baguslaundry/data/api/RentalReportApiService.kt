package dev.ferynnd.baguslaundry.data.api


import dev.ferynnd.baguslaundry.model.ReportRental
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RentalReportApiService {

    @GET("api/{role}/transaction_rentals")
    suspend fun getReportRental(
        @Path("role") role: String,
    ): ApiResponse<ReportRental>

//    @POST("api/{role}/create_transaction_rentals")
//    suspend fun createReportRental( @Body reportRental: ReportRental): DefaultRequest<ReportRental>

    @GET("api/{role}/transaction_rentals/{id}")
    suspend fun getReportRentalById(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ReportRental>

    @DELETE("api/{role}/delete_transaction_rentals/{id}")
    suspend fun deleteReportRental(
        @Path("role") role: String,
        @Path("id") id: Int
    ): DefaultRequest<ReportRental>

    @PUT("api/{role}/edit_transaction_rentals/{id}")
    suspend fun updateReportRental(
        @Path("role") role: String,
        @Path("id") id: Int,
        @Body reportRental: ReportRental
    ): DefaultRequest<ReportRental>

}