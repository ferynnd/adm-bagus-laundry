package dev.ferynnd.admbaguslaundry.data.helper

import android.content.Context
import dev.ferynnd.admbaguslaundry.data.api.AuthApiService
import dev.ferynnd.admbaguslaundry.data.api.BranchApiService
import dev.ferynnd.admbaguslaundry.data.api.ClientApiService
import dev.ferynnd.admbaguslaundry.data.api.LaundryProductApiService
import dev.ferynnd.admbaguslaundry.data.api.LaundryReportApiService
import dev.ferynnd.admbaguslaundry.data.api.RentalProductApiService
import dev.ferynnd.admbaguslaundry.data.api.RentalReportApiService
import dev.ferynnd.admbaguslaundry.data.api.UserApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitHelper(context: Context) {

    private val baseURL = "http://192.168.1.5:8000/"
//     private val baseURL = "https://www.dev.baguslaundry.my.id/"

    private val prefs = context.getSharedPreferences("AppSharePref", Context.MODE_PRIVATE)


    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(prefs))
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseURL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val branchApiService : BranchApiService by lazy {
        retrofit.create(BranchApiService::class.java)
    }

    val userApiService : UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }

    val clientApiService : ClientApiService by lazy {
        retrofit.create(ClientApiService::class.java)
    }

    val laundryProductApiService : LaundryProductApiService by lazy {
        retrofit.create(LaundryProductApiService::class.java)
    }

    val rentalProductApiService : RentalProductApiService by lazy {
        retrofit.create(RentalProductApiService::class.java)
    }

    val rentalReportApiService : RentalReportApiService by lazy {
        retrofit.create(RentalReportApiService::class.java)
    }

    val laundryReportApiService : LaundryReportApiService by lazy {
        retrofit.create(LaundryReportApiService::class.java)
    }


}