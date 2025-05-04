package dev.ferynnd.baguslaundry.data.viewmodel.report
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.report.RentalReportRepository
import dev.ferynnd.baguslaundry.model.ReportRental
import kotlinx.coroutines.launch

class RentalReportViewModel   (application: Application) : AndroidViewModel(application) {

    private var rentalReportRepository = RentalReportRepository(application.applicationContext)

    private val _rentalReports = MutableLiveData<List<ReportRental>>()
    val rentalReports: LiveData<List<ReportRental>> get() = _rentalReports

    init {
        if (rentalReportRepository.isLoggedIn()) {
            getAllReportRental()
        }
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


}