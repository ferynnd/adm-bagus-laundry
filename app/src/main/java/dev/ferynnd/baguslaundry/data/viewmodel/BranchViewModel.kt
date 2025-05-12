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

    private lateinit var branchRepository: BranchRepository

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

    suspend fun getBranchById(id: Int): DefaultRequest<Branch> {
        return branchRepository.getBranchById(id)
    }


    private val _filteredBranches = MutableLiveData<List<Branch>>()  // hasil pencarian
    val filteredBranches: LiveData<List<Branch>> get() = _filteredBranches


    fun filterClient(branchId: Int?) {
        val allBranches = _branches.value ?: return
        _filteredBranches.value = if (branchId == null) {
            allBranches
        } else {
            allBranches.filter { it.id_branch == branchId }
        }
    }

     fun searchBranches(query: String) {
        val allBranches = _branches.value ?: return
        if (query.isBlank()) {
            _filteredBranches.value = allBranches
        } else {
            _filteredBranches.value = allBranches.filter {
                it.name_branch?.contains(query, ignoreCase = true) == true ||
                it.city_branch?.contains(query, ignoreCase = true) == true
            }
        }
    }


}