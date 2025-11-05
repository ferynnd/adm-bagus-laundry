package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.Branch

class BranchRepository(context: Context) {

    private val sharedPreferences = SharePrefrenceHelper(context)
    private val retrofitHelper = RetrofitHelper(context)
    private val branchApiService = retrofitHelper.branchApiService

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"

    // Hanya mengambil data mentah dari API
    suspend fun getBranch(): ApiResponse<Branch> {
        return branchApiService.getBranch(role)
    }

    suspend fun getBranchById(id: Int): DefaultRequest<Branch> {
        return branchApiService.getBranchById(role, id)
    }
}
