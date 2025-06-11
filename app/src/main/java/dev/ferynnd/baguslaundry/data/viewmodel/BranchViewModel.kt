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

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String>() // Ubah menjadi non-nullable String
    val error: LiveData<String> get() = _error

    private val _filteredBranches = MutableLiveData<List<Branch>>()  // hasil pencarian
    val filteredBranches: LiveData<List<Branch>> get() = _filteredBranches

    fun init(context: Context) {
        branchRepository = BranchRepository(context)
        getAllBranch()
    }

    // Fungsi publik untuk mereset pesan error
    fun resetErrorMessage() {
        _error.postValue("") // Gunakan postValue untuk memastikan pembaruan terjadi di main thread
    }

    private fun getAllBranch() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = branchRepository.getBranch()
                if (response.success) {
                    _branches.postValue(response.data)
                    _filteredBranches.postValue(response.data) // Inisialisasi filtered list dengan semua data
                } else {
                    _error.postValue("Gagal memuat cabang awal: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat memuat data cabang awal.")
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }


    suspend fun getBranch() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        try {
            val response = branchRepository.getBranch()
            if (response.success) {
                val branch = response.data
                _branches.postValue(branch) // Memperbarui LiveData dengan data baru
                _filteredBranches.postValue(branch) // Perbarui juga filtered list
            } else {
                _error.postValue("Permintaan API gagal saat mengambil cabang: ${response.message ?: "Pesan tidak tersedia"}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil data cabang.")
        } finally {
            _loading.postValue(false) // Always set loading to false
        }
    }

    suspend fun getBranchById(id: Int): DefaultRequest<Branch> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            branchRepository.getBranchById(id)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil cabang berdasarkan ID.")
            // Pastikan mengembalikan DefaultRequest yang valid, bukan null
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequest<Branch>
    }

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