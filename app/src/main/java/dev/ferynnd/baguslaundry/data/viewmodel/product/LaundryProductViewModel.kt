package dev.ferynnd.baguslaundry.data.viewmodel.product

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.repository.product.LaundryProductRepository
import dev.ferynnd.baguslaundry.model.ProductLaundry
import kotlinx.coroutines.launch

class LaundryProductViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var laundryProductRepository: LaundryProductRepository

    private val _laundryProducts = MutableLiveData<List<ProductLaundry>>()
    val laundryProducts: LiveData<List<ProductLaundry>> get() = _laundryProducts

    fun init(context: Context) {
        laundryProductRepository = LaundryProductRepository(context)
        getAllProductLaundry()
    }


    private fun getAllProductLaundry() {
        viewModelScope.launch {
            val result = laundryProductRepository.getProductLaundry().data
            _laundryProducts.postValue(result)
        }
    }


    suspend fun getProductLaundry() {
        try {
            val response = laundryProductRepository.getProductLaundry()
            if (response.success) {
                val productLaundry = response.data
                _laundryProducts.postValue(productLaundry) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }


    suspend fun getProductLaundryById(id: Int): DefaultRequest<ProductLaundry> {
        return laundryProductRepository.getProductLaundryById(id)
    }

    private val _filteredLaundryProducts =
        MutableLiveData<List<ProductLaundry>>()  // hasil pencarian
    val filteredLaundryProducts: LiveData<List<ProductLaundry>> get() = _filteredLaundryProducts


    fun filterClient(branchId: Int?) {
        val allLaundryProducts = _laundryProducts.value ?: return
        _filteredLaundryProducts.value = if (branchId == null) {
            allLaundryProducts
        } else {
            allLaundryProducts.filter { it.id_branch_laundry_item == branchId }
        }
    }

     fun searchLaundryProducts(query: String) {
        val allLaundryProducts = _laundryProducts.value ?: return
        if (query.isBlank()) {
            _filteredLaundryProducts.value = allLaundryProducts
        } else {
            _filteredLaundryProducts.value = allLaundryProducts.filter {
                it.name_laundry_item?.contains(query, ignoreCase = true) == true
            }
        }
    }


}