package dev.ferynnd.baguslaundry.model


data class BluetoothDeviceItem(
    val name: String?,
    val address: String,
    var isPaired: Boolean = false,
    var isConnecting: Boolean = false,
    var isConnected: Boolean = false
)
