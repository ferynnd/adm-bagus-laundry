package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.repository.report.ListTransactionReportLaundryRepository
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import kotlinx.coroutines.launch

class ListTransactionReportLaundryViewModel  (application: Application) : AndroidViewModel(application) {

    private var listTransactionReportLaundryRepository = ListTransactionReportLaundryRepository(application.applicationContext)

    private val _listTransactionLaundryReports = MutableLiveData<List<ListTransactionLaundry>>()
    val listTransactionLaundryReports: LiveData<List<ListTransactionLaundry>> get() = _listTransactionLaundryReports

    init {
        if (listTransactionReportLaundryRepository.isLoggedIn()) {
            getAllListTransactionLaundry()
        }
    }

    private fun getAllListTransactionLaundry() {
        viewModelScope.launch {
            _listTransactionLaundryReports.postValue(listTransactionReportLaundryRepository.getListTransactionReportLaundry().data)
        }
    }


    suspend fun getListTransactionLaundry() {
        try {
            val response = listTransactionReportLaundryRepository.getListTransactionReportLaundry()
            if (response.success) {
                val client = response.data
                _listTransactionLaundryReports.postValue(client) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }

}