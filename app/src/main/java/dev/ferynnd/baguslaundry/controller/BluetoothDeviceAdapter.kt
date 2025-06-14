package dev.ferynnd.baguslaundry.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.CardBluetoothLayoutBinding
import dev.ferynnd.baguslaundry.model.BluetoothDeviceItem

class BluetoothDeviceAdapter(
    private val listener: OnDeviceClickListener
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<BluetoothDeviceItem>()

    interface OnDeviceClickListener {
        fun onDeviceClick(device: BluetoothDeviceItem)
    }

    // Update the list of devices
    fun updateDevices(newDevices: List<BluetoothDeviceItem>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }

    // Add a single device (useful for scanning)
    fun addDevice(device: BluetoothDeviceItem) {
        if (!devices.any { it.address == device.address }) { // Prevent duplicates
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        } else {
            // Optionally update existing device if properties change (e.g., name discovered)
            val index = devices.indexOfFirst { it.address == device.address }
            if (index != -1) {
                devices[index] = device // Replace with updated info
                notifyItemChanged(index)
            }
        }
    }

    // Update a specific device's status
    fun updateDeviceStatus(address: String, isPaired: Boolean, isConnecting: Boolean, isConnected: Boolean) {
        val index = devices.indexOfFirst { it.address == address }
        if (index != -1) {
            devices[index].isPaired = isPaired
            devices[index].isConnecting = isConnecting
            devices[index].isConnected = isConnected
            notifyItemChanged(index)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = CardBluetoothLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device, listener)
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(private val binding: CardBluetoothLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BluetoothDeviceItem, listener: OnDeviceClickListener) {
            binding.deviceName.text = device.name ?: "Unknown Device"
            binding.deviceAddress.text = device.address

            // Update device status text and visibility based on flags
            when {
                device.isConnected -> {
                    binding.deviceStatus.text = "Connected"
                    binding.deviceStatus.visibility = View.VISIBLE
                    binding.pairingStatusIcon.setImageResource(R.drawable.radiobtn) // You'll need this drawable
                    // You might want a different tint for connected status
                    binding.pairingStatusIcon.setColorFilter(binding.root.context.getColor(R.color.blue600))
                }
                device.isConnecting -> {
                    binding.deviceStatus.text = "Connecting..."
                    binding.deviceStatus.visibility = View.VISIBLE
                    binding.pairingStatusIcon.setImageResource(R.drawable.radiobtn)
                    binding.pairingStatusIcon.setColorFilter(binding.root.context.getColor(R.color.blueBase)) // Indicate pending
                }
                device.isPaired -> {
                    binding.deviceStatus.text = "Paired"
                    binding.deviceStatus.visibility = View.VISIBLE
                    binding.pairingStatusIcon.setImageResource(R.drawable.radiobtn)
                    binding.pairingStatusIcon.setColorFilter(binding.root.context.getColor(R.color.blue200))
                }
                else -> {
                    binding.deviceStatus.text = "Tap to pair"
                    binding.deviceStatus.visibility = View.VISIBLE
                    binding.pairingStatusIcon.setImageResource(R.drawable.radiobtn)
                    binding.pairingStatusIcon.setColorFilter(binding.root.context.getColor(R.color.red))
                }
            }

            // Optional: Change device icon based on device type (e.g., headphones, speaker)
            // binding.deviceIcon.setImageResource(R.drawable.bluetooth) // Default

            binding.root.setOnClickListener {
                listener.onDeviceClick(device)
            }
        }
    }
}