package dev.ferynnd.admbaguslaundry.data.helper

import android.content.SharedPreferences
import dev.ferynnd.admbaguslaundry.data.helper.Constant.Companion.PREF_USER_TOKEN
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val prefs: SharedPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = prefs.getString(PREF_USER_TOKEN, null)
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
