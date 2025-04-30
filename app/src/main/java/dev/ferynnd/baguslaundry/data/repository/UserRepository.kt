package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.api.LoginResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.User
import org.json.JSONObject

//private val authApiService: AuthApiService = RetrofitHelper(context).authApiService
class UserRepository(context: Context) {


    private val sharedPreferences = SharePrefrenceHelper(context)

    private val retrofitHelper = RetrofitHelper(context)
    private val authApiService = retrofitHelper.authApiService
    private val userApiService = retrofitHelper.userApiService

     private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

    fun isLoggedIn(): Boolean {
        val token = sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApiService.login(username, password)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    // Respons berhasil tapi login gagal (contoh: username/password salah)
                    Result.failure(Exception(body.message ?: "Login gagal"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Terjadi kesalahan"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


//
//    suspend fun login(username: String, password: String): Result<LoginResponse> {
//            return try {
//                val response = authApiService.login(username, password)
//
//                if (response.isSuccessful && response.body() != null) {
//                    Result.success(response.body()!!)
//                } else {
//                    val errorMsg = response.errorBody()?.string()
//                    Result.failure(Exception(errorMsg))
//                }
//            } catch (e: Exception) {
//                Result.failure(e)
//            }
//    }

      suspend fun getUser(): ApiResponse<User> {
        val response = userApiService.getUser(role)
        if (response.success) {
            return response
        } else {
            throw Exception("API request failed")
        }
    }

    suspend fun getUserById(id: Int): DefaultRequest<User> {
        val response = userApiService.getUserById(role, id)
        if (response.success) {
            return response
        } else {
            throw Exception("API request failed")
        }
    }
}


