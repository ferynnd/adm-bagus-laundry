package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import android.util.Log
import dev.ferynnd.admbaguslaundry.data.api.ApiResponse
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.api.LoginResponse
import dev.ferynnd.admbaguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.model.ChangePasswordRequest
import dev.ferynnd.admbaguslaundry.model.User

class UserRepository(context: Context) {

    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)

    private val authApiService = retrofitHelper.authApiService
    private val userApiService = retrofitHelper.userApiService

    fun isLoggedIn(): Boolean {
        val token =
            sharedPreferences.getString("PREF_USER_TOKEN", null)  // atau apapun key token kamu
        return !token.isNullOrEmpty()
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApiService.login(username, password)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.success) Result.success(body)
                else Result.failure(Exception(body.message ?: "Username atau password salah"))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login gagal: Response kosong"
                Log.e("LoginViewModel", "Login failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUser(page : Int = 1): ApiResponse<User> {
        val response = userApiService.getUser(page)


            Log.d("USER_REPOSITORY", "Success: ${response.success}")
            Log.d("USER_REPOSITORY", "Message: ${response.message}")
            Log.d("USER_REPOSITORY", "Items size: ${response.data?.items?.size ?: 0}")
            Log.d("USER_REPOSITORY", "Pagination: ${response.data?.pagination}")
            Log.d("USER_REPOSITORY", "Errors: ${response.errors}")
        if (response.success) {
            return response
        } else {
            throw Exception("API request failed")
        }
    }

    suspend fun getUserById(id: Int): DefaultRequest<User> {
        val response = userApiService.getUserById(id)
        if (response.success) {
            return response
        } else {
            throw Exception("API request failed")
        }
    }

    suspend fun changePassword(token: String, currentPassword: String, newPassword: String): DefaultRequest<Any> {
        try {
            val bearerToken = "Bearer $token"
            val request = ChangePasswordRequest(currentPassword, newPassword, newPassword)
            val response = userApiService.changePassword(bearerToken, request)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
            } catch (e: Exception) {
            throw e
        }
    }
}


