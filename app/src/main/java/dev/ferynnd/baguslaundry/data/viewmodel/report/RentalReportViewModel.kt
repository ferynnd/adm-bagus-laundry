package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.api.DefaultRequestInvoice
import dev.ferynnd.baguslaundry.data.api.DefaultRequestPrint
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.repository.UserRepository
import dev.ferynnd.baguslaundry.data.repository.report.RentalReportRepository
import dev.ferynnd.baguslaundry.model.RentalTransactionResponse
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.RentalTransactionRequest
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ExportInvoicePdfRentalRequest
import dev.ferynnd.baguslaundry.model.ExportReportRental
import dev.ferynnd.baguslaundry.model.InvoiceRentalResponse
import dev.ferynnd.baguslaundry.model.PostInvoiceRentalRequest
import dev.ferynnd.baguslaundry.model.RentalPrintTransaction
import dev.ferynnd.baguslaundry.model.RentalTransactionData
import dev.ferynnd.baguslaundry.model.ReportRentalResponse
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.launch

class RentalReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var rentalReportRepository: RentalReportRepository

    private val sharedPreferences = SharePrefrenceHelper(application)

    private lateinit var userRepository: UserRepository

    private val _rentalReports = MutableLiveData<List<ReportRental>>()
    val rentalReports: LiveData<List<ReportRental>> get() = _rentalReports

    private val _createTransactionResponse =
        MutableLiveData<DefaultRequest<RentalTransactionData>?>()
    val createTransactionResponse: LiveData<DefaultRequest<RentalTransactionData>?> get() = _createTransactionResponse

    private val _printData = MutableLiveData<RentalPrintTransaction>()
    val printData: LiveData<RentalPrintTransaction> get() = _printData

    private val _invoiceRental = MutableLiveData<List<InvoiceRentalResponse>>()
    val invoiceRental: LiveData<List<InvoiceRentalResponse>> get() = _invoiceRental

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>() // Ubah menjadi non-nullable String
    val error: LiveData<String> get() = _error

    // Inisialisasi daftar terfilter di sini
    private val _filteredInvoices =
        MutableLiveData<List<InvoiceRentalResponse>>()
    val filteredInvoices: LiveData<List<InvoiceRentalResponse>> get() = _filteredInvoices

    private val _filteredRentalReports = MutableLiveData<List<ReportRental>>()
    val filteredRentalReports: LiveData<List<ReportRental>> get() = _filteredRentalReports


    fun init(context: Context) {
        rentalReportRepository = RentalReportRepository(context)
        userRepository = UserRepository(context)
        getAllReportRental()
        getAllInvoiceRental()
    }

    // Fungsi publik untuk mereset pesan error
    fun resetErrorMessage() {
        _error.postValue("") // Gunakan postValue untuk memastikan pembaruan terjadi di main thread
    }

    private fun getAllReportRental() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.getReportRental()
                if (response.success && response.data != null) {
                    _rentalReports.postValue(response.data)
                    _filteredRentalReports.postValue(response.data) // Inisialisasi filtered list
                } else {
                    _error.postValue("Gagal memuat laporan rental awal: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat memuat data laporan rental awal."
                )
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    private fun getAllInvoiceRental() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.getInvoiceRental()
                if (response.success && response.data != null) {
                    _invoiceRental.postValue(response.data)
                    _filteredInvoices.postValue(response.data) // Inisialisasi filtered list
                    Log.d("InvoiceRental", "Invoice Rental: ${_invoiceRental.value}")
                } else {
                    _error.postValue("Gagal memuat invoice rental awal: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat memuat data invoice rental awal."
                )
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    suspend fun getReportRental() {
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
            val response = rentalReportRepository.getReportRental()
            if (response.success) {
                val rentalReports = response.data
                val filteredList =
                    rentalReports.filter { it.id_branch_transaction_rental == branchId }
                _rentalReports.postValue(filteredList)
                _filteredRentalReports.postValue(filteredList) // Update filtered list as well
            } else {
                _error.postValue("Permintaan API gagal saat mengambil transaksi rental: ${response.message}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil transaksi rental.")
        } finally {
            _loading.postValue(false)
        }
    }

    // Mengganti suspend fun getReportRental() menjadi non-suspend dan memperbarui LiveData
    fun fetchReportRental() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.getReportRental()
                if (response.success && response.data != null) {
                    _rentalReports.postValue(response.data)
                    _filteredRentalReports.postValue(response.data) // Perbarui juga filtered list
                } else {
                    _error.postValue("API request gagal saat mengambil laporan rental: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat mengambil data laporan rental."
                )
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    suspend fun getRentalPrint(id: Int): DefaultRequestPrint<RentalPrintTransaction> {
        _loading.postValue(true)
        _error.postValue("") // Reset error message

        var result: DefaultRequestPrint<RentalPrintTransaction> =
        // Anda mungkin perlu memberikan nilai default awal atau membuatnya nullable
            // Berikan nilai default yang sesuai atau jadikan result nullable
            DefaultRequestPrint( // Contoh nilai default, sesuaikan dengan konstruktor DefaultRequestPrint
                data = null, // Atau DefaultRequestPrint() jika konstruktornya tanpa argumen
                message = "Initial",
                success = false
            )

        try {
            val response = rentalReportRepository.getRentalPrint(id)
            Log.d("RentalReportViewModel", "Response: ${response}")

            // === BAGIAN YANG DIPERBAIKI DI SINI ===
            result = response // <--- Ubah dari response.data menjadi response
            // =====================================

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Terjadi kesalahan yang tidak diketahui."
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


    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            rentalReportRepository.getReportRentalById(id)
        } catch (e: Exception) {
            _error.postValue(
                e.message ?: "Terjadi kesalahan saat mengambil laporan rental berdasarkan ID."
            )
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequest<ReportRental>
    }

    fun createRentalTransaction(rentalTransactionRequest: RentalTransactionRequest) {
        _loading.postValue(true)
        _error.postValue("")
        _createTransactionResponse.postValue(null) // Reset response
        viewModelScope.launch {
            try {
                val response =
                    rentalReportRepository.createReportRental(rentalTransactionRequest)
                _createTransactionResponse.postValue(response)
                // Logic error di sini akan ditangani oleh observer _error atau _createTransactionResponse
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat membuat transaksi laundry.")
            } finally {
                _loading.postValue(false)
            }
        }
    }

//    fun createRentalTransaction(rentalTransactionRequest: RentalTransactionRequest) {
//        _loading.postValue(true) // Set loading to true
//        _error.postValue("") // Reset error message
//        _createTransactionResponse.postValue(null) // Reset response
//        viewModelScope.launch {
//            try {
//                val response = rentalReportRepository.createReportRental(rentalTransactionRequest)
//                _createTransactionResponse.postValue(response) // Gunakan postValue
//                if (!response.success) {
//                    Log.e("API_ERROR", "Error: ${response.errors}")
//                    _error.postValue(response.message ?: "Gagal membuat transaksi rental.")
//                } else {
//                    // Refresh data setelah transaksi berhasil
//                    getAllReportRental()
//                    getAllInvoiceRental()
//                }
//            } catch (e: Exception) {
//                Log.e("RentalReportViewModel", "Error creating rental transaction", e)
//                _error.postValue(e.message ?: "Terjadi kesalahan saat membuat transaksi rental.")
//                _createTransactionResponse.postValue(null)
//            } finally {
//                _loading.postValue(false) // Always set loading to false
//            }
//        }
//    }

    fun resetCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }

    suspend fun exportRentalMonthly(exportReport: ExportReportRental): DefaultRequest<ReportRentalResponse> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            rentalReportRepository.exportRentalMonthly(exportReport)
        } catch (e: Exception) {
            _error.postValue(
                e.message ?: "Terjadi kesalahan saat mengekspor laporan rental bulanan."
            )
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequest<ReportRentalResponse>
    }

    suspend fun createInvoiceRental(postInvoiceReportRental: PostInvoiceRentalRequest): DefaultRequestInvoice<InvoiceRentalResponse> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            val response = rentalReportRepository.createInvoiceRental(postInvoiceReportRental)
            if (response.success) {
                // Refresh daftar invoice setelah pembuatan berhasil
                getAllInvoiceRental()
                response // Mengembalikan respons yang berhasil
            } else {
                _error.postValue("Gagal membuat invoice rental: ${response.message ?: "Pesan tidak tersedia"}")
                DefaultRequestInvoice(success = false, message = response.message, data = null)
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Request failed saat membuat invoice rental.")
            DefaultRequestInvoice(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequestInvoice<InvoiceRentalResponse>
    }

    suspend fun exportInvoiceRental(exportInvoiceRental: ExportInvoicePdfRentalRequest): DefaultRequestInvoice<ReportRentalResponse> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        return try {
            rentalReportRepository.exportInvoiceRental(exportInvoiceRental)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengekspor invoice rental.")
            DefaultRequestInvoice(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequestInvoice<ReportRentalResponse>
    }

    // Mengganti suspend fun getInvoiceRental() menjadi non-suspend dan memperbarui LiveData
    fun fetchInvoiceRental() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error message
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.getInvoiceRental()
                if (response.success && response.data != null) {
                    _invoiceRental.postValue(response.data)
                    _filteredInvoices.postValue(response.data) // Perbarui juga filtered list
                } else {
                    _error.postValue("API request gagal saat mengambil invoice rental: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat mengambil data invoice rental."
                )
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    fun filterClientInvoice(branchId: Int?) {
        val allInvoices = _invoiceRental.value ?: return
        _filteredInvoices.value = if (branchId == null) {
            allInvoices
        } else {
            allInvoices.filter { it.id_branch_invoice == branchId }
        }
    }

    fun filterClient(branchId: Int?) {
        val allRentalReports = _rentalReports.value ?: return
        _filteredRentalReports.value = if (branchId == null) {
            allRentalReports
        } else {
            allRentalReports.filter { it.id_branch_transaction_rental == branchId }
        }
    }

    private var branches: List<Branch> = emptyList()
    private var client: List<Client> = emptyList()
    private var users: List<User> = emptyList()

    fun setBranches(data: List<Branch>) {
        branches = data
    }

    fun setClient(data: List<Client>) {
        client = data
    }

    fun setUsers(data: List<User>) {
        users = data
    }

    fun postPrintData(data: RentalPrintTransaction) {
        _printData.postValue(data)
    }

    // Fungsi untuk clear/reset data
    fun clearCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }

    fun searchRentalReports(query: String) {
        val allRentalReports = _rentalReports.value ?: return
        if (query.isBlank()) {
            _filteredRentalReports.value = allRentalReports
        } else {
            _filteredRentalReports.value = allRentalReports.filter { report ->
                val branchName =
                    branches.find { it.id_branch == report.id_branch_transaction_rental }?.name_branch
                        ?: ""
                val userName =
                    users.find { it.id_user == report.id_kurir_transaction_rental }?.username ?: ""

                branchName.contains(query, ignoreCase = true) ||
                        userName.contains(query, ignoreCase = true)
            }
        }
    }
}