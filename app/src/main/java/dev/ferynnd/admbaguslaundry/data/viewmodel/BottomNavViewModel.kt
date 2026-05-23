package dev.ferynnd.admbaguslaundry.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BottomNavViewModel : ViewModel() {
    private val _isVisible = MutableLiveData(true)
    val isVisible: LiveData<Boolean> get() = _isVisible

    fun show() = _isVisible.postValue(true)
    fun hide() = _isVisible.postValue(false)
}