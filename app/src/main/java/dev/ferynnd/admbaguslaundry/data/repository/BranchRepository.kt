package dev.ferynnd.admbaguslaundry.data.repository

import android.content.Context
import dev.ferynnd.admbaguslaundry.data.api.ApiResponse
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.model.Branch

class BranchRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val branchApiService = retrofitHelper.branchApiService

    // Hanya mengambil data mentah dari API
    suspend fun getBranch(page : Int = 1): ApiResponse<Branch> {
        return branchApiService.getBranch(page)
    }

    suspend fun getBranchById(id: Int): DefaultRequest<Branch> {
        return branchApiService.getBranchById(id)
    }
}
