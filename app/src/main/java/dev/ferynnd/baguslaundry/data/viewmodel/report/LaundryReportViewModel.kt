package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.UserRepository
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.repository.report.LaundryReportRepository
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ExportReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundryResponse
import dev.ferynnd.baguslaundry.model.User
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.LaundryTransactionResponse
import dev.ferynnd.baguslaundry.model.TransactionData
import kotlinx.coroutines.launch

class LaundryReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var laundryReportRepository: LaundryReportRepository

    private val sharedPreferences = SharePrefrenceHelper(application)

    private lateinit var userRepository: UserRepository

    private val _laundryReports = MutableLiveData<List<ReportLaundry>>()
    val laundryReports: LiveData<List<ReportLaundry>> get() = _laundryReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<TransactionData>?>()
    val createTransactionResponse: LiveData<DefaultRequest<TransactionData>?> get() = _createTransactionResponse

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error =
        MutableLiveData<String>() // Tidak lagi nullable, diinisialisasi dengan string kosong
    val error: LiveData<String> = _error

    private val _filteredLaundryReports = MutableLiveData<List<ReportLaundry>>()
    val filteredLaundryReports: LiveData<List<ReportLaundry>> get() = _filteredLaundryReports


    fun init(context: Context) {
        laundryReportRepository = LaundryReportRepository(context)
        userRepository = UserRepository(context)
        getAllReportLaundry()
    }

    // Fungsi publik untuk mereset pesan error
    fun resetErrorMessage() {
        _error.postValue("") // Gunakan postValue untuk memastikan pembaruan terjadi di main thread
    }

    private fun getAllReportLaundry() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = laundryReportRepository.getReportLaundry()
                if (response.success) {
                    _laundryReports.postValue(response.data)
                    _filteredLaundryReports.postValue(response.data) // Inisialisasi filtered dengan semua data
                } else {
                    _error.postValue(response.message ?: "Gagal mengambil data laundry awal.")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat memuat transaksi laundry awal."
                )
            } finally {
                _loading.postValue(false) // Set loading to false
            }
        }
    }

    suspend fun getReportLaundry() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        try {
            val userId = sharedPreferences.getString("PREF_USER_ID")?.toIntOrNull()
            if (userId == null) {
                _error.postValue("ID user tidak ditemukan di SharedPreferences.")
                return
            }

            val userResponse = userRepository.getUserById(userId)
            if (!userResponse.success) {
                _error.postValue("Gagal mengambil data user.")
                return
            }

            val branchId = userResponse.data.id_branch_user
            val response = laundryReportRepository.getReportLaundry()
            if (response.success) {
                val laundryReports = response.data
                val filteredList = laundryReports.filter { it.id_branch_transaction_laundry == branchId }
                _laundryReports.postValue(filteredList)
                _filteredLaundryReports.postValue(filteredList) // Update filtered list as well
            } else {
                _error.postValue("Permintaan API gagal saat mengambil transaksi laundry: ${response.message}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil transaksi laundry.")
        } finally {
            _loading.postValue(false) // Set loading to false
        }
    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            laundryReportRepository.getReportLaundryById(id)
        } catch (e: Exception) {
            _error.postValue(
                e.message ?: "Terjadi kesalahan saat mengambil laporan laundry berdasarkan ID."
            )
            // Penting: Pastikan ini mengembalikan objek DefaultRequest yang valid, bukan null
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Set loading to false
        } as DefaultRequest<ReportLaundry>
    }

    fun createReportLaundry(laundryTransactionRequest: TransactionData) {
        _loading.postValue(true)
        _error.postValue("")
        _createTransactionResponse.postValue(null) // Reset response
        viewModelScope.launch {
            try {
                val response =
                    laundryReportRepository.createReportLaundry(laundryTransactionRequest)
                _createTransactionResponse.postValue(response)
                // Logic error di sini akan ditangani oleh observer _error atau _createTransactionResponse
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat membuat transaksi laundry.")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun clearCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }

    fun clearError() {
        _error.value = ""
    }


    fun resetCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }

    suspend fun exportLaundryMonthly(exportReportLaundry: ExportReportLaundry): DefaultRequest<ReportLaundryResponse> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            laundryReportRepository.exportLaundryMonthly(exportReportLaundry)
        } catch (e: Exception) {
            _error.postValue(
                e.message ?: "Terjadi kesalahan saat mengekspor laporan bulanan laundry."
            )
            // Penting: Pastikan ini mengembalikan objek DefaultRequest yang valid, bukan null
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Set loading to false
        } as DefaultRequest<ReportLaundryResponse>
    }

    fun filterClient(branchId: Int?) {
        // Tidak perlu indikator loading di sini karena ini adalah operasi filter lokal
        val allLaundryReports = _laundryReports.value ?: return
        _filteredLaundryReports.value =
            if (branchId == null || branchId == -1) { // Menambahkan kondisi untuk "Semua Cabang" (-1)
                allLaundryReports
            } else {
                allLaundryReports.filter { it.id_branch_transaction_laundry == branchId }
            }
    }

    private var branches: List<Branch> = emptyList()
    private var users: List<User> = emptyList()

    fun setBranches(data: List<Branch>) {
        branches = data
    }

    fun setUsers(data: List<User>) {
        users = data
    }

    fun searchLaundryReports(query: String) {
        // Tidak perlu indikator loading di sini karena ini adalah operasi pencarian lokal
        val allLaundryReports = _laundryReports.value ?: return
        if (query.isBlank()) {
            _filteredLaundryReports.value = allLaundryReports
        } else {
            _filteredLaundryReports.value = allLaundryReports.filter { report ->
                val branchName =
                    branches.find { it.id_branch == report.id_branch_transaction_laundry }?.name_branch
                        ?: ""
                val userName =
                    users.find { it.id_user == report.id_user_transaction_laundry }?.username ?: ""

                report.id_transaction_laundry.toString().contains(query, ignoreCase = true) ||
                        branchName.contains(query, ignoreCase = true) ||
                        userName.contains(query, ignoreCase = true)
            }
        }
    }
}