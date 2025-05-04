package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.repository.report.ListTransactionReportRentalRepository
import dev.ferynnd.baguslaundry.model.ListTransactionRental
import kotlinx.coroutines.launch

class ListTransactionReportRentalViewModel  (application: Application) : AndroidViewModel(application) {

    private var listTransactionReportRentalRepository = ListTransactionReportRentalRepository(application.applicationContext)

    private val _listTransactionRentalReports = MutableLiveData<List<ListTransactionRental>>()
    val listTransactionRentalReports: LiveData<List<ListTransactionRental>> get() = _listTransactionRentalReports

    fun init(context: Context) {
        listTransactionReportRentalRepository = ListTransactionReportRentalRepository(context)
        getAllListTransactionRental()
    }

    init {
        if (listTransactionReportRentalRepository.isLoggedIn()) {
            getAllListTransactionRental()
        }
    }

    private fun getAllListTransactionRental() {
        viewModelScope.launch {
            _listTransactionRentalReports.postValue(listTransactionReportRentalRepository.getListTransactionReportRental().data)
        }
    }


    suspend fun getListTransactionRental() {
        try {
            val response = listTransactionReportRentalRepository.getListTransactionReportRental()
            if (response.success) {
                val client = response.data
                _listTransactionRentalReports.postValue(client) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }

}