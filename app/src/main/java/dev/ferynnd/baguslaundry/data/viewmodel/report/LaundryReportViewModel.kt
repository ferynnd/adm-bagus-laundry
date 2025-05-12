package dev.ferynnd.baguslaundry.data.viewmodel.report

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.report.LaundryReportRepository
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ExportReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundryResponse
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.launch

class LaundryReportViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var laundryReportRepository: LaundryReportRepository

    private val _laundryReports = MutableLiveData<List<ReportLaundry>>()
    val laundryReports: LiveData<List<ReportLaundry>> get() = _laundryReports

    fun init(context: Context) {
        laundryReportRepository = LaundryReportRepository(context)
        getAllReportLaundry()
    }

    private fun getAllReportLaundry() {
        viewModelScope.launch {
            _laundryReports.postValue(laundryReportRepository.getReportLaundry().data)
        }
    }


    suspend fun getReportLaundry() {
        try {
            val response = laundryReportRepository.getReportLaundry()
            if (response.success) {
                val client = response.data
                _laundryReports.postValue(client) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }

    suspend fun getReportLaundryById(id: Int): DefaultRequest<ReportLaundry> {
        return laundryReportRepository.getReportLaundryById(id)
    }

    suspend fun exportLaundryMonthly(exportReportLaundry: ExportReportLaundry): DefaultRequest<ReportLaundryResponse> {
        return laundryReportRepository.exportLaundryMonthly(exportReportLaundry)
    }

    private val _filteredLaundryReports = MutableLiveData<List<ReportLaundry>>()  // hasil pencarian
    val filteredLaundryReports: LiveData<List<ReportLaundry>> get() = _filteredLaundryReports


    fun filterClient(branchId: Int?) {
        val allLaundryReports = _laundryReports.value ?: return
        _filteredLaundryReports.value = if (branchId == null) {
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