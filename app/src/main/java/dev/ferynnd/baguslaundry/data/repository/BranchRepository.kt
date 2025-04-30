package dev.ferynnd.baguslaundry.data.repository

import android.content.Context
import dev.ferynnd.baguslaundry.data.api.ApiResponse
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.RetrofitHelper
import dev.ferynnd.baguslaundry.model.Branch

//private  val branchApiService: BranchApiService = RetrofitHelper().branchApiService

class BranchRepository(context: Context) {

    private val retrofitHelper = RetrofitHelper(context)
    private val branchApiService = retrofitHelper.branchApiService

    suspend fun getBranch(): ApiResponse<Branch> {
        try {
            val response = branchApiService.getBranch()
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createBranch(branch: Branch): DefaultRequest<Branch> {
        try {
            val response = branchApiService.createBranch(branch)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

            }
    }

    suspend fun getBranchById(id: Int): DefaultRequest<Branch> {
        try {
            val response = branchApiService.getBranchById(id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e

        }
    }

    suspend fun deleteBranch(id: Int): DefaultRequest<Branch> {
        try {
            val response = branchApiService.deleteBranch(id)
            if (response.success) {
                return response
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateBranch(id: Int, branch: Branch): DefaultRequest<Branch> {
        try {
            val response = branchApiService.updateBranch(id,branch)
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