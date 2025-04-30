package dev.ferynnd.baguslaundry.data.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.BranchRepository
import dev.ferynnd.baguslaundry.model.Branch
import kotlinx.coroutines.launch


class BranchViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var branchRepository : BranchRepository

    private val _branches = MutableLiveData<List<Branch>>()
    val branches: LiveData<List<Branch>> get() = _branches

    fun init(context: Context) {
       branchRepository = BranchRepository(context)
        getAllBranch()
    }

     private fun getAllBranch() {
        viewModelScope.launch {
            _branches.postValue(branchRepository.getBranch().data)
        }
    }


    suspend fun getBranch() {
        try {
            val response = branchRepository.getBranch()
            if (response.success) {
                val branch = response.data
                _branches.postValue(branch) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }

    suspend fun createBranch(branch: Branch) {
        branchRepository.createBranch(branch)
    }

    suspend fun getBranchById(id: Int): DefaultRequest<Branch> {
        return branchRepository.getBranchById(id)
    }

    suspend fun deleteBranch(branch: Branch) {
        branch.id_branch?.let { branchRepository.deleteBranch(it) }
    }

    suspend fun updateBranch(branch: Branch) {
        branch.id_branch?.let { branchRepository.updateBranch(it,branch) }
    }

}