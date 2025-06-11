package dev.ferynnd.baguslaundry.data.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.api.LoginResponse
import dev.ferynnd.baguslaundry.data.repository.UserRepository
import dev.ferynnd.baguslaundry.model.User
import dev.ferynnd.baguslaundry.model.UserRole
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application.applicationContext)

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> get() = _loginResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>() // Ubah menjadi non-nullable String
    val error: LiveData<String> get() = _error

    private val _filteredUsers = MutableLiveData<List<User>>()  // hasil pencarian
    val filteredUsers: LiveData<List<User>> get() = _filteredUsers


    init {
        if (userRepository.isLoggedIn()){
            getAllUsers()
        }
    }

    // Fungsi publik untuk mereset pesan error
    fun resetErrorMessage() {
        _error.postValue("") // Gunakan postValue untuk memastikan pembaruan terjadi di main thread
    }

    fun login(username: String, password: String) {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val result = userRepository.login(username, password)
                _loginResult.postValue(result)
                if (result.isSuccess) {
                    // Jika login berhasil, refresh daftar user
                    getAllUsers()
                } else {
                    _error.postValue("Login gagal: ${result.exceptionOrNull()?.message ?: "Terjadi kesalahan"}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat login.")
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    private fun getAllUsers() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = userRepository.getUser()
                if (response.success && response.data != null) {
                    val filteredUsersData = response.data.filter { it.role_user == UserRole.kurir }
                    _users.postValue(filteredUsersData)
                    _filteredUsers.postValue(filteredUsersData) // Inisialisasi filtered list dengan semua data
                } else {
                    _error.postValue("Gagal memuat daftar pengguna awal: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat memuat data pengguna awal.")
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    // Mengganti suspend fun getUser() menjadi non-suspend jika dipanggil dari ViewModel,
    // dan pastikan menggunakan postValue.
    // Jika fungsi ini dipanggil dari observer di Fragment, pertimbangkan untuk hanya memicu getAllUsers().
    // Untuk konsistensi, saya akan memodifikasinya agar sesuai dengan pola LiveData.
    fun fetchUsers() { // Ubah nama fungsi agar tidak ambigu dengan suspend fun getUserById
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = userRepository.getUser()
                if (response.success && response.data != null) {
                    val filteredUsersData = response.data.filter { it.role_user == UserRole.kurir }
                    _users.postValue(filteredUsersData)
                    _filteredUsers.postValue(filteredUsersData) // Perbarui juga filtered list
                } else {
                    _error.postValue("Permintaan API gagal saat mengambil pengguna: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil data pengguna.")
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    suspend fun getUserById(id: Int): DefaultRequest<User> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            userRepository.getUserById(id)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil pengguna berdasarkan ID.")
            // Pastikan mengembalikan DefaultRequest yang valid, bukan null
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequest<User>
    }

    fun filterClient(branchId: Int?) {
        val allUsers = _users.value ?: return
        _filteredUsers.value = if (branchId == null) {
            allUsers
        } else {
            allUsers.filter { it.id_branch_user == branchId }
        }
    }

    fun searchUsers(query: String) {
        val allUsers = _users.value ?: return
        if (query.isBlank()) {
            _filteredUsers.value = allUsers
        } else {
            _filteredUsers.value = allUsers.filter {
                it.username?.contains(query, ignoreCase = true) == true ||
                it.fullname_user?.contains(query, ignoreCase = true) == true
            }
        }
    }
}