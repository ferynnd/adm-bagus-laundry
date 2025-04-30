package dev.ferynnd.baguslaundry.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import dev.ferynnd.baguslaundry.data.helper.NetworkConnectionLiveData

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    val isConnected: LiveData<Boolean> = NetworkConnectionLiveData(application.applicationContext)
}
