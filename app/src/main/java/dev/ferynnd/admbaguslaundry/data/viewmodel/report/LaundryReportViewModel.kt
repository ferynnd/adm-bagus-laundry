package dev.ferynnd.admbaguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.admbaguslaundry.data.api.*
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.data.repository.BranchRepository
import dev.ferynnd.admbaguslaundry.data.repository.UserRepository
import dev.ferynnd.admbaguslaundry.data.repository.report.LaundryReportRepository
import dev.ferynnd.admbaguslaundry.model.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class LaundryReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var laundryReportRepository: LaundryReportRepository
    private lateinit var userRepository: UserRepository
    private lateinit var branchRepository: BranchRepository
    private val sharedPreferences = SharePrefrenceHelper(application)

    private var isInitialized = false

    private val _laundryReports = MutableLiveData<List<ReportLaundry>>()
    val laundryReports: LiveData<List<ReportLaundry>> get() = _laundryReports

    private val _filteredLaundryReports = MutableLiveData<List<ReportLaundry>>()
    val filteredLaundryReports: LiveData<List<ReportLaundry>> get() = _filteredLaundryReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<TransactionData>?>()
    val createTransactionResponse: LiveData<DefaultRequest<TransactionData>?> get() = _createTransactionResponse

    private val _updateTransactionResponse = MutableLiveData<DefaultResponse?>()
    val updateTransactionResponse: LiveData<DefaultResponse?> = _updateTransactionResponse


    private val _deleteTransactionResponse = MutableLiveData<DefaultRequest<ReportLaundry>?>()
    val deleteTransactionResponse: LiveData<DefaultRequest<ReportLaundry>?> get() = _deleteTransactionResponse

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
    }

    fun resetDeleteTransactionResponse() {
        _deleteTransactionResponse.postValue(null)
    }
    suspend fun getReportLaundry() {
        _loading.postValue(true)
        _error.postValue("")

        try {
            val response = laundryReportRepository.getReportLaundry()

            if (response.success) {
                val items = response.data?.items ?: emptyList()

                _laundryReports.postValue(items)
                _filteredLaundryReports.postValue(items)
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

    suspend fun getReportLatestLaundry() {
        _loading.postValue(true)
        _error.postValue("")

        try {
            val response = laundryReportRepository.getReportLaundry()

            if (response.success) {
                val items = response.data?.items ?: emptyList()

                _laundryReports.postValue(items)
                _filteredLaundryReports.postValue(items)
            } else {
                _error.postValue("Gagal mengambil transaksi laundry: ${response.message}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan")
        } finally {
            _loading.postValue(false)
        }
    }

    suspend fun getLaundryPrint(id: Int): DefaultRequestPrint<LaundryPrintTransaction> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            laundryReportRepository.getLaundryPrint(id)
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Terjadi kesalahan"
            _error.postValue(errorMessage)
            DefaultRequestPrint(data = null, message = errorMessage, success = false)
        } finally {
            _loading.postValue(false)
        } as DefaultRequestPrint<LaundryPrintTransaction>
    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            laundryReportRepository.getReportLaundryById(id)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan")
            DefaultRequest(false, e.message.toString(), null)
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

     private  val TAG = "UPDATE_LAUNDRY_VM"

    suspend fun updateReportLaundry(
        id: Int,
        request: UpdateLaundryFullRequest
    ): DefaultResponse {

        try {

            Log.d(TAG, "CALL UPDATE FROM VIEWMODEL")
            Log.d(TAG, "TRANSACTION ID : $id")
            Log.d(TAG, "REQUEST : $request")

            val response = laundryReportRepository.updateReportLaundry(id, request)

            Log.d(TAG, "UPDATE SUCCESS")
            Log.d(TAG, "RESPONSE : $response")

            return response

        } catch (e: Exception) {

            Log.e(TAG, "UPDATE FAILED")
            Log.e(TAG, "ERROR : ${e.message}")
            Log.e(TAG, "STACKTRACE : ", e)

            throw e
        }
    }

    fun deleteReportLaundry(id: Int) {
        _loading.postValue(true)
        _error.postValue("")
        _deleteTransactionResponse.postValue(null)

        viewModelScope.launch {

            try {


                val response = laundryReportRepository.deleteReportLaundry(id)

                _deleteTransactionResponse.postValue(response)

                if (response.success) {
                    getReportLaundry()

                }

            } catch (e: Exception) {

                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat menghapus transaksi"
                )

            } finally {

                Log.d(TAG, "FINISH DELETE LOADING")

                _loading.postValue(false)
            }
        }
    }

    fun forceDeleteLaundryTransaction(id: Int) {
        _loading.postValue(true)
        _error.postValue("")
        _deleteTransactionResponse.postValue(null)

        viewModelScope.launch {

            try {

                val response = laundryReportRepository.forceDeleteReportLaundry(id)
                _deleteTransactionResponse.postValue(response)

                if (response.success) {
                    getReportLaundry()
                }

            } catch (e: Exception) {

                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat menghapus permanen transaksi"
                )

            } finally {

                Log.d(TAG, "FINISH FORCE DELETE LOADING")

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
            DefaultRequest(false, e.message.toString(), null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<ReportLaundryResponse>
    }

    fun filterClient(branchId: Int?) {
        val allLaundryReports = _laundryReports.value ?: return

        _filteredLaundryReports.value = if (branchId == null || branchId == -1) {
            allLaundryReports
        } else {
            allLaundryReports.filter { it.id_branch_transaction_laundry == branchId }
        }
    }

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

                report.id_transaction_laundry.toString().contains(query, true) ||
                        branchName.contains(query, true) ||
                        userName.contains(query, true)
            }
        }
    }

    fun setBranches(data: List<Branch>) {
        branches = data
    }

    fun setUsers(data: List<User>) {
        users = data
    }

    fun postPrintData(data: LaundryPrintTransaction) {
        _printData.postValue(data)
    }

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