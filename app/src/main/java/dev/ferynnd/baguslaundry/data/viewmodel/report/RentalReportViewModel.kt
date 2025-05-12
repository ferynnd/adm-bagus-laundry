package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.report.RentalReportRepository
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.LaundryTransactionResponse
import dev.ferynnd.baguslaundry.model.RentalTransactionResponse
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.RentalTransactionRequest
import kotlinx.coroutines.launch

class RentalReportViewModel (application: Application) : AndroidViewModel(application) {

    private lateinit var rentalReportRepository: RentalReportRepository

    private val _rentalReports = MutableLiveData<List<ReportRental>>()
    val rentalReports: LiveData<List<ReportRental>> get() = _rentalReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<RentalTransactionResponse>?>()
    val createTransactionResponse: LiveData<DefaultRequest<RentalTransactionResponse>?> get() = _createTransactionResponse

    fun init(context: Context) {
        rentalReportRepository = RentalReportRepository(context)
        getAllReportRental()
    }

    private fun getAllReportRental() {
        viewModelScope.launch {
            _rentalReports.postValue(rentalReportRepository.getReportRental().data)
        }
    }


    suspend fun getReportRental() {
        try {
            val response = rentalReportRepository.getReportRental()
            if (response.success) {
                val client = response.data
                _rentalReports.postValue(client) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }

//    suspend fun createReportRental(client: ReportRental) {
//        rentalReportRepository.createReportRental(client)
//    }

    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        return rentalReportRepository.getReportRentalById(id)
    }

    fun createRentalTransaction(rentalTransactionRequest: RentalTransactionRequest) {
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.createReportRental(rentalTransactionRequest)
                _createTransactionResponse.value = response
                if (!response.success) {
                    Log.e("API_ERROR", "Error: ${response.errors}")
                }
            } catch (e: Exception) {
                Log.e("RentalReportViewModel", "Error creating rental transaction", e)
                // Handle the exception appropriately
            }
        }
    }

    fun resetCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }
}