package dev.ferynnd.baguslaundry.data.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.ClientRepository
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.launch


class ClientViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var clientRepository: ClientRepository

    private val _clients = MutableLiveData<List<Client>>()
    val clients: LiveData<List<Client>> get() = _clients

    // Tambahkan LiveData untuk loading dan error
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String>() // Ubah menjadi non-nullable String
    val error: LiveData<String> get() = _error

    private val _filteredClients = MutableLiveData<List<Client>>()  // hasil pencarian
    val filteredClients: LiveData<List<Client>> get() = _filteredClients


    fun init(context: Context) {
        clientRepository = ClientRepository(context)
        getAllClient()
    }

    // Fungsi publik untuk mereset pesan error
    fun resetErrorMessage() {
        _error.postValue("") // Gunakan postValue untuk memastikan pembaruan terjadi di main thread
    }

    private fun getAllClient() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = clientRepository.getClient()
                if (response.success) {
                    _clients.postValue(response.data)
                    _filteredClients.postValue(response.data) // Inisialisasi filtered list dengan semua data
                } else {
                    _error.postValue("Gagal memuat data klien awal: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat memuat data klien awal.")
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }


    suspend fun getClient() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        try {
            val response = clientRepository.getClient()
            if (response.success) {
                val client = response.data
                _clients.postValue(client) // Memperbarui LiveData dengan data baru
                _filteredClients.postValue(client) // Perbarui juga filtered list
            } else {
                _error.postValue("Permintaan API gagal saat mengambil klien: ${response.message ?: "Pesan tidak tersedia"}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil data klien.")
        } finally {
            _loading.postValue(false) // Always set loading to false
        }
    }


    suspend fun getClientById(id: Int): DefaultRequest<Client> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            clientRepository.getClientById(id)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil klien berdasarkan ID.")
            // Pastikan mengembalikan DefaultRequest yang valid, bukan null
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequest<Client>
    }


    fun filterClient(branchId: Int?) {
        val allClients = _clients.value ?: return
        _filteredClients.value = if (branchId == null) {
            allClients
        } else {
            allClients.filter { it.id_branch_client == branchId }
        }
    }

     fun searchClients(query: String) {
        val allClients = _clients.value ?: return
        if (query.isBlank()) {
            _filteredClients.value = allClients
        } else {
            _filteredClients.value = allClients.filter {
                it.name_client?.contains(query, ignoreCase = true) == true
            }
        }
    }
}