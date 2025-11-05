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

    private val _filteredBranches = MutableLiveData<List<Branch>>()
    val filteredBranches: LiveData<List<Branch>> get() = _filteredBranches

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private var currentQuery: String = ""
    private var selectedBranchId: Int? = null


    fun init(context: Context) {
        branchRepository = BranchRepository(context)
        fetchAllBranches()
    }


    private fun fetchAllBranches() {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue("")
            try {
                val response = branchRepository.getBranch()
                if (response.success) {
                    _branches.postValue(response.data)
                    applyCurrentFilters(response.data)
                } else {
                    _error.postValue(response.message ?: "Gagal memuat cabang.")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Kesalahan jaringan.")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun applyCurrentFilters(all: List<Branch>) {
        var result = all
        selectedBranchId?.let { id ->
            result = result.filter { it.id_branch == id }
        }
        if (currentQuery.isNotEmpty()) {
            result = result.filter {
                it.name_branch?.contains(currentQuery, ignoreCase = true) == true ||
                it.city_branch?.contains(currentQuery, ignoreCase = true) == true
            }
        }
        _filteredBranches.postValue(result)
    }

    fun onSearchQueryChanged(query: String) {
        currentQuery = query
        _branches.value?.let { applyCurrentFilters(it) }
    }

    fun onBranchFilterSelected(branchId: Int?) {
        selectedBranchId = branchId
        _branches.value?.let { applyCurrentFilters(it) }
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }
}