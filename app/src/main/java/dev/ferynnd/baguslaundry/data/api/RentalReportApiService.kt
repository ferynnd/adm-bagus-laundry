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
    suspend fun getReportRental(@Path("role") role: String): ApiResponse<ReportRental>

    @GET("api/{role}/transaction_rentals/{id}")
    suspend fun getReportRentalById(@Path("role") role: String,@Path("id") id: Int): DefaultRequest<ReportRental>

}