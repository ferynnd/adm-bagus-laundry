package dev.ferynnd.baguslaundry.data.api


import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import retrofit2.http.Body
import retrofit2.http.GET

interface ListTransactionLaundryReportApiService {

    @GET("api/list_transaction_laundries")
    suspend fun getListTrListTransactionLaundry(): ApiResponse<ListTransactionLaundry>

}