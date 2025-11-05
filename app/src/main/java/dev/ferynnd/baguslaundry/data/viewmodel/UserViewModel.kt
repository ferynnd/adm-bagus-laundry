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
    import dev.ferynnd.baguslaundry.model.AlertData
    import dev.ferynnd.baguslaundry.model.User
    import dev.ferynnd.baguslaundry.model.UserRole
    import kotlinx.coroutines.launch
    import dev.ferynnd.baguslaundry.model.Event
    import dev.ferynnd.baguslaundry.R

    class UserViewModel(application: Application) : AndroidViewModel(application) {

        private lateinit var  userRepository : UserRepository

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

        private val _alertEvent = MutableLiveData<Event<AlertData>>()
        val alertEvent: LiveData<Event<AlertData>> get() = _alertEvent

        private fun sendAlert(alertData: AlertData) {
            _alertEvent.postValue(Event(alertData))
        }

        private var currentQuery: String = ""
        private var selectedBranchId: Int? = null

        fun init(context: Context) {
            userRepository = UserRepository(context)
            if (userRepository.isLoggedIn()){
                fetchAllUsers()
            }
        }

        fun login(username: String, password: String) {
                _loading.postValue(true) // Start loading
                viewModelScope.launch {
                    try {
                        val result = userRepository.login(username, password)
                        _loginResult.postValue(result)
                        if (result.isSuccess) {
                            sendAlert(AlertData(
                                title = "Login Berhasil",
                                message = "Selamat datang, $username!",
                                iconRes = R.drawable.success
                            ))
                        } else {
                            sendAlert(AlertData(
                                title = "Login Gagal",
                                message ="Login gagal untuk $username",
                                backgroundColorRes = android.R.color.holo_red_dark,
                                iconRes = R.drawable.failed,
                                duration = 5000
                            ))
                        }

                    } catch (e: Exception) {
                        sendAlert(AlertData(
                            title = "Login Error",
                            message = "Terjadi kesalahan",
                            backgroundColorRes = android.R.color.holo_red_dark,
                            iconRes = R.drawable.failed,
                            duration = 5000
                        ))
                    } finally {
                        _loading.postValue(false) // End loading
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

        private fun fetchAllUsers() {
            viewModelScope.launch {
                _loading.postValue(true)
                _error.postValue("")
                try {
                    val response = userRepository.getUser()
                    if (response.success) {
                        _users.postValue(response.data)
                        applyCurrentFilters(response.data)
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
                result = result.filter { it.id_user == id }
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
            _filteredUsers.value?.let { applyCurrentFilters(it) }
        }

        fun onBranchFilterSelected(branchId: Int?) {
            selectedBranchId = branchId
            _filteredUsers.value?.let { applyCurrentFilters(it) }
        }

        fun resetErrorMessage() {
            _error.postValue("")
        }

         suspend fun changePassword(token: String, currentPassword: String, newPassword: String, confirmNewPassword : String) {
            _loading.postValue(true) // Start loading
            if(currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty())
            {
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
                        sendAlert(AlertData(
                            title = "Ubah Password Error",
                            message = "Terjadi kesalahan",
                            backgroundColorRes = R.color.red600,
                            iconRes = R.drawable.failed,
                            duration = 5000
                        ))
                    } finally {
                        _loading.postValue(false) // End loading
                    }
                }
        }
    }