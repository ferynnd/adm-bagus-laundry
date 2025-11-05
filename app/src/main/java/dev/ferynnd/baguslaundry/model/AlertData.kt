package dev.ferynnd.baguslaundry.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import dev.ferynnd.baguslaundry.R

// Event wrapper agar alert hanya terpanggil sekali
open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null else {
            hasBeenHandled = true
            content
        }
    }
    fun peekContent(): T = content
}

// Data class untuk alert
data class AlertData(
    val title: String,
    val message: String,
    val duration: Long = 3000,
    val backgroundColorRes: Int = R.color.primary,
    val iconRes: Int? = null
)
