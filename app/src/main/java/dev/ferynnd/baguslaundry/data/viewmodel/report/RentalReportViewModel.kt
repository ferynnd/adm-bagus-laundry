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
import dev.ferynnd.baguslaundry.data.repository.report.RentalReportRepository
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.LaundryTransactionResponse
import dev.ferynnd.baguslaundry.model.RentalTransactionResponse
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.RentalTransactionRequest
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ExportInvoicePdfRentalRequest
import dev.ferynnd.baguslaundry.model.ExportReportRental
import dev.ferynnd.baguslaundry.model.InvoiceRentalResponse
import dev.ferynnd.baguslaundry.model.PostInvoiceRentalRequest
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.ReportRentalResponse
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.launch

class RentalReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var rentalReportRepository: RentalReportRepository

    private val _rentalReports = MutableLiveData<List<ReportRental>>()
    val rentalReports: LiveData<List<ReportRental>> get() = _rentalReports

    private val _createTransactionResponse = MutableLiveData<DefaultRequest<RentalTransactionResponse>?>()
    val createTransactionResponse: LiveData<DefaultRequest<RentalTransactionResponse>?> get() = _createTransactionResponse

    private val _invoiceRental = MutableLiveData<List<InvoiceRentalResponse>>()
    val invoiceRental: LiveData<List<InvoiceRentalResponse>> get() = _invoiceRental


    fun init(context: Context) {
        rentalReportRepository = RentalReportRepository(context)
        getAllReportRental()
        getAllInvoiceRental()
    }

    private fun getAllReportRental() {
        viewModelScope.launch {
            _rentalReports.postValue(rentalReportRepository.getReportRental().data)
        }
    }

    private fun getAllInvoiceRental() {
        viewModelScope.launch {
            _invoiceRental.postValue(rentalReportRepository.getInvoiceRental().data)
            Log.d("InvoiceRental", "Invoice Rental: ${_invoiceRental.value}")
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

    suspend fun getReportRentalById(id: Int): DefaultRequest<ReportRental> {
        return rentalReportRepository.getReportRentalById(id)
    }


    fun createRentalTransaction(rentalTransactionRequest: RentalTransactionRequest) {
        viewModelScope.launch {
            try {
                val response = rentalReportRepository.createReportRental(rentalTransactionRequest)
                _createTransactionResponse.value = response
                if (!response.success) {
                    Log.e("API_ERROR", "Error: ${response.errors}")
                }
            } catch (e: Exception) {
                Log.e("RentalReportViewModel", "Error creating rental transaction", e)
            }
        }
    }
    
    fun resetCreateTransactionResponse() {
        _createTransactionResponse.value = null
    }

    suspend fun exportRentalMonthly(exportReport: ExportReportRental): DefaultRequest<ReportRentalResponse> {
        return rentalReportRepository.exportRentalMonthly(exportReport)
    }

    suspend fun createInvoiceRental(postInvoiceReportRental: PostInvoiceRentalRequest): DefaultRequestInvoice<InvoiceRentalResponse> {
        try {

            val response = rentalReportRepository.createInvoiceRental(postInvoiceReportRental)

            if (response.success) {
                val invoiceData = response.data
                return response
            } else {
                throw Exception("Error: ${response.message}")
            }
        } catch (e: Exception) {
            throw Exception("Request failed: ${e.message}")
        }
    }

    suspend fun exportInvoiceRental(exportInvoiceRental: ExportInvoicePdfRentalRequest): DefaultRequestInvoice<ReportRentalResponse> {
        return rentalReportRepository.exportInvoiceRental(exportInvoiceRental)

    }

    suspend fun getInvoiceRental() {
        try {
            val response = rentalReportRepository.getInvoiceRental()
            if (response.success) {
                val data = response.data
                _invoiceRental.postValue(data) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }

    private val _filteredInvoices =
        MutableLiveData<List<InvoiceRentalResponse>>()  // hasil pencarian
    val filteredInvoices: LiveData<List<InvoiceRentalResponse>> get() = _filteredInvoices


    fun filterClientInvoice(branchId: Int?) {
        val allUsers = _invoiceRental.value ?: return
        _filteredInvoices.value = if (branchId == null) {
            allUsers
        } else {
            allUsers.filter { it.id_branch_invoice == branchId }
        }
    }


    private val _filteredRentalReports = MutableLiveData<List<ReportRental>>()  // hasil pencarian
    val filteredRentalReports: LiveData<List<ReportRental>> get() = _filteredRentalReports


    fun filterClient(branchId: Int?) {
        val allRentalReports = _rentalReports.value ?: return
        _filteredRentalReports.value = if (branchId == null) {
            allRentalReports
        } else {
            allRentalReports.filter { it.id_branch_transaction_rental == branchId }
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