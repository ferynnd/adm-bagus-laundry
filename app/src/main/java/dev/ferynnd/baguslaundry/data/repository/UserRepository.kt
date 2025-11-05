package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.api.LoginResponse
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.ChangePasswordRequest
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UserRepository(context: Context) {

    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)

    private val authApiService = retrofitHelper.authApiService
    private val userApiService = retrofitHelper.userApiService

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

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
                if (body.success) {
                    Result.success(body) // login benar
                } else {
                    Result.failure(Exception(body.message ?: "Username atau password salah"))
                }
            } else {
                val errorMsg = response.errorBody()?.string()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


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


