package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.*
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.helper.TimezoneHelper
import dev.ferynnd.baguslaundry.data.repository.BranchRepository
import dev.ferynnd.baguslaundry.data.repository.UserRepository
import dev.ferynnd.baguslaundry.data.repository.report.LaundryReportRepository
import dev.ferynnd.baguslaundry.model.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class LaundryReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var laundryReportRepository: LaundryReportRepository
    private lateinit var userRepository: UserRepository
    private lateinit var branchRepository: BranchRepository
    private val sharedPreferences = SharePrefrenceHelper(application)

    // Cache untuk branches agar tidak perlu fetch berulang
    private val branchesMap = mutableMapOf<Int, Branch>()

    // Flag untuk memastikan init hanya dipanggil sekali
    private var isInitialized = false

    private val _laundryReports = MutableLiveData<List<ReportLaundry>>()
    val laundryReports: LiveData<List<ReportLaundry>> get() = _laundryReports

    private val _filteredLaundryReports = MutableLiveData<List<ReportLaundry>>()
    val filteredLaundryReports: LiveData<List<ReportLaundry>> get() = _filteredLaundryReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<TransactionData>?>()
    val createTransactionResponse: LiveData<DefaultRequest<TransactionData>?> get() = _createTransactionResponse

    private val _updateTransactionResponse = MutableLiveData<DefaultResponse?>()
    val updateTransactionResponse: LiveData<DefaultResponse?> = _updateTransactionResponse

    private val _printData = MutableLiveData<LaundryPrintTransaction>()
    val printData: LiveData<LaundryPrintTransaction> get() = _printData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var branches: List<Branch> = emptyList()
    private var users: List<User> = emptyList()

    fun init(context: Context) {
        if (isInitialized) return

        laundryReportRepository = LaundryReportRepository(context)
        userRepository = UserRepository(context)
        branchRepository = BranchRepository(context)

        isInitialized = true

        // Load branches terlebih dahulu
        loadBranchesAndData()
    }

    /**
     * Load branches dan data secara berurutan
     */
    private fun loadBranches() {
        viewModelScope.launch {
            try {
                val response = branchRepository.getBranch()
                if (response.success) {
                    branches = response.data
                    response.data.forEach { branch ->
                        branch.id_branch?.let { id ->
                            branchesMap[id] = branch
                        }
                    }
                    Log.d("LaundryViewModel", "Branches loaded: ${branches.size}")
                }
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Konversi waktu UTC dari API ke timezone branch
     */
    private fun formatTimeForBranch(utcTime: String?, branchId: Int?): String {
        if (utcTime == null || branchId == null) {
            return "-"
        }

        val branch = branchesMap[branchId]
        val timezone = branch?.timezone_branch ?: "Asia/Jakarta" // Default timezone


        val formattedTime = TimezoneHelper.convertUtcToBranchTimezone(
            utcTimeString = utcTime,
            branchTimezone = timezone,
            outputFormat = "dd MMM yyyy, HH:mm"
        )

        return formattedTime
    }

    /**
     * Transform data dari API dengan konversi timezone
     */
    private fun transformReportLaundry(report: ReportLaundry): ReportLaundry {
        val formattedFirstDate = formatTimeForBranch(
            report.first_date_transaction_laundry,
            report.id_branch_transaction_laundry
        )
        val formattedLastDate = formatTimeForBranch(
            report.last_date_transaction_laundry,
            report.id_branch_transaction_laundry
        )

        return report.copy(
            formatted_first_date = formattedFirstDate,
            formatted_last_date = formattedLastDate
        )
    }

    /**
     * Konversi waktu UTC dari API ke timezone branch
     */
    private fun formatTimeForBranch(utcTime: String?, branchId: Int?): String {
        if (utcTime == null || branchId == null) {
            return "-"
        }

        val branch = branchesMap[branchId]
        val timezone = branch?.timezone_branch ?: "Asia/Jakarta"

        return try {
            TimezoneHelper.convertUtcToBranchTimezone(
                utcTimeString = utcTime,
                branchTimezone = timezone,
                outputFormat = "dd MMM yyyy, HH:mm"
            )
        } catch (e: Exception) {
            Log.e("LaundryViewModel", "Error formatting time: ${e.message}")
            utcTime
        }
    }

    /**
     * Transform data dari API dengan konversi timezone
     */
    private fun transformReportLaundry(report: ReportLaundry): ReportLaundry {
        val formattedFirstDate = formatTimeForBranch(
            report.first_date_transaction_laundry,
            report.id_branch_transaction_laundry
        )
        val formattedLastDate = formatTimeForBranch(
            report.last_date_transaction_laundry,
            report.id_branch_transaction_laundry
        )

        return report.copy(
            formatted_first_date = formattedFirstDate,
            formatted_last_date = formattedLastDate
        )
    }

    /**
     * Get reports filtered by user's branch
     */
    suspend fun getReportLaundry() {
        _loading.postValue(true)
        _error.postValue("")
        try {
            Log.d("LaundryViewModel", "Getting laundry reports...")

            val userId = sharedPreferences.getString("PREF_USER_ID")?.toIntOrNull()
            if (userId == null) {
                _error.postValue("ID user tidak ditemukan")
                _loading.postValue(false)
                return
            }

            val userResponse = userRepository.getUserById(userId)
            if (!userResponse.success) {
                _error.postValue("Gagal mengambil data user")
                _loading.postValue(false)
                return
            }

            val branchId = userResponse.data?.id_branch_user
            Log.d("LaundryViewModel", "User branch ID: $branchId")

            val response = laundryReportRepository.getReportLaundry()
            Log.d("LaundryViewModel", "API Response success: ${response.success}, data size: ${response.data?.size ?: 0}")

            if (response.success) {
                // Filter dan transform data
                val filteredList = response.data
                    .filter { it.id_branch_transaction_laundry == branchId }
                    .map { transformReportLaundry(it) }

                Log.d("LaundryViewModel", "Filtered laundry reports: ${filteredList.size}")

                _laundryReports.postValue(filteredList)
                _filteredLaundryReports.postValue(filteredList)
            } else {
                _error.postValue("Gagal mengambil transaksi laundry: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("LaundryViewModel", "Error getting laundry reports", e)
            _error.postValue(e.message ?: "Terjadi kesalahan")
        } finally {
            _loading.postValue(false)
        }
    }

    /**
     * Get latest reports (paid/unpaid)
     */
    suspend fun getReportLatestLaundry() {
        _loading.postValue(true)
        _error.postValue("")
        try {
            val userId = sharedPreferences.getString("PREF_USER_ID")?.toIntOrNull()
            if (userId == null) {
                _error.postValue("ID user tidak ditemukan")
                _loading.postValue(false)
                return
            }

            val userResponse = userRepository.getUserById(userId)
            if (!userResponse.success) {
                _error.postValue("Gagal mengambil data user")
                _loading.postValue(false)
                return
            }

            val branchId = userResponse.data?.id_branch_user
            val response = laundryReportRepository.getReportLaundry()

            if (response.success) {
                val filteredList = response.data
                    .filter {
                        it.id_branch_transaction_laundry == branchId &&
                                (it.status_transaction_laundry == StatusReportLaundry.paid ||
                                        it.status_transaction_laundry == StatusReportLaundry.unpaid)
                    }
                    .map { transformReportLaundry(it) }

                _laundryReports.postValue(filteredList)
                _filteredLaundryReports.postValue(filteredList)
            } else {
                _error.postValue("Gagal mengambil transaksi laundry: ${response.message}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan")
        } finally {
            _loading.postValue(false)
        }
    }

    /**
     * Update transaction status
     */
    suspend fun updateTransactionStatus(reportToUpdate: ReportLaundry) {
        _loading.postValue(true)
        _error.postValue("")
        _updateTransactionResponse.postValue(null)
        try {
            val response = laundryReportRepository.updateReportLaundry(reportToUpdate)
            _updateTransactionResponse.postValue(response)
            if (response.success) {
                getReportLatestLaundry()
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Gagal memperbarui status transaksi")
        } finally {
            _loading.postValue(false)
        }
    }

    /**
     * Get print data dengan timezone conversion
     */
    suspend fun getLaundryPrint(id: Int): DefaultRequestPrint<LaundryPrintTransaction> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            val response = laundryReportRepository.getLaundryPrint(id)
            response
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Terjadi kesalahan"
            _error.postValue(errorMessage)
            DefaultRequestPrint(
                data = null,
                message = errorMessage,
                success = false
            )
        } finally {
            _loading.postValue(false)
        }
    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        _loading.postValue(true)
        _error.postValue("")
        return try {
            val response = laundryReportRepository.getReportLaundryById(id)

            if (response.success && response.data != null) {
                val transformedData = transformReportLaundry(response.data)
                DefaultRequest(
                    success = true,
                    message = response.message,
                    data = transformedData
                )
            } else {
                response
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan")
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<ReportLaundry>
    }

    fun createReportLaundry(laundryTransactionRequest: TransactionData) {
        _loading.postValue(true)
        _error.postValue("")
        _createTransactionResponse.postValue(null)
        viewModelScope.launch {
            try {
                val response = laundryReportRepository.createReportLaundry(laundryTransactionRequest)
                _createTransactionResponse.postValue(response)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat membuat transaksi")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    suspend fun exportLaundryMonthly(exportReportLaundry: ExportReportLaundry): DefaultRequest<ReportLaundryResponse> {
        _loading.postValue(true)
        _error.postValue("")
        return try {
            laundryReportRepository.exportLaundryMonthly(exportReportLaundry)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengekspor")
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<ReportLaundryResponse>
    }

    /**
     * Filter by branch
     */
    fun filterClient(branchId: Int?) {
        val allLaundryReports = _laundryReports.value ?: return
        _filteredLaundryReports.value = if (branchId == null || branchId == -1) {
            allLaundryReports
        } else {
            allLaundryReports.filter { it.id_branch_transaction_laundry == branchId }
        }
    }

    /**
     * Search reports
     */
    fun searchLaundryReports(query: String) {
        val allLaundryReports = _laundryReports.value ?: return
        if (query.isBlank()) {
            _filteredLaundryReports.value = allLaundryReports
        } else {
            _filteredLaundryReports.value = allLaundryReports.filter { report ->
                val branchName = branches.find {
                    it.id_branch == report.id_branch_transaction_laundry
                }?.name_branch ?: ""
                val userName = users.find {
                    it.id_user == report.id_kurir_transaction_laundry
                }?.username ?: ""

                report.id_transaction_laundry.toString().contains(query, ignoreCase = true) ||
                        branchName.contains(query, ignoreCase = true) ||
                        userName.contains(query, ignoreCase = true)
            }
        }
    }

    // Setter methods
    fun setBranches(data: List<Branch>) {
        branches = data
        data.forEach { branch ->
            branch.id_branch?.let { id ->
                branchesMap[id] = branch
            }
        }
    }

    fun setUsers(data: List<User>) {
        users = data
    }

    fun postPrintData(data: LaundryPrintTransaction) {
        _printData.postValue(data)
    }

    // Clear methods
    fun clearCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }

    fun clearUpdateTransactionResponse() {
        _updateTransactionResponse.value = null
    }

    fun clearError() {
        _error.value = ""
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }

    fun resetCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }
}