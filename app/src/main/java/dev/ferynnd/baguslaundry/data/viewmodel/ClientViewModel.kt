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


class ClientViewModel (application: Application) : AndroidViewModel(application) {

    private var clientRepository = ClientRepository(application.applicationContext)

    private val _clients = MutableLiveData<List<Client>>()
    val clients: LiveData<List<Client>> get() = _clients

    init {
        if (clientRepository.isLoggedIn()) {   // <<< cek dulu
            getAllClient()
        }
    }

     private fun getAllClient() {
        viewModelScope.launch {
            _clients.postValue(clientRepository.getClient().data)
        }
    }


    suspend fun getClient() {
        try {
            val response = clientRepository.getClient()
            if (response.success) {
                val client = response.data
                _clients.postValue(client) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }


    suspend fun getClientById(id: Int): DefaultRequest<Client> {
        return clientRepository.getClientById(id)
    }



}