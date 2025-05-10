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

    init {
         if (userRepository.isLoggedIn()) {
            getAllUsers()
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = userRepository.login(username, password)
            _loginResult.value = result
        }
    }

    private fun getAllUsers() {
    viewModelScope.launch {
        val userList = userRepository.getUser().data
        val filteredUsers = userList.filter { it.role_user == UserRole.kurir } // Ganti dari owner ke kurir
        _users.postValue(filteredUsers)
    }
}



    suspend fun getUser() {
        try {
            val response = userRepository.getUser()
            if (response.success) {
                val filterUsers = response.data.filter { it.role_user == UserRole.kurir }
                _users.postValue(filterUsers)
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getUserById(id: Int): DefaultRequest<User> {
        return userRepository.getUserById(id)
    }


}

