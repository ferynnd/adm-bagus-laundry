package dev.ferynnd.admbaguslaundry.data.viewmodel.product

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.admbaguslaundry.data.api.DefaultRequest
import dev.ferynnd.admbaguslaundry.data.api.Pagination
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.data.repository.UserRepository
import dev.ferynnd.admbaguslaundry.data.repository.product.RentalProductRepository
import dev.ferynnd.admbaguslaundry.model.ProductRental
import kotlinx.coroutines.launch

class RentalProductViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var rentalProductRepository: RentalProductRepository

    private val sharedPreferences = SharePrefrenceHelper(application)

    private lateinit var userRepository: UserRepository

    private val _rentalProducts = MutableLiveData<List<ProductRental>>()
    val rentalProducts: LiveData<List<ProductRental>> get() = _rentalProducts

    private val _filteredRentalProducts = MutableLiveData<List<ProductRental>>()
    val filteredRentalProducts: LiveData<List<ProductRental>> get() = _filteredRentalProducts

    private val _pagination = MutableLiveData<Pagination>()
    val pagination: LiveData<Pagination> get() = _pagination

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentQuery: String = ""
    private var currentBranchId: Int? = null

    private var isDataLoaded = false

    fun init(context: Context) {
        rentalProductRepository = RentalProductRepository(context)
        userRepository = UserRepository(context)
        getProductRentalPage(1)
    }

    fun getProductRentalPage(page: Int = 1) {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue("")

            try {
                val response = rentalProductRepository.getProductRental(page)

                if (response.success) {
                    val items = response.data?.items ?: emptyList()

                    _rentalProducts.postValue(items)
                    _pagination.postValue(response.data?.pagination)

                    applyCurrentFilters(items)

                    isDataLoaded = true
                } else {
                    _error.postValue(response.message ?: "Failed to load rental items.")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "An error occurred while loading rental items."
                )
            } finally {
                _loading.postValue(false)
            }
        }
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
        val all = _rentalProducts.value ?: return
        applyCurrentFilters(all)
    }

    private fun applyCurrentFilters(all: List<ProductRental>) {
        val filtered = all.filter { item ->
            val matchBranch = currentBranchId?.let {
                item.id_branch_rental_item == it
            } ?: true

            val matchQuery = if (currentQuery.isBlank()) {
                true
            } else {
                item.name_rental_item?.contains(currentQuery, ignoreCase = true) == true
            }

            matchBranch && matchQuery
        }

        _filteredRentalProducts.postValue(filtered)
    }

    fun resetErrorMessage() {
        _error.postValue("")
    }

    suspend fun getProductRental(forceRefresh: Boolean = false) {
        if (isDataLoaded && !forceRefresh) {
            applyFilters()
            return
        }

        _loading.postValue(true)
        _error.postValue("")

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

            val response = rentalProductRepository.getProductRental(1)
            if (response.success) {
                val rentalProducts = response.data?.items ?: emptyList()

                val filteredList = rentalProducts.filter {
                    it.id_branch_rental_item == branchId
                }

                _rentalProducts.postValue(filteredList)
                _filteredRentalProducts.postValue(filteredList)
                _pagination.postValue(response.data?.pagination)

                isDataLoaded = true
            } else {
                _error.postValue(response.message ?: "Failed to get rental items.")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "An error occurred while fetching rental items.")
        } finally {
            _loading.postValue(false)
        }
    }

    suspend fun getProductRentalById(id: Int): DefaultRequest<ProductRental> {
        _loading.postValue(true)
        _error.postValue("")

        return try {
            rentalProductRepository.getProductRentalById(id)
        } catch (e: Exception) {
            _error.postValue(
                e.message ?: "Terjadi kesalahan saat mengambil item rental berdasarkan ID."
            )
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false)
        } as DefaultRequest<ProductRental>
    }

    fun filterClient(branchId: Int?) {
        currentBranchId = branchId
        applyFilters()
    }

    fun searchRentalProducts(query: String) {
        currentQuery = query
        applyFilters()
    }

    suspend fun createProductRental(client: ProductRental) {
        rentalProductRepository.createProductRental(client)
    }

    suspend fun updateProductRental(client: ProductRental) {
        client.id_rental_item?.let {
            rentalProductRepository.updateProductRental(it, client)
        }
    }

    suspend fun deleteProductRental(client: ProductRental) {
        client.id_rental_item?.let {
            rentalProductRepository.deleteProductRental(it)
        }
    }
}