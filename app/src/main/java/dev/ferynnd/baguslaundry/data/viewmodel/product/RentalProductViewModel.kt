package dev.ferynnd.baguslaundry.data.viewmodel.product

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.product.RentalProductRepository
import dev.ferynnd.baguslaundry.model.ProductRental
import kotlinx.coroutines.launch

class RentalProductViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var rentalProductRepository: RentalProductRepository

    private val _rentalProducts = MutableLiveData<List<ProductRental>>()
    val rentalProducts: LiveData<List<ProductRental>> get() = _rentalProducts

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>() // Tidak lagi nullable
    val error: LiveData<String> = _error

    private val _filteredRentalProducts = MutableLiveData<List<ProductRental>>()  // hasil pencarian
    val filteredRentalProducts: LiveData<List<ProductRental>> get() = _filteredRentalProducts

    fun init(context: Context) {
        rentalProductRepository = RentalProductRepository(context)
        getAllProductRental()
    }

    // Fungsi publik untuk mereset pesan error
    fun resetErrorMessage() {
        _error.postValue("") // Gunakan postValue untuk memastikan pembaruan terjadi di main thread
    }

    private fun getAllProductRental() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error
        viewModelScope.launch {
            try {
                val response = rentalProductRepository.getProductRental()
                if (response.success) {
                    _rentalProducts.postValue(response.data)
                    _filteredRentalProducts.postValue(response.data) // Initialize filtered with all data
                } else {
                    _error.postValue(response.message ?: "Failed to load initial rental items.")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "An error occurred while loading initial rental items.")
            } finally {
                _loading.postValue(false) // Set loading to false
            }
        }
    }

    suspend fun getProductRental() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error
        try {
            val response = rentalProductRepository.getProductRental()
            if (response.success) {
                _rentalProducts.postValue(response.data)
                _filteredRentalProducts.postValue(response.data) // Update filtered list as well
            } else {
                _error.postValue(response.message ?: "Failed to get rental items.")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "An error occurred while fetching rental items.")
        } finally {
            _loading.postValue(false) // Set loading to false
        }
    }

    suspend fun getProductRentalById(id: Int): DefaultRequest<ProductRental> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error
        return try {
            rentalProductRepository.getProductRentalById(id)
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil item rental berdasarkan ID.")
            // Penting: Pastikan ini mengembalikan objek DefaultRequest yang valid, bukan null
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Set loading to false
        } as DefaultRequest<ProductRental>
    }

    fun filterClient(branchId: Int?) {
        // No loading indicator here as it's a local filter operation
        val allRentalProducts = _rentalProducts.value ?: return
        _filteredRentalProducts.value = if (branchId == null) {
            allRentalProducts
        } else {
            allRentalProducts.filter { it.id_branch_rental_item == branchId }
        }
    }

    fun searchRentalProducts(query: String) {
        // No loading indicator here as it's a local search operation
        val allRentalProducts = _rentalProducts.value ?: return
        if (query.isBlank()) {
            _filteredRentalProducts.value = allRentalProducts
        } else {
            _filteredRentalProducts.value = allRentalProducts.filter {
                it.name_rental_item?.contains(query, ignoreCase = true) == true
            }
        }
    }
}