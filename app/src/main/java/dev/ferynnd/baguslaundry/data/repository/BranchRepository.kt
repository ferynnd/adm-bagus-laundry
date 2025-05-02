package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.model.Branch

//private  val branchApiService: BranchApiService = RetrofitHelper().branchApiService

class BranchRepository(context: Context) {

    private val sharedPreferences = SharePrefrenceHelper(context)


    private val retrofitHelper = RetrofitHelper(context)
    private val branchApiService = retrofitHelper.branchApiService

    private val role: String
        get() = sharedPreferences.getString("PREF_USER_ROLE", "kurir") ?: "kurir"


    suspend fun getBranch(): ApiResponse<Branch> {
        try {
            val response = branchApiService.getBranch(role)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

//    suspend fun createBranch(branch: Branch): DefaultRequest<Branch> {
//        try {
//            val response = branchApiService.createBranch(role,branch)
//            if (response.success) {
//                return response
//            } else {
//                throw Exception("API request failed")
//            }
//        } catch (e: Exception) {
//            throw e
//
//            }
//    }

    suspend fun getBranchById(id: Int): DefaultRequest<Branch> {
        try {
            val response = branchApiService.getBranchById(role,id)
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