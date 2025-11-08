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
import dev.ferynnd.baguslaundry.data.repository.report.RentalReportRepository
import dev.ferynnd.baguslaundry.model.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class RentalReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var rentalReportRepository: RentalReportRepository
    private lateinit var userRepository: UserRepository
    private lateinit var branchRepository: BranchRepository
    private val sharedPreferences = SharePrefrenceHelper(application)

    // Cache untuk branches
    private val branchesMap = mutableMapOf<Int, Branch>()

    // Flag untuk memastikan init hanya dipanggil sekali
    private var isInitialized = false

    private val _rentalReports = MutableLiveData<List<ReportRental>>()
    val rentalReports: LiveData<List<ReportRental>> get() = _rentalReports

    private val _filteredRentalReports = MutableLiveData<List<ReportRental>>()
    val filteredRentalReports: LiveData<List<ReportRental>> get() = _filteredRentalReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<RentalTransactionData>?>()
    val createTransactionResponse: LiveData<DefaultRequest<RentalTransactionData>?> get() = _createTransactionResponse

    private val _printData = MutableLiveData<RentalPrintTransaction>()
    val printData: LiveData<RentalPrintTransaction> get() = _printData

    private val _invoiceRental = MutableLiveData<List<InvoiceRentalResponse>>()
    val invoiceRental: LiveData<List<InvoiceRentalResponse>> get() = _invoiceRental

    private val _filteredInvoices = MutableLiveData<List<InvoiceRentalResponse>>()
    val filteredInvoices: LiveData<List<InvoiceRentalResponse>> get() = _filteredInvoices

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private var branches: List<Branch> = emptyList()
    private var client: List<Client> = emptyList()
    private var users: List<User> = emptyList()

    fun init(context: Context) {
        if (isInitialized) return

        rentalReportRepository = RentalReportRepository(context)
        userRepository = UserRepository(context)
        branchRepository = BranchRepository(context)

        isInitialized = true

        // Load branches terlebih dahulu, baru load data
        loadBranchesAndData()
    }

    /**
     * Load branches dan data secara berurutan
     */
    private fun loadBranchesAndData() {
        viewModelScope.launch {
            try {
                // Load branches dulu
                val branchResponse = branchRepository.getBranch()
                if (branchResponse.success) {
                    branches = branchResponse.data
                    branchResponse.data.forEach { branch ->
                        branch.id_branch?.let { id ->
                            branchesMap[id] = branch
                        }
                    }
                    Log.d("RentalViewModel", "Branches loaded: ${branches.size}")
                }

                // Setelah branches loaded, baru load data rental dan invoice
                // Data akan di-load lewat getReportRental() yang dipanggil dari fragment
            } catch (e: Exception) {
                Log.e("RentalViewModel", "Failed to load branches: ${e.message}")
                _error.postValue("Gagal memuat data cabang: ${e.message}")
            }
        }
    }

    /**
     * Konversi waktu UTC dari API ke timezone branch
     */
    private fun formatTimeForBranch(utcTime: String?, branchId: Int?): String {
        if (utcTime == null || branchId == null) return "-"

        val branch = branchesMap[branchId]
        val timezone = branch?.timezone_branch ?: "Asia/Jakarta"

        return try {
            TimezoneHelper.convertUtcToBranchTimezone(
                utcTimeString = utcTime,
                branchTimezone = timezone,
                outputFormat = "dd MMM yyyy, HH:mm"
            )
        } catch (e: Exception) {
            Log.e("RentalViewModel", "Error formatting time: ${e.message}")
            utcTime
        }
    }

    /**
     * Transform data rental dengan konversi timezone
     */
    private fun transformReportRental(report: ReportRental): ReportRental {
        val formattedTime = formatTimeForBranch(
            report.time_transaction_rental,
            report.id_branch_transaction_rental
        )

        return try {
            // Coba set langsung jika property mutable
            report.formatted_time_transaction_rental = formattedTime
            report
        } catch (e: Exception) {
            Log.e("RentalViewModel", "Error setting formatted time: ${e.message}")
            // Fallback: return report tanpa formatted time
            report
        }
    }

    /**
     * Get reports filtered by user's branch
     */
    suspend fun getReportRental() {
        _loading.postValue(true)
        _error.postValue("")
        try {
            Log.d("RentalViewModel", "Getting rental reports...")

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

            val branchId = userResponse.data.id_branch_user
            Log.d("RentalViewModel", "User branch ID: $branchId")

            val response = rentalReportRepository.getReportRental()
            Log.d("RentalViewModel", "API Response success: ${response.success}, data size: ${response.data?.size ?: 0}")

            if (response.success) {
                val filteredList = response.data
                    .filter { it.id_branch_transaction_rental == branchId }
                    .map { transformReportRental(it) }

                Log.d("RentalViewModel", "Filtered rental reports: ${filteredList.size}")

                _rentalReports.postValue(filteredList)
                _filteredRentalReports.postValue(filteredList)
            } else {
                _error.postValue("Gagal mengambil transaksi rental: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("RentalViewModel", "Error getting rental reports", e)
            _error.postValue(e.message ?: "Terjadi kesalahan")
        } finally {
            _loading.postValue(false)
        }
    }

    /**
     * Fetch all reports (refresh)
     */
    fun fetchReportRental() {
        _loading.postValue(true)
        _error.postValue("")
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.getReportRental()
                if (response.success && response.data != null) {
                    val transformedData = response.data.map { transformReportRental(it) }
                    _rentalReports.postValue(transformedData)
                    _filteredRentalReports.postValue(transformedData)
                } else {
                    _error.postValue("Gagal mengambil laporan rental: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    /**
     * Get all invoice rental
     */
    private fun getAllInvoiceRental() {
        _loading.postValue(true)
        _error.postValue("")
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.getInvoiceRental()
                if (response.success && response.data != null) {
                    _invoiceRental.postValue(response.data)
                    _filteredInvoices.postValue(response.data)
                } else {
                    _error.postValue("Gagal memuat invoice rental: ${response.message ?: "Pesan tidak tersedia"}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat memuat invoice rental")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    /**
     * Get print data dengan timezone conversion
     */
    suspend fun getRentalPrint(id: Int): DefaultRequestPrint<RentalPrintTransaction> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            val response = rentalReportRepository.getRentalPrint(id)
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

    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        _loading.postValue(true)
        _error.postValue("")
        return try {
            val response = rentalReportRepository.getReportRentalById(id)

            if (response.success && response.data != null) {
                val transformedData = transformReportRental(response.data)
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
        } as DefaultRequest<ReportRental>
    }

    fun createRentalTransaction(rentalTransactionRequest: RentalTransactionRequest) {
        _loading.postValue(true)
        _error.postValue("")
        _createTransactionResponse.postValue(null)
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.createReportRental(rentalTransactionRequest)
                _createTransactionResponse.postValue(response)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat membuat transaksi")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    suspend fun exportRentalMonthly(exportReport: ExportReportRental): DefaultRequest<ReportRentalResponse> {
        _loading.postValue(true)
        _error.postValue("")
        return try {
            rentalReportRepository.exportRentalMonthly(exportReport)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengekspor")
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<ReportRentalResponse>
    }

    suspend fun createInvoiceRental(postInvoiceReportRental: PostInvoiceRentalRequest): DefaultRequestInvoice<InvoiceRentalResponse> {
        _loading.postValue(true)
        _error.postValue("")
        return try {
            val response = rentalReportRepository.createInvoiceRental(postInvoiceReportRental)
            if (response.success) {
                getAllInvoiceRental()
                response
            } else {
                _error.postValue("Gagal membuat invoice: ${response.message ?: "Pesan tidak tersedia"}")
                DefaultRequestInvoice(success = false, message = response.message, data = null)
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Gagal membuat invoice")
            DefaultRequestInvoice(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequestInvoice<InvoiceRentalResponse>
    }

    suspend fun exportInvoiceRental(exportInvoiceRental: ExportInvoicePdfRentalRequest): DefaultRequestInvoice<ReportRentalResponse> {
        _loading.postValue(true)
        _error.postValue("")
        return try {
            rentalReportRepository.exportInvoiceRental(exportInvoiceRental)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengekspor invoice")
            DefaultRequestInvoice(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequestInvoice<ReportRentalResponse>
    }

    fun fetchInvoiceRental() {
        getAllInvoiceRental()
    }

    /**
     * Filter by client and month dengan timezone consideration
     */
    fun filterByClientAndMonth(clientId: Int?, monthYear: String?) {
        val allReports = rentalReports.value ?: return

        val filteredReports = allReports.filter { report ->
            val matchClient = report.id_client_transaction_rental == clientId

            val matchMonth = if (monthYear != null && monthYear.isNotEmpty()) {
                val reportMonthYear = TimezoneHelper.getMonthYearFromUtc(
                    report.time_transaction_rental,
                    branchesMap[report.id_branch_transaction_rental]?.timezone_branch
                )
                reportMonthYear == monthYear
            } else {
                true
            }

            matchClient && matchMonth
        }

        _filteredRentalReports.value = filteredReports
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

    fun searchRentalReports(query: String) {
        val allRentalReports = _rentalReports.value ?: return
        if (query.isBlank()) {
            _filteredRentalReports.value = allRentalReports
        } else {
            _filteredRentalReports.value = allRentalReports.filter { report ->
                val branchName = branches.find {
                    it.id_branch == report.id_branch_transaction_rental
                }?.name_branch ?: ""
                val userName = users.find {
                    it.id_user == report.id_kurir_transaction_rental
                }?.username ?: ""

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

    fun setClient(data: List<Client>) {
        client = data
    }

    fun setUsers(data: List<User>) {
        users = data
    }

    fun postPrintData(data: RentalPrintTransaction) {
        _printData.postValue(data)
    }

    // Clear/Reset methods
    fun resetCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }
}