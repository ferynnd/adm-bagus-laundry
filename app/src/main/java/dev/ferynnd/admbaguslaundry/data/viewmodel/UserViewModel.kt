package dev.ferynnd.admbaguslaundry.data.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.api.LoginResponse
import dev.ferynnd.admbaguslaundry.data.api.Pagination
import dev.ferynnd.admbaguslaundry.data.repository.UserRepository
import dev.ferynnd.admbaguslaundry.model.AlertData
import dev.ferynnd.admbaguslaundry.model.Event
import dev.ferynnd.admbaguslaundry.model.User
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var userRepository: UserRepository

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _filteredUsers = MutableLiveData<List<User>>()
    val filteredUsers: LiveData<List<User>> get() = _filteredUsers

    private val _pagination = MutableLiveData<Pagination>()
    val pagination: LiveData<Pagination> get() = _pagination

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> get() = _loginResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _alertEvent = MutableLiveData<Event<AlertData>>()
    val alertEvent: LiveData<Event<AlertData>> get() = _alertEvent

    private var currentQuery: String = ""
    private var selectedBranchId: Int? = null

    fun init(context: Context) {
        userRepository = UserRepository(context)
        if (userRepository.isLoggedIn()) {
            getUser(1)
        }
    }

    fun getUser(page: Int = 1) {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue("")

            try {
                val response = userRepository.getUser(page)

                if (response.success) {
                    val items = response.data.items ?: emptyList()

                     Log.d("USER_VM", "Items size: ${items.size}")
                items.forEachIndexed { index, user ->
                    Log.d("USER_VM", "User[$index]: $user")
                }

                Log.d("USER_VM", "Pagination: ${response.data?.pagination}")

                    _users.postValue(items)
                    _pagination.postValue(response.data.pagination)
                    applyCurrentFilters(items)
                } else {
                    _error.postValue(response.message ?: "Gagal memuat user.")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Kesalahan jaringan.")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun applyCurrentFilters(all: List<User>) {
        var result = all

        selectedBranchId?.let { id ->
            result = result.filter { it.id_branch_user == id }
        }

        if (currentQuery.isNotEmpty()) {
            result = result.filter {
                it.fullname_user?.contains(currentQuery, ignoreCase = true) == true ||
                        it.username?.contains(currentQuery, ignoreCase = true) == true
            }
        }

        _filteredUsers.postValue(result)
    }

    fun onSearchQueryChanged(query: String) {
        currentQuery = query
        _users.value?.let { applyCurrentFilters(it) }
    }

    fun onBranchFilterSelected(branchId: Int?) {
        selectedBranchId = branchId
        _users.value?.let { applyCurrentFilters(it) }
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }

    private fun sendAlert(alertData: AlertData) {
        _alertEvent.postValue(Event(alertData))
    }

    fun login(username: String, password: String) {
        val TAG = "LoginViewModel"
        _loading.postValue(true)

        viewModelScope.launch {
            try {
                val result = userRepository.login(username, password)
                _loginResult.postValue(result)

                if (result.isSuccess) {
                    Log.i(TAG, "Login berhasil untuk user: $username")
                    sendAlert(
                        AlertData(
                            title = "Login Berhasil",
                            message = "Selamat datang, $username!",
                            iconRes = R.drawable.success
                        )
                    )
                } else {
                    sendAlert(
                        AlertData(
                            title = "Login Gagal",
                            message = "Login gagal untuk $username",
                            backgroundColorRes = android.R.color.holo_red_dark,
                            iconRes = R.drawable.failed,
                            duration = 5000
                        )
                    )
                }
            } catch (e: Exception) {
                sendAlert(
                    AlertData(
                        title = "Login Error",
                        message = "Terjadi kesalahan",
                        backgroundColorRes = android.R.color.holo_red_dark,
                        iconRes = R.drawable.failed,
                        duration = 5000
                    )
                )
            } finally {
                _loading.postValue(false)
            }
        }
    }

    suspend fun getUserById(id: Int): DefaultRequest<User> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            userRepository.getUserById(id)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil pengguna berdasarkan ID.")
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<User>
    }

    suspend fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ) {
        _loading.postValue(true)

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            return sendAlert(
                AlertData(
                    title = "Peringatan!",
                    message = "Lengkapi semua inputan!",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.info
                )
            )
        }

        if (newPassword.length < 8) {
            return sendAlert(
                AlertData(
                    title = "Peringatan!",
                    message = "Password baru harus lebih dari 8 karakter",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.info
                )
            )
        }

        if (newPassword != confirmNewPassword) {
            sendAlert(
                AlertData(
                    title = "Password Tidak Sama",
                    message = "Password baru dan konfirmasi password baru tidak sama",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed
                )
            )
        } else {
            try {
                val response = userRepository.changePassword(token, currentPassword, newPassword)

                if (response.success) {
                    sendAlert(
                        AlertData(
                            title = "Password Berhasil Diubah",
                            message = "Password Kamu Berhasil Diubah",
                            backgroundColorRes = R.color.primary,
                            iconRes = R.drawable.success
                        )
                    )
                } else {
                    sendAlert(
                        AlertData(
                            title = "Password Gagal Diubah",
                            message = "Password Kamu Gagal Diubah",
                            backgroundColorRes = R.color.red600,
                            iconRes = R.drawable.failed
                        )
                    )
                }
            } catch (e: Exception) {
                sendAlert(
                    AlertData(
                        title = "Ubah Password Error",
                        message = "Terjadi kesalahan",
                        backgroundColorRes = R.color.red600,
                        iconRes = R.drawable.failed,
                        duration = 5000
                    )
                )
            } finally {
                _loading.postValue(false)
            }
        }
    }
}