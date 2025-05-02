package dev.ferynnd.baguslaundry.data.api


import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path

interface ListTransactionLaundryReportApiService {

    @GET("api/{role}/list_transaction_laundries")
    suspend fun getListTrListTransactionLaundry(@Path("role") role: String): ApiResponse<ListTransactionLaundry>

}