package dev.ferynnd.baguslaundry.data.api


import dev.ferynnd.baguslaundry.model.ReportRental
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RentalReportApiService {

    @GET("api/transaction_rentals")
    suspend fun getReportRental(): ApiResponse<ReportRental>

//    @POST("api/create_transaction_rentals")
//    suspend fun createReportRental( @Body reportRental: ReportRental): DefaultRequest<ReportRental>

    @GET("api/transaction_rentals/{id}")
    suspend fun getReportRentalById(@Path("id") id: Int): DefaultRequest<ReportRental>

    @DELETE("api/delete_transaction_rentals/{id}")
    suspend fun deleteReportRental(@Path("id") id: Int): DefaultRequest<ReportRental>

    @PUT("api/edit_transaction_rentals/{id}")
    suspend fun updateReportRental(@Path("id") id: Int, @Body reportRental: ReportRental): DefaultRequest<ReportRental>

}