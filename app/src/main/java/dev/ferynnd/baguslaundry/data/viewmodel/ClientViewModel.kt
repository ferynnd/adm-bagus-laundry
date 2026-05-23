package dev.ferynnd.baguslaundry.data.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.admbaguslaundry.data.api.Pagination
import dev.ferynnd.admbaguslaundry.data.repository.ClientRepository
import dev.ferynnd.admbaguslaundry.model.Client
import kotlinx.coroutines.launch

class ClientViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var clientRepository: ClientRepository

    private val _clients = MutableLiveData<List<Client>>()
    val clients: LiveData<List<Client>> get() = _clients

    private val _filteredClients = MutableLiveData<List<Client>>()
    val filteredClients: LiveData<List<Client>> get() = _filteredClients

    private val _pagination = MutableLiveData<Pagination>()
    val pagination: LiveData<Pagination> get() = _pagination

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private var currentQuery: String = ""
    private var selectedBranchId: Int? = null

    fun init(context: Context) {
        clientRepository = ClientRepository(context)
        getClient(1)
    }

    fun getClient(page: Int = 1) {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue("")

            try {
                val response = clientRepository.getClient(page)

                if (response.success) {
                    val items = response.data?.items ?: emptyList()

                    _clients.postValue(items)
                    _pagination.postValue(response.data?.pagination)
                    applyCurrentFilters(items)
                } else {
                    _error.postValue(response.message ?: "Gagal memuat klien.")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Kesalahan jaringan.")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun applyCurrentFilters(all: List<Client>) {
        var result = all

        selectedBranchId?.let { id ->
            result = result.filter { it.id_branch_client == id }
        }

        if (currentQuery.isNotEmpty()) {
            result = result.filter {
                it.name_client?.contains(currentQuery, ignoreCase = true) == true ||
                        it.city_client?.contains(currentQuery, ignoreCase = true) == true
            }
        }

        _filteredClients.postValue(result)
    }

    fun onSearchQueryChanged(query: String) {
        currentQuery = query
        _clients.value?.let { applyCurrentFilters(it) }
    }

    fun onBranchFilterSelected(branchId: Int?) {
        selectedBranchId = branchId
        _clients.value?.let { applyCurrentFilters(it) }
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }
}