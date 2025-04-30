package dev.ferynnd.baguslaundry.data.api


import dev.ferynnd.baguslaundry.model.ListTransactionRental
import retrofit2.http.GET

interface ListTransactionRentalReportApiService {

    @GET("api/list_transaction_rentals")
    suspend fun getListTransactionReportRental(): ApiResponse<ListTransactionRental>

}