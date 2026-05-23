package dev.ferynnd.baguslaundry.data.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.data.repository.UserRepository
import dev.ferynnd.admbaguslaundry.data.repository.report.LaundryReportRepository
import dev.ferynnd.admbaguslaundry.data.repository.report.RentalReportRepository
import dev.ferynnd.admbaguslaundry.model.ReportLaundry
import dev.ferynnd.admbaguslaundry.model.ReportRental
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "DashboardViewModel"

    private val context = application.applicationContext

    private val laundryReportRepository = LaundryReportRepository(context)
    private val rentalReportRepository = RentalReportRepository(context)
    private val userRepository = UserRepository(context)
    private val sharedPreferences = SharePrefrenceHelper(context)

    private val _latestTransactions = MutableLiveData<List<Any>>()
    val latestTransactions: LiveData<List<Any>> = _latestTransactions

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadLatestTransactions()
    }

    fun loadLatestTransactions() {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Mulai load latest transactions tanpa filter user/branch")

                val laundryResponse = laundryReportRepository.getReportLaundry(1)
                val rentalResponse = rentalReportRepository.getReportRental(1)

                Log.d(TAG, "Laundry response success: ${laundryResponse.success}")
                Log.d(TAG, "Rental response success: ${rentalResponse.success}")

                val laundryList = if (laundryResponse.success) {
                    laundryResponse.data?.items ?: emptyList()
                } else {
                    emptyList()
                }

                val rentalList = if (rentalResponse.success) {
                    rentalResponse.data?.items ?: emptyList()
                } else {
                    emptyList()
                }

                Log.d(TAG, "Laundry raw size: ${laundryList.size}")
                Log.d(TAG, "Rental raw size: ${rentalList.size}")

                val combinedList: MutableList<Any> =
                    (laundryList + rentalList).toMutableList()

                combinedList.sortByDescending {
                    when (it) {
                        is ReportLaundry -> it.first_date_transaction_laundry ?: ""
                        is ReportRental -> it.time_transaction_rental ?: ""
                        else -> ""
                    }
                }

                val latest = combinedList.take(8)

                Log.d(TAG, "Latest final size: ${latest.size}")

                latest.forEachIndexed { index, item ->
                    Log.d(TAG, "Latest[$index]: $item")
                }

                _latestTransactions.postValue(latest)

            } catch (e: Exception) {
                Log.e(TAG, "Error load latest transactions: ${e.message}", e)
                _error.postValue(e.message ?: "An error occurred")
            } finally {
                _loading.postValue(false)
            }
        }
    }
}