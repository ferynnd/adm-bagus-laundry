package dev.ferynnd.baguslaundry.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.google.android.material.snackbar.Snackbar
import dev.ferynnd.baguslaundry.controller.BluetoothDeviceAdapter
import dev.ferynnd.baguslaundry.databinding.FragmentBluetoothPairingBinding
import dev.ferynnd.baguslaundry.model.BluetoothDeviceItem
import java.util.UUID


class BluetoothPairingFragment : Fragment(), BluetoothDeviceAdapter.OnDeviceClickListener {

    private var _binding: FragmentBluetoothPairingBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceAdapter: BluetoothDeviceAdapter

    private val foundDevices = mutableListOf<BluetoothDeviceItem>()
    private val pairedDevices = mutableListOf<BluetoothDeviceItem>()

    // Request permissions for Bluetooth scanning (Android 12+ requires BLUETOOTH_SCAN)
    private val requestBluetoothPermissions =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Log.d("BluetoothFragment", "Bluetooth permissions granted.")
            startBluetoothScan()
        } else {
            Snackbar.make(
                binding.root,
                "Bluetooth permissions are required to scan for devices.",
                Snackbar.LENGTH_LONG
            ).show()
            Log.w("BluetoothFragment", "Bluetooth permissions denied.")
            showScanningStatus(false, "Permissions denied.")
        }
    }

    // BroadcastReceiver to listen for Bluetooth events
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    device?.let  {
                        val deviceItem = BluetoothDeviceItem(it.name, it.address, false, false, false)
                        if (!foundDevices.any { existing -> existing.address == it.address }) {
                            foundDevices.add(deviceItem)
                            deviceAdapter.addDevice(deviceItem)
                            Log.d("BluetoothFragment", "Found device: ${it.name} (${it.address})")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("BluetoothFragment", "Discovery started.")
                    showScanningStatus(true, "Searching for devices...")
                    foundDevices.clear() // Clear previous scan results
                    deviceAdapter.updateDevices(emptyList()) // Clear adapter
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BluetoothFragment", "Discovery finished.")
                    showScanningStatus(false, "Scan complete. Found ${foundDevices.size} devices.")
                    // After discovery, combine paired and found devices
                    displayCombinedDevices()
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    device?.let {
                        val deviceAddress = it.address
                        when (bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                Log.d("BluetoothFragment", "Device ${it.name} bonded (paired).")
                                Snackbar.make(binding.root, "Paired with ${it.name}", Snackbar.LENGTH_SHORT).show()
                                updateDeviceStatus(deviceAddress, isPaired = true, isConnecting = false, isConnected = false)
                                refreshPairedDevices() // Refresh list to show it as paired
                            }
                            BluetoothDevice.BOND_BONDING -> {
                                Log.d("BluetoothFragment", "Device ${it.name} bonding (pairing in progress).")
                                updateDeviceStatus(deviceAddress, isPaired = false, isConnecting = true, isConnected = false)
                            }
                            BluetoothDevice.BOND_NONE -> {
                                Log.d("BluetoothFragment", "Device ${it.name} bond removed (unpaired).")
                                updateDeviceStatus(deviceAddress, isPaired = false, isConnecting = false, isConnected = false)
                                refreshPairedDevices() // Refresh list if unpaired
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothPairingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize BluetoothAdapter
        val bluetoothManager = ContextCompat.getSystemService(requireContext(), BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
            ?: run {
                Snackbar.make(binding.root, "Bluetooth is not supported on this device.", Snackbar.LENGTH_LONG).show()
                binding.scanButton.isEnabled = false
                return
            }

        // Setup Toolbar
        binding.toolbar.apply {
            title = "Bluetooth Pairing"
            // You can add navigation icon or menu items here if needed
            // setNavigationIcon(R.drawable.ic_back)
            // setNavigationOnClickListener { findNavController().popBackStack() }
        }

        // Setup RecyclerView
        deviceAdapter = BluetoothDeviceAdapter(this)
        binding.bluetoothRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = deviceAdapter
        }

        // Setup Scan Button
        binding.scanButton.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                checkBluetoothPermissionsAndScan()
            } else {
                Snackbar.make(binding.root, "Bluetooth is off. Please enable it.", Snackbar.LENGTH_LONG).show()
                // You might want to prompt the user to enable Bluetooth here
                // val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        // Initial setup
        refreshPairedDevices() // Show already paired devices
        showScanningStatus(false, "Tap 'Scan' to find devices.")
    }

    override fun onResume() {
        super.onResume()
        // Register BroadcastReceiver for Bluetooth events
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        requireContext().registerReceiver(bluetoothReceiver, filter)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onPause() {
        super.onPause()
        // Unregister BroadcastReceiver
        requireContext().unregisterReceiver(bluetoothReceiver)
        // Stop discovery if running to save battery
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Bluetooth Logic Functions ---

    private fun checkBluetoothPermissionsAndScan() {
        val permissionsToRequest = mutableListOf<String>()

        // For older Android versions (API 30 and below)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        // For Android 12 (API 31) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissions.launch(permissionsToRequest.toTypedArray())
        } else {
            startBluetoothScan()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startBluetoothScan() {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery() // Cancel any ongoing discovery
        }
        foundDevices.clear()
        deviceAdapter.updateDevices(emptyList()) // Clear adapter
        refreshPairedDevices() // Re-add paired devices before scanning for new ones

        val started = bluetoothAdapter.startDiscovery()
        if (started) {
            Log.d("BluetoothFragment", "Bluetooth discovery initiated.")
            showScanningStatus(true, "Searching for devices...")
        } else {
            Log.e("BluetoothFragment", "Failed to start Bluetooth discovery.")
            Snackbar.make(binding.root, "Failed to start scan.", Snackbar.LENGTH_SHORT).show()
            showScanningStatus(false, "Scan failed.")
        }
    }

    private fun refreshPairedDevices() {
        pairedDevices.clear()
        try {
            // Requires BLUETOOTH_CONNECT permission (Android 12+)
            val bondedDevices = bluetoothAdapter.bondedDevices
            bondedDevices?.forEach { device ->
                val deviceItem = BluetoothDeviceItem(device.name, device.address, isPaired = true)
                pairedDevices.add(deviceItem)
            }
        } catch (e: SecurityException) {
            Log.e("BluetoothFragment", "SecurityException: ${e.message}")
            Snackbar.make(binding.root, "Missing Bluetooth permissions to get paired devices.", Snackbar.LENGTH_LONG).show()
        }
        // For a single RecyclerView, display paired devices first, then new ones
        displayCombinedDevices()
    }

    private fun displayCombinedDevices() {
        val combinedList = mutableListOf<BluetoothDeviceItem>()
        // Add paired devices first
        combinedList.addAll(pairedDevices.distinctBy { it.address }) // Use distinctBy to avoid duplicates

        // Add found (non-paired) devices, ensuring they aren't already in paired list
        val newFoundDevices = foundDevices.filter { found ->
            !pairedDevices.any { paired -> paired.address == found.address }
        }
        combinedList.addAll(newFoundDevices.distinctBy { it.address })

        deviceAdapter.updateDevices(combinedList)
    }


    private fun showScanningStatus(isScanning: Boolean, statusText: String) {
        binding.progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE
        binding.statusTextView.text = statusText
        binding.scanButton.text = if (isScanning) "Scanning..." else "Scan for Devices"
        binding.scanButton.isEnabled = !isScanning // Disable button while scanning
    }

    private fun updateDeviceStatus(address: String, isPaired: Boolean, isConnecting: Boolean, isConnected: Boolean) {
        deviceAdapter.updateDeviceStatus(address, isPaired, isConnecting, isConnected)
        // Also update the internal lists
        foundDevices.find { it.address == address }?.apply {
            this.isPaired = isPaired
            this.isConnecting = isConnecting
            this.isConnected = isConnected
        }
        pairedDevices.find { it.address == address }?.apply {
            this.isPaired = isPaired
            this.isConnecting = isConnecting
            this.isConnected = isConnected
        }
    }

    // --- OnDeviceClickListener Implementation ---
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDeviceClick(device: BluetoothDeviceItem) {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery() // Stop discovery before trying to connect/pair
            showScanningStatus(false, "Discovery cancelled.")
        }

        try {
            val bluetoothDevice: BluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)

            if (device.isPaired) {
                Snackbar.make(binding.root, "Device already paired: ${device.name}", Snackbar.LENGTH_SHORT).show()
                // Optionally connect if already paired
                 connectToDevice(bluetoothDevice)
            } else {
                // Attempt to create bond (pair)
                Log.d("BluetoothFragment", "Attempting to pair with ${device.name} (${device.address})")
                Snackbar.make(binding.root, "Pairing with ${device.name}...", Snackbar.LENGTH_SHORT).show()
                deviceAdapter.updateDeviceStatus(device.address, false, true, false) // Set connecting status
                bluetoothDevice.createBond()
            }
        } catch (e: SecurityException) {
            Snackbar.make(binding.root, "Bluetooth permissions missing for pairing.", Snackbar.LENGTH_LONG).show()
            Log.e("BluetoothFragment", "SecurityException during pairing: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Snackbar.make(binding.root, "Invalid Bluetooth address.", Snackbar.LENGTH_LONG).show()
            Log.e("BluetoothFragment", "IllegalArgumentException for address: ${device.address}, ${e.message}")
        }
    }

    // You would implement connection logic here if needed, after pairing
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToDevice(device: BluetoothDevice) {
        // This is where you'd typically initiate an RFCOMM socket connection.
        // This is more complex and depends on the service you want to connect to.
        // For example:
         try {
             val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
             val socket = device.createRfcommSocketToServiceRecord(uuid)
             socket.connect() // This is a blocking call, do in a background thread
        //     // Handle successful connection
        //     Log.d("BluetoothFragment", "Connected to ${device.name}")
             Snackbar.make(binding.root, "Connected to ${device.name}", Snackbar.LENGTH_SHORT).show()
             updateDeviceStatus(device.address, isPaired = true, isConnecting = false, isConnected = true)
         } catch (e: Exception) {
        //     Log.e("BluetoothFragment", "Error connecting: ${e.message}")
             Snackbar.make(binding.root, "Failed to connect to ${device.name}", Snackbar.LENGTH_SHORT).show()
             updateDeviceStatus(device.address, isPaired = true, isConnecting = false, isConnected = false)
         }
    }
}