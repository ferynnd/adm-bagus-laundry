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

    fun init(context: Context) {
       rentalProductRepository = RentalProductRepository(context)
        getAllProductRental()
    }

     private fun getAllProductRental() {
        viewModelScope.launch {
            _rentalProducts.postValue(rentalProductRepository.getProductRental().data)
        }
    }


    suspend fun getProductRental() {
        try {
            val response = rentalProductRepository.getProductRental()
            if (response.success) {
                val client = response.data
                _rentalProducts.postValue(client) // Memperbarui LiveData dengan data baru
            } else {
                throw Exception("API request failed")
            }
        } catch (e: Exception) {
            throw e // Menangani error jika ada
        }
    }



    suspend fun getProductRentalById(id: Int): DefaultRequest<ProductRental> {
        return rentalProductRepository.getProductRentalById(id)
    }


}