package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.api.DefaultRequestPrint
import dev.ferynnd.baguslaundry.data.api.DefaultResponse
import dev.ferynnd.baguslaundry.data.repository.UserRepository
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.repository.report.LaundryReportRepository
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ExportReportLaundry
import dev.ferynnd.baguslaundry.model.LaundryPrintTransaction
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundryResponse
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.User
import dev.ferynnd.baguslaundry.model.TransactionData
import kotlinx.coroutines.launch

class LaundryReportViewModel(application: Application) : AndroidViewModel(application) {


    private var laundryReportRepository: LaundryReportRepository
    private var userRepository: UserRepository
    private val sharedPreferences = SharePrefrenceHelper(application)

    private val _laundryReports = MutableLiveData<List<ReportLaundry>>()
    val laundryReports: LiveData<List<ReportLaundry>> get() = _laundryReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<TransactionData>?>()
    val createTransactionResponse: LiveData<DefaultRequest<TransactionData>?> get() = _createTransactionResponse

    private val _updateTransactionResponse = MutableLiveData<DefaultResponse?>()
    val updateTransactionResponse: LiveData<DefaultResponse?> = _updateTransactionResponse

    // Di dalam LaundryReportViewModel.kt

    private val _printData = MutableLiveData<LaundryPrintTransaction>()
    val printData: LiveData<LaundryPrintTransaction> get() = _printData


    private var currentActiveStatusFilter: StatusReportLaundry? = null

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error =
        MutableLiveData<String>() // Tidak lagi nullable, diinisialisasi dengan string kosong
    val error: LiveData<String> = _error

    private val _filteredLaundryReports = MutableLiveData<List<ReportLaundry>>()
    val filteredLaundryReports: LiveData<List<ReportLaundry>> get() = _filteredLaundryReports

    init {
        laundryReportRepository = LaundryReportRepository(application)
        userRepository = UserRepository(application)
    }

    fun init(context: Context) {
        laundryReportRepository = LaundryReportRepository(context)
        userRepository = UserRepository(context)
        getAllReportLaundry()
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

            val branchId = userResponse.data?.id_branch_user
            val response = laundryReportRepository.getReportLaundry()
            if (response.success) {
                val laundryReports = response.data
                val filteredList =
                    laundryReports.filter { it.id_branch_transaction_laundry == branchId }
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

    suspend fun getReportLatestLaundry() {
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

            val branchId = userResponse.data?.id_branch_user
            val response = laundryReportRepository.getReportLaundry()
            if (response.success) {
                val laundryReports = response.data

                val filteredList = laundryReports.filter {
                    it.id_branch_transaction_laundry == branchId &&
                            (it.status_transaction_laundry == StatusReportLaundry.paid ||
                                    it.status_transaction_laundry == StatusReportLaundry.unpaid)
                }

                _laundryReports.postValue(filteredList)
                _filteredLaundryReports.postValue(filteredList)
            } else {
                _error.postValue("Permintaan API gagal saat mengambil transaksi laundry: ${response.message}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil transaksi laundry.")
        } finally {
            _loading.postValue(false) // Set loading to false
        }
    }

    suspend fun updateTransactionStatus(reportToUpdate: ReportLaundry) {
        _loading.postValue(true)
        _error.postValue("")
        _updateTransactionResponse.postValue(null) // Reset response
        try {
            // reportToUpdate sudah berisi status baru
            val response = laundryReportRepository.updateReportLaundry(reportToUpdate)
            Log.d("UserViewModel", "Response: $response")
            _updateTransactionResponse.postValue(response)
            if (response.success) {
                // Setelah berhasil update, muat ulang laporan terbaru (paid/unpaid/proses)
                getReportLatestLaundry() // Panggil tanpa parameter
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Gagal memperbarui status transaksi.")
        } finally {
            _loading.postValue(false)
        }
    }

    suspend fun getLaundryPrint(id: Int): DefaultRequestPrint<LaundryPrintTransaction> {
        _loading.postValue(true)
        _error.postValue("") // Reset error message

        var result: DefaultRequestPrint<LaundryPrintTransaction> =
        // Anda mungkin perlu memberikan nilai default awal atau membuatnya nullable
            // Berikan nilai default yang sesuai atau jadikan result nullable
            DefaultRequestPrint( // Contoh nilai default, sesuaikan dengan konstruktor DefaultRequestPrint
                data = null, // Atau DefaultRequestPrint() jika konstruktornya tanpa argumen
                message = "Initial",
                success = false
            )


        try {
            val response = laundryReportRepository.getLaundryPrint(id)
            Log.d("LaundryReportViewModel", "getReportLaundryById result: $response")

            // === BAGIAN YANG DIPERBAIKI DI SINI ===
            result = response // <--- Ubah dari response.data menjadi response
            // =====================================

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Terjadi kesalahan yang tidak diketahui."
            Log.e("LaundryReport", errorMessage)
            _error.postValue(errorMessage)
            // Jika terjadi kesalahan, Anda mungkin ingin mengembalikan DefaultRequestPrint yang menandakan error
            result = DefaultRequestPrint(
                data = null, // Data akan null saat error
                message = errorMessage,
                success = false
            )
        } finally {
            _loading.postValue(false)
        }

        return result
    }

//
//    suspend fun getLaundryPrint(id: Int?): DefaultRequest<LaundryPrintTransaction> {
//        _loading.postValue(true) // Set loading to true
//        _error.postValue("") // Reset error message
//        return try {
//            laundryReportRepository.getLaundryPrint(id)
//            Log.d("Print", "Response: $id")
//            // Penting: Pastikan ini mengembalikan objek DefaultRequest yang valid, bukan null
//            Log.d("Print", "Response: ${laundryReportRepository.getLaundryPrint(id)}")
//        } catch (e: Exception) {
//            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil laporan laundry berdasarkan ID.")
//            DefaultRequest(success = false, message = e.message.toString(), data = null)
//        } finally {
//            _loading.postValue(false) // Set loading to false
//        }  as DefaultRequest<LaundryPrintTransaction>
//
//    }

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

    // Fungsi untuk clear/reset data
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

    fun postPrintData(data: LaundryPrintTransaction) {
        _printData.postValue(data)
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
                    users.find { it.id_user == report.id_kurir_transaction_laundry }?.username ?: ""

                report.id_transaction_laundry.toString().contains(query, ignoreCase = true) ||
                        branchName.contains(query, ignoreCase = true) ||
                        userName.contains(query, ignoreCase = true)
            }
        }
    }


}