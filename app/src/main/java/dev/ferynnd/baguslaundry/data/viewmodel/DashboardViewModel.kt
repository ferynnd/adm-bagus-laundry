package dev.ferynnd.baguslaundry.data.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportRental
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class DashboardViewModel (application: Application) : AndroidViewModel(application) {

    private val laundryReportViewModel: LaundryReportViewModel =
        LaundryReportViewModel(application)
    private val rentalReportViewModel: RentalReportViewModel =
        RentalReportViewModel(application)

    private val _latestTransactions = MutableLiveData<List<Any>>()
    val latestTransactions: LiveData<List<Any>> = _latestTransactions

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: MutableLiveData<String?> = _error

    init {
        laundryReportViewModel.init(application.applicationContext)
        rentalReportViewModel.init(application.applicationContext)
        loadLatestTransactions()
    }

    fun loadLatestTransactions() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // Ambil data dari kedua ViewModel
                laundryReportViewModel.getReportLaundry()
                rentalReportViewModel.fetchReportRental()

                // Combine the lists.
                val laundryList = laundryReportViewModel.laundryReports.value ?: emptyList()
                val rentalList = rentalReportViewModel.rentalReports.value ?: emptyList()
                val combinedList: MutableList<Any> = (laundryList + rentalList).toMutableList()

                // Sort the combined list
                 combinedList.sortByDescending {
                    when (it) {
                        is ReportLaundry -> it.formatted_first_date
                        is ReportRental -> it.formatted_time_transaction_rental
                        else -> ""
                    }
                }

                _latestTransactions.value = combinedList.take(8)
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
                _loading.value = false
            }
        }
    }
}