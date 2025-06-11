package dev.ferynnd.baguslaundry.data.viewmodel.product

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.repository.UserRepository
import dev.ferynnd.baguslaundry.data.repository.product.LaundryProductRepository
import dev.ferynnd.baguslaundry.model.ProductLaundry
import kotlinx.coroutines.launch

class LaundryProductViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var laundryProductRepository: LaundryProductRepository


    private val sharedPreferences = SharePrefrenceHelper(application)

    private val userId: String
        get() = sharedPreferences.getString("PREF_USER_ID") ?: ""

    private lateinit var userRepository: UserRepository


    private val _laundryProducts = MutableLiveData<List<ProductLaundry>>()
    val laundryProducts: LiveData<List<ProductLaundry>> get() = _laundryProducts

    private val _filteredProductLaundry =
        MutableLiveData<List<ProductLaundry>>()  // hasil pencarian
    val filteredProductLaundry: LiveData<List<ProductLaundry>> get() = _filteredProductLaundry

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>() // Tidak lagi nullable
    val error: LiveData<String> = _error

    fun init(context: Context) {
        laundryProductRepository = LaundryProductRepository(context)
        userRepository = UserRepository(context)
        getAllProductLaundry()
    }

    // Fungsi publik untuk mereset pesan error
    fun resetErrorMessage() {
        _error.postValue("") // Gunakan postValue untuk memastikan pembaruan terjadi di main thread
    }

    private fun getAllProductLaundry() {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error
        viewModelScope.launch {
            try {
                val response = laundryProductRepository.getProductLaundry()
                if (response.success) {
                    _laundryProducts.postValue(response.data)
                    _filteredProductLaundry.postValue(response.data) // Initialize filtered list
                } else {
                    _error.postValue("Gagal memuat item laundry awal: ${response.message}")
                }
            } catch (e: Exception) {
                _error.postValue(
                    e.message ?: "Terjadi kesalahan saat memuat data item laundry awal."
                )
            } finally {
                _loading.postValue(false) // Always set loading to false
            }
        }
    }

    suspend fun getProductLaundry() {
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

            val branchId = userResponse.data.id_branch_user // Pastikan field ini sesuai model User kamu

            val response = laundryProductRepository.getProductLaundry()
            if (response.success) {
                val productLaundry = response.data

                val filteredList = productLaundry.filter { it.id_branch_laundry_item == branchId }

                _laundryProducts.postValue(filteredList)
                _filteredProductLaundry.postValue(filteredList)
            } else {
                _error.postValue("Permintaan API gagal saat mengambil item laundry: ${response.message}")
            }
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil item laundry.")
        } finally {
            _loading.postValue(false)
        }
    }


//    suspend fun getProductLaundry() {
//        _loading.postValue(true) // Set loading to true
//        _error.postValue("") // Reset error
//        try {
//            val response = laundryProductRepository.getProductLaundry()
//            if (response.success) {
//                val productLaundry = response.data
//                _laundryProducts.postValue(productLaundry) // Memperbarui LiveData dengan data baru
//                _filteredProductLaundry.postValue(productLaundry) // Also update filtered list
//            } else {
//                _error.postValue("Permintaan API gagal saat mengambil item laundry: ${response.message}")
//            }
//        } catch (e: Exception) {
//            _error.postValue(e.message ?: "Terjadi kesalahan saat mengambil item laundry.")
//        } finally {
//            _loading.postValue(false) // Always set loading to false
//        }
//    }

    suspend fun getProductLaundryById(id: Int): DefaultRequest<ProductLaundry> {
        _loading.postValue(true) // Set loading to true
        _error.postValue("") // Reset error
        return try {
            laundryProductRepository.getProductLaundryById(id)
        } catch (e: Exception) {
            _error.postValue(
                e.message ?: "Terjadi kesalahan saat mengambil item laundry berdasarkan ID."
            )
            // Penting: Pastikan ini mengembalikan objek DefaultRequest yang valid, bukan null
            DefaultRequest(success = false, message = e.message.toString(), data = null)
        } finally {
            _loading.postValue(false) // Always set loading to false
        } as DefaultRequest<ProductLaundry>
    }

    fun filterClient(branchId: Int?) {
        // No loading indicator here as it's a local filter operation
        val allLaundryProducts = _laundryProducts.value ?: return
        _filteredProductLaundry.value = if (branchId == null) {
            allLaundryProducts
        } else {
            allLaundryProducts.filter { it.id_branch_laundry_item == branchId }
        }
    }

    fun searchLaundryProducts(query: String) {
        // No loading indicator here as it's a local search operation
        val allLaundryProducts = _laundryProducts.value ?: return
        if (query.isBlank()) {
            _filteredProductLaundry.value = allLaundryProducts
        } else {
            _filteredProductLaundry.value = allLaundryProducts.filter {
                it.name_laundry_item?.contains(query, ignoreCase = true) == true
            }
        }
    }

    // MutableLiveData untuk item yang terpilih
    private val _selectedItems = MutableLiveData<List<ProductLaundry>>()
    val selectedItems: LiveData<List<ProductLaundry>> get() = _selectedItems

    // Menyimpan daftar item terpilih
    fun setSelectedItems(items: List<ProductLaundry>) {
        _selectedItems.postValue(items)
    }
//
//    fun toggleItemSelection(item: ProductLaundry) {
//        val currentList = _selectedItems.value?.toMutableList() ?: mutableListOf()
//        if (currentList.any { it.id_laundry_item == item.id_laundry_item }) {
//            currentList.removeAll { it.id_laundry_item == item.id_laundry_item }
//        } else {
//            currentList.add(item)
//        }
//        _selectedItems.postValue(currentList)
//    }

    fun toggleItemSelection(item: ProductLaundry) {
        item.isSelected = !(item.isSelected ?: false)

        val updatedList = _laundryProducts.value?.toMutableList() ?: return
        val index = updatedList.indexOfFirst { it.id_laundry_item == item.id_laundry_item }
        if (index != -1) {
            updatedList[index] = item
            _laundryProducts.value = updatedList
        }

        val selected = updatedList.filter { it.isSelected == true }
        _selectedItems.value = selected
    }


//    fun toggleItemSelection(item: ProductLaundry) {
//    val currentList = _selectedItems.value?.toMutableList() ?: mutableListOf()
//
//    if (currentList.any { it.id_laundry_item == item.id_laundry_item }) {
//        // Batalkan seleksi
//        currentList.removeAll { it.id_laundry_item == item.id_laundry_item }
//        item.isSelected = false // ✅ Update state di objek
//    } else {
//        // Seleksi baru
//        item.isSelected = true // ✅ Update state di objek
//        currentList.add(item)
//    }
//
//    _selectedItems.postValue(currentList)
//}


    fun clearSelectedItems() {
        _selectedItems.postValue(emptyList())
    }
}