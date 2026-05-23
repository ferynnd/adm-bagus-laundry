package dev.ferynnd.admbaguslaundry.data.viewmodel.product

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.api.Pagination
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.data.repository.UserRepository
import dev.ferynnd.admbaguslaundry.data.repository.product.LaundryProductRepository
import dev.ferynnd.admbaguslaundry.model.LaundryTransactionState
import dev.ferynnd.admbaguslaundry.model.ProductLaundry
import kotlinx.coroutines.launch
import java.math.BigDecimal

class LaundryProductViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var laundryProductRepository: LaundryProductRepository

    private val sharedPreferences = SharePrefrenceHelper(application)

    private lateinit var userRepository: UserRepository

    private val _laundryProducts = MutableLiveData<List<ProductLaundry>>()
    val laundryProducts: LiveData<List<ProductLaundry>> get() = _laundryProducts

        private val _pagination = MutableLiveData<Pagination>()
    val pagination: LiveData<Pagination> get() = _pagination

    private val _filteredProductLaundry =
        MutableLiveData<List<ProductLaundry>>()
    val filteredProductLaundry: LiveData<List<ProductLaundry>> get() = _filteredProductLaundry

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentQuery: String = ""
    private var currentBranchId: Int? = null

    fun init(context: Context) {
        laundryProductRepository = LaundryProductRepository(context)
        userRepository = UserRepository(context)
        getProductLaundryPage(1)
    }

    fun setBranchFilter(branchId: Int?) {
        currentBranchId = branchId
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        val all = _laundryProducts.value ?: return

        val filtered = all.filter { item ->
            val matchBranch = currentBranchId?.let { item.id_branch_laundry_item == it } ?: true
            val matchQuery = if (currentQuery.isBlank()) true
            else item.name_laundry_item?.contains(currentQuery, true) == true
            matchBranch && matchQuery
        }

        _filteredProductLaundry.postValue(filtered)
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }

    private fun getAllProductLaundry() {
        _loading.postValue(true)
        _error.postValue("")
        viewModelScope.launch {
            try {
                val response = laundryProductRepository.getProductLaundry()
                if (response.success) {
                    val items = response.data?.items ?: emptyList()

                    _laundryProducts.postValue(items)
                    _filteredProductLaundry.postValue(items)
                } else {
                    _error.postValue("Gagal memuat item laundry awal: ${response.message}")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat memuat data item laundry awal."
                )
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun getProductLaundryPage(page: Int = 1) {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue("")

            try {
                val response = laundryProductRepository.getProductLaundry(page)

                if (response.success) {
                    val items = response.data?.items ?: emptyList()

                    _laundryProducts.postValue(items)
                    _filteredProductLaundry.postValue(items)
                    _pagination.postValue(response.data?.pagination)
                } else {
                    _error.postValue(response.message ?: "Gagal memuat item laundry.")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Terjadi kesalahan saat memuat item laundry.")
            } finally {
                _loading.postValue(false)
            }
        }
    }
    suspend fun getProductLaundryById(id: Int): DefaultRequest<ProductLaundry> {
        _loading.postValue(true)
        _error.postValue("")
        return try {
            laundryProductRepository.getProductLaundryById(id)
        } catch (e: Exception) {
            _error.postValue(
                e.message ?: "Terjadi kesalahan saat mengambil item laundry berdasarkan ID."
            )
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<ProductLaundry>
    }

    fun filterClient(branchId: Int?) {
        val allLaundryProducts = _laundryProducts.value ?: return
        _filteredProductLaundry.value = if (branchId == null) {
            allLaundryProducts
        } else {
            allLaundryProducts.filter { it.id_branch_laundry_item == branchId }
        }
    }

    fun searchLaundryProducts(query: String) {
        val allLaundryProducts = _laundryProducts.value ?: return
        if (query.isBlank()) {
            _filteredProductLaundry.value = allLaundryProducts
        } else {
            _filteredProductLaundry.value = allLaundryProducts.filter {
                it.name_laundry_item?.contains(query, ignoreCase = true) == true
            }
        }
    }

    suspend fun createProductLaundry(productLaundry: ProductLaundry) {
        laundryProductRepository.createProductLaundry(productLaundry)
    }

    suspend fun updateProductLaundry(productLaundry: ProductLaundry) {
        productLaundry.id_laundry_item?.let {
            laundryProductRepository.updateProductLaundry(it, productLaundry)
        }
    }

    suspend fun deleteProductLaundry(productLaundry: ProductLaundry) {
        productLaundry.id_laundry_item?.let {
            laundryProductRepository.deleteProductLaundry(it)
        }
    }
}
