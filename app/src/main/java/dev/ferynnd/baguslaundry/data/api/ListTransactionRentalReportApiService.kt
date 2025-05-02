package dev.ferynnd.baguslaundry.data.api


import dev.ferynnd.baguslaundry.model.ListTransactionRental
import retrofit2.http.GET
import retrofit2.http.Path

interface ListTransactionRentalReportApiService {

    @GET("api/{role}/list_transaction_rentals")
    suspend fun getListTransactionReportRental(@Path("role") role: String): ApiResponse<ListTransactionRental>

}