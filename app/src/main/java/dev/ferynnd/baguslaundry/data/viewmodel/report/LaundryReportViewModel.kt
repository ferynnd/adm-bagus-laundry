package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.report.LaundryReportRepository
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.LaundryTransactionResponse
import dev.ferynnd.baguslaundry.model.ReportLaundry
import kotlinx.coroutines.launch

class LaundryReportViewModel (application: Application) : AndroidViewModel(application) {

    private lateinit var laundryReportRepository: LaundryReportRepository

    private val _laundryReports = MutableLiveData<List<ReportLaundry>>()
    val laundryReports: LiveData<List<ReportLaundry>> get() = _laundryReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<LaundryTransactionResponse>?>()
    val createTransactionResponse: LiveData<DefaultRequest<LaundryTransactionResponse>?> get() = _createTransactionResponse

    fun init(context: Context) {
        laundryReportRepository = LaundryReportRepository(context)
        getAllReportLaundry()
    }

    private fun getAllReportLaundry() {
        viewModelScope.launch {
            _laundryReports.postValue(laundryReportRepository.getReportLaundry().data)
        }
    }

    suspend fun getReportLaundry() {
        try {
            val response = laundryReportRepository.getReportLaundry()
            if (response.success) {
                val client = response.data
                _laundryReports.postValue(client) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        return laundryReportRepository.getReportLaundryById(id)
    }

    fun createReportLaundry(laundryTransactionRequest: LaundryTransactionRequest) {
        viewModelScope.launch {
            try {
                val response = laundryReportRepository.createReportLaundry(laundryTransactionRequest)
                _createTransactionResponse.value = response
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun resetCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }
}