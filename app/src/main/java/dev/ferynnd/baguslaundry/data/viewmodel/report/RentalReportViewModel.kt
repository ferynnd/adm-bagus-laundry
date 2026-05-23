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
import dev.ferynnd.admbaguslaundry.data.api.*
import dev.ferynnd.admbaguslaundry.data.repository.report.RentalReportRepository
import dev.ferynnd.admbaguslaundry.model.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class RentalReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var rentalReportRepository: RentalReportRepository
    private var isInitialized = false

    private val _rentalReports = MutableLiveData<List<ReportRental>>()
    val rentalReports: LiveData<List<ReportRental>> get() = _rentalReports

    private val _filteredRentalReports = MutableLiveData<List<ReportRental>>()
    val filteredRentalReports: LiveData<List<ReportRental>> get() = _filteredRentalReports

    private val _pagination = MutableLiveData<Pagination>()
    val pagination: LiveData<Pagination> get() = _pagination

    private val _updateTransactionResponse = MutableLiveData<DefaultRequest<ReportRental>?>()
    val updateTransactionResponse: LiveData<DefaultRequest<ReportRental>?> get() = _updateTransactionResponse

    private val _deleteTransactionResponse = MutableLiveData<DefaultRequest<ReportRental>?>()
    val deleteTransactionResponse: LiveData<DefaultRequest<ReportRental>?> get() = _deleteTransactionResponse

    private val _invoiceRental = MutableLiveData<List<InvoiceRentalResponse>>()
    val invoiceRental: LiveData<List<InvoiceRentalResponse>> get() = _invoiceRental

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private var branches: List<Branch> = emptyList()
    private var clients: List<Client> = emptyList()
    private var users: List<User> = emptyList()

    fun init(context: Context) {
        if (isInitialized) return
        rentalReportRepository = RentalReportRepository(context)
        isInitialized = true
        getInvoiceRental()
    }

    fun getReportRental(page: Int = 1) {
        _loading.postValue(true)
        _error.postValue("")

        viewModelScope.launch {
            try {
                val response = rentalReportRepository.getReportRental(page)

                if (response.success) {
                    val items = response.data?.items ?: emptyList()

                    _rentalReports.postValue(items)
                    _filteredRentalReports.postValue(items)
                    response.data?.pagination?.let {
                        _pagination.postValue(it)
                    }

                    Log.d("RentalViewModel", "Rental size: ${items.size}")
                    Log.d("RentalViewModel", "Pagination: ${response.data?.pagination}")
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
    }

    fun updateRentalTransaction(id: Int, updateRequest: UpdateRentalFullRequest) {
        _loading.postValue(true)
        _error.postValue("")
        _updateTransactionResponse.postValue(null)


        viewModelScope.launch {
            try {
                val response = rentalReportRepository.updateReportRental(id, updateRequest)

                _updateTransactionResponse.postValue(response)

                if (response.success) {
                    getReportRental()
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat mengupdate transaksi")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun deleteRentalTransaction(id: Int) {
        _loading.postValue(true)
        _error.postValue("")
        _deleteTransactionResponse.postValue(null)

        viewModelScope.launch {
            try {
                val response = rentalReportRepository.deleteReportRental(id)
                _deleteTransactionResponse.postValue(response)

                if (response.success) {
                    getReportRental()
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat menghapus transaksi")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun forceDeleteRentalTransaction(id: Int) {
        _loading.postValue(true)
        _error.postValue("")
        _deleteTransactionResponse.postValue(null)

        viewModelScope.launch {
            try {
                val response = rentalReportRepository.forceDeleteReportRental(id)
                _deleteTransactionResponse.postValue(response)

                if (response.success) {
                    getReportRental()
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat menghapus permanen transaksi")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            rentalReportRepository.getReportRentalById(id)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan")
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<ReportRental>
    }

    fun filterByClientAndMonth(clientId: Int?, monthYear: String?) {
        val allReports = _rentalReports.value ?: return

        val filteredReports = allReports.filter { report ->
            val matchClient = report.id_client_transaction_rental == clientId

            val matchMonth = if (!monthYear.isNullOrEmpty()) {
                report.time_transaction_rental?.startsWith(monthYear) == true
            } else {
                true
            }

            matchClient && matchMonth
        }

        _filteredRentalReports.value = filteredReports
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

        _filteredRentalReports.value = if (query.isBlank()) {
            allRentalReports
        } else {
            allRentalReports.filter { report ->
                val branchName = branches.find {
                    it.id_branch == report.id_branch_transaction_rental
                }?.name_branch ?: ""

                val userName = users.find {
                    it.id_user == report.id_kurir_transaction_rental
                }?.username ?: ""

                val clientName = clients.find {
                    it.id_client == report.id_client_transaction_rental
                }?.name_client ?: ""

                branchName.contains(query, true) ||
                        userName.contains(query, true) ||
                        clientName.contains(query, true) ||
                        report.id_transaction_rental.toString().contains(query, true) ||
                        report.number_transaction_rental.toString().contains(query, true)
            }
        }
    }

    fun setBranches(data: List<Branch>) {
        branches = data
    }

    fun setClient(data: List<Client>) {
        clients = data
    }

    fun setUsers(data: List<User>) {
        users = data
    }

    fun resetUpdateTransactionResponse() {
        _updateTransactionResponse.value = null
    }

    fun resetDeleteTransactionResponse() {
        _deleteTransactionResponse.value = null
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }

    suspend fun exportRentalMonthly(
        exportReport: ExportReportRental
    ): DefaultRequest<ReportRentalResponse> {
        return rentalReportRepository.exportRentalMonthly(exportReport)
    }

    private val _invoicePagination = MutableLiveData<Pagination>()
    val invoicePagination: LiveData<Pagination> get() = _invoicePagination

    fun getInvoiceRental(page: Int = 1) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val response = rentalReportRepository.getInvoiceRental(page)

                Log.d("INVOICE_DEBUG", "GET RESPONSE: $response")

                if (response.success) {
                    _invoiceRental.value = response.data.items ?: emptyList()
                    _invoicePagination.value = response.data.pagination
                } else {
                    _invoiceRental.value = emptyList()
                }

            } catch (e: Exception) {
                _invoiceRental.value = emptyList()
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun createInvoiceRental(
        request: PostInvoiceRentalRequest
    ): DefaultRequestInvoice<InvoiceRentalResponse> {
        return rentalReportRepository.createInvoiceRental(request)
    }

    suspend fun exportInvoiceRental(
        request: ExportInvoicePdfRentalRequest
    ): DefaultRequestInvoice<ReportRentalResponse> {
        return rentalReportRepository.exportInvoiceRental(request)
    }

    private val _printRentalData = MutableLiveData<RentalPrintTransaction?>()
    val printRentalData: LiveData<RentalPrintTransaction?> get() = _printRentalData

    suspend fun getRentalPrint(
        id: Int
    ): DefaultRequestPrint<RentalPrintTransaction> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            rentalReportRepository.getRentalPrint(id)
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Terjadi kesalahan"
            _error.postValue(errorMessage)

            DefaultRequestPrint(
                success = false,
                message = errorMessage,
                data = null
            )
        } finally {
            _loading.postValue(false)
        }
    }

    fun postPrintData(data: RentalPrintTransaction?) {
        _printRentalData.postValue(data)
    }
}