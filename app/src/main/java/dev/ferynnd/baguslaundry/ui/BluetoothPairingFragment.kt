package dev.ferynnd.baguslaundry.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.google.android.material.snackbar.Snackbar
import dev.ferynnd.baguslaundry.controller.BluetoothDeviceAdapter
import dev.ferynnd.baguslaundry.databinding.FragmentBluetoothPairingBinding
import dev.ferynnd.baguslaundry.model.BluetoothDeviceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Fragmen ini menangani penemuan, pemasangan, dan koneksi ke perangkat Bluetooth,
 * khususnya difilter untuk printer termal.
 *
 * Alur Kerja:
 * 1. Memeriksa dan meminta izin Bluetooth & Lokasi yang diperlukan.
 * 2. Memastikan Bluetooth dan layanan lokasi (jika perlu) diaktifkan.
 * 3. Menampilkan daftar perangkat yang sudah dipasangkan (paired).
 * 4. Memindai (scan) perangkat baru di sekitar.
 * 5. Memfilter hasil pemindaian untuk hanya menampilkan perangkat yang kemungkinan adalah printer.
 * 6. Mengizinkan pengguna untuk mengklik perangkat untuk memulai pemasangan (pairing) atau koneksi.
 * 7. Menangani status koneksi dan memberikan umpan balik kepada pengguna.
 */
class BluetoothPairingFragment : Fragment(), BluetoothDeviceAdapter.OnDeviceClickListener {

    private var _binding: FragmentBluetoothPairingBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceAdapter: BluetoothDeviceAdapter

    // Daftar terpisah untuk mengelola perangkat yang ditemukan dan yang sudah dipasangkan
    private val foundDevices = mutableListOf<BluetoothDeviceItem>()
    private val pairedDevices = mutableListOf<BluetoothDeviceItem>()

    // UUID standar untuk Serial Port Profile (SPP), umum digunakan oleh printer Bluetooth
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Launcher untuk meminta beberapa izin sekaligus (Bluetooth, Lokasi)
    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Cek jika semua izin yang diminta telah diberikan
            if (permissions.values.all { it }) {
                Log.d("BluetoothFragment", "Semua izin yang diperlukan telah diberikan.")
                // Lanjutkan ke proses pemindaian setelah izin diberikan
                checkPrerequisitesAndScan()
            } else {
                Log.w("BluetoothFragment", "Beberapa izin ditolak.")
                Snackbar.make(
                    binding.root,
                    "Izin Bluetooth dan Lokasi sangat penting. Harap izinkan di pengaturan aplikasi.",
                    Snackbar.LENGTH_LONG
                ).show()
                showScanningStatus(false, "Izin ditolak.")
            }
        }

    // Launcher untuk meminta pengguna mengaktifkan Bluetooth
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == -1) { // -1 adalah Activity.RESULT_OK
            Log.d("BluetoothFragment", "Bluetooth berhasil diaktifkan oleh pengguna.")
            checkPrerequisitesAndScan()
        } else {
            Log.w("BluetoothFragment", "Pengguna menolak untuk mengaktifkan Bluetooth.")
            Snackbar.make(
                binding.root,
                "Bluetooth harus diaktifkan untuk memindai perangkat.",
                Snackbar.LENGTH_LONG
            ).show()
            showScanningStatus(false, "Bluetooth nonaktif.")
        }
    }

    // BroadcastReceiver untuk menangkap event Bluetooth (penemuan perangkat, status pemasangan)
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission") // Izin sudah diperiksa sebelum mendaftarkan receiver
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                // Saat perangkat baru ditemukan
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    device?.let { handleFoundDevice(it) }
                }

                // Saat status pemasangan (bonding) berubah
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)

                    device?.let {
                        Log.d("BluetoothFragment", "Status pemasangan berubah untuk ${it.name}: ${getBondStateString(prevBondState)} -> ${getBondStateString(bondState)}")
                        handleBondStateChange(it, bondState)
                    }
                }

                // Saat pemindaian selesai
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BluetoothFragment", "Pemindaian selesai.")
                    showScanningStatus(false, "Pemindaian selesai. Pilih perangkat untuk terhubung.")
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

        // Inisialisasi BluetoothAdapter
        val bluetoothManager = ContextCompat.getSystemService(requireContext(), BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter ?: run {
            Snackbar.make(binding.root, "Bluetooth tidak didukung pada perangkat ini.", Snackbar.LENGTH_LONG).show()
            binding.scanButton.isEnabled = false
            return
        }

        binding.toolbar.title = "Pemasangan Printer Bluetooth"

        // Setup RecyclerView
        deviceAdapter = BluetoothDeviceAdapter(this)
        binding.bluetoothRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = deviceAdapter
        }

        binding.scanButton.setOnClickListener {
            checkPrerequisitesAndScan()
        }

        // Memuat perangkat yang sudah dipasangkan saat pertama kali fragment dibuat
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            refreshPairedDevices()
        }
        showScanningStatus(false, "Ketuk 'Pindai' untuk mencari printer.")
    }

    override fun onResume() {
        super.onResume()
        // Daftarkan BroadcastReceiver untuk mendengarkan event Bluetooth
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        requireContext().registerReceiver(bluetoothReceiver, filter)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onPause() {
        super.onPause()
        // Hentikan pemindaian jika sedang berjalan dan unregister receiver
        if (hasPermission(Manifest.permission.BLUETOOTH_SCAN) && bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        requireContext().unregisterReceiver(bluetoothReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Memeriksa semua prasyarat (izin, status Bluetooth, lokasi) sebelum memulai pemindaian.
     */
    private fun checkPrerequisitesAndScan() {
        // 1. Kumpulkan izin yang dibutuhkan berdasarkan versi Android
        val requiredPermissions = getRequiredPermissions()

        val missingPermissions = requiredPermissions.filter { !hasPermission(it) }

        // 2. Jika ada izin yang kurang, minta ke pengguna
        if (missingPermissions.isNotEmpty()) {
            Log.d("BluetoothFragment", "Meminta izin: $missingPermissions")
            requestBluetoothPermissions.launch(missingPermissions.toTypedArray())
            return
        }

        // 3. Jika Bluetooth tidak aktif, minta pengguna untuk mengaktifkannya
        if (!bluetoothAdapter.isEnabled) {
            Log.d("BluetoothFragment", "Bluetooth tidak aktif, meminta untuk diaktifkan.")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }

        // 4. (Untuk Android < 12) Jika layanan lokasi tidak aktif, minta pengguna mengaktifkannya
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationServiceEnabled()) {
            Log.d("BluetoothFragment", "Layanan lokasi tidak aktif, meminta untuk diaktifkan.")
            Snackbar.make(binding.root, "Layanan lokasi harus aktif untuk memindai perangkat.", Snackbar.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            showScanningStatus(false, "Layanan lokasi nonaktif.")
            return
        }

        // 5. Jika semua prasyarat terpenuhi, mulai pemindaian
        Log.d("BluetoothFragment", "Semua prasyarat terpenuhi. Memulai pemindaian.")
        startBluetoothScan()
    }

    /**
     * Memulai proses penemuan (discovery) perangkat Bluetooth.
     */
    @SuppressLint("MissingPermission") // Izin sudah dipastikan di checkPrerequisitesAndScan
    private fun startBluetoothScan() {
        // Hentikan pemindaian yang sedang berjalan sebelum memulai yang baru
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        // Kosongkan daftar perangkat yang ditemukan sebelumnya
        foundDevices.clear()
        // Perbarui juga daftar perangkat terpasang
        refreshPairedDevices()

        showScanningStatus(true, "Mencari printer di sekitar...")

        // Mulai penemuan
        if (!bluetoothAdapter.startDiscovery()) {
            Log.e("BluetoothFragment", "Gagal memulai discovery. Periksa status adapter.")
            showScanningStatus(false, "Gagal memulai pemindaian.")
            Snackbar.make(binding.root, "Tidak dapat memulai pemindaian. Coba matikan/hidupkan Bluetooth.", Snackbar.LENGTH_LONG).show()
        } else {
            Log.d("BluetoothFragment", "Pemindaian Bluetooth berhasil dimulai.")
        }
    }

    /**
     * Memuat ulang dan menampilkan daftar perangkat yang sudah dipasangkan (paired).
     * Izin BLUETOOTH_CONNECT harus sudah diberikan sebelum memanggil ini.
     */
    @SuppressLint("MissingPermission")
    private fun refreshPairedDevices() {
        try {
            pairedDevices.clear()
            val bondedDevices = bluetoothAdapter.bondedDevices
            bondedDevices?.forEach { device ->
                if (isDeviceLikelyPrinter(device)) {
                    val deviceItem = BluetoothDeviceItem(
                        device.name ?: "Unknown Device",
                        device.address,
                        isPaired = true
                    )
                    pairedDevices.add(deviceItem)
                }
            }
            Log.d("BluetoothFragment", "Memuat ${pairedDevices.size} printer yang sudah dipasangkan.")
        } catch (e: SecurityException) {
            Log.e("BluetoothFragment", "Gagal mendapatkan paired devices karena masalah izin.", e)
        }
        displayCombinedDevices()
    }

    /**
     * Menangani perangkat yang baru ditemukan oleh BroadcastReceiver.
     */
    @SuppressLint("MissingPermission")
    private fun handleFoundDevice(device: BluetoothDevice) {
        // Pastikan nama perangkat tidak null dan belum ada di daftar manapun
        if (device.name != null &&
            !foundDevices.any { it.address == device.address } &&
            !pairedDevices.any { it.address == device.address }) {

            // Filter hanya untuk perangkat yang kemungkinan adalah printer
            if (isDeviceLikelyPrinter(device)) {
                val deviceItem = BluetoothDeviceItem(
                    device.name,
                    device.address,
                    isPaired = false
                )
                foundDevices.add(deviceItem)
                displayCombinedDevices()
                Log.d("BluetoothFragment", "Printer ditemukan: ${device.name} (${device.address})")
            }
        }
    }

    /**
     * Menangani perubahan status pemasangan (bonding).
     */
    @SuppressLint("MissingPermission")
    private fun handleBondStateChange(device: BluetoothDevice, bondState: Int) {
        when (bondState) {
            BluetoothDevice.BOND_BONDED -> {
                Snackbar.make(binding.root, "Berhasil memasangkan dengan ${device.name}", Snackbar.LENGTH_SHORT).show()
                Log.d("BluetoothFragment", "Pemasangan berhasil dengan ${device.name}.")
                // Setelah berhasil dipasangkan, perbarui daftar dan coba hubungkan
                refreshPairedDevices()
                connectToDevice(device)
            }
            BluetoothDevice.BOND_NONE -> {
                Snackbar.make(binding.root, "Gagal memasangkan atau pemasangan dibatalkan.", Snackbar.LENGTH_SHORT).show()
                Log.w("BluetoothFragment", "Pemasangan gagal atau dilepas dari ${device.name}.")
                // Perbarui UI untuk merefleksikan status yang tidak terpasang lagi
                updateDeviceStatus(device.address, isPaired = false, isConnecting = false, isConnected = false)
            }
            BluetoothDevice.BOND_BONDING -> {
                Log.d("BluetoothFragment", "Sedang memasangkan dengan ${device.name}...")
                // UI sudah diatur ke 'isConnecting' saat createBond() dipanggil
            }
        }
    }

    /**
     * Menggabungkan daftar perangkat terpasang dan ditemukan, lalu menampilkannya di RecyclerView.
     */
    private fun displayCombinedDevices() {
        val combinedList = mutableListOf<BluetoothDeviceItem>()
        // Tambahkan perangkat terpasang, pastikan tidak ada duplikat
        combinedList.addAll(pairedDevices.distinctBy { it.address })

        // Tambahkan perangkat ditemukan yang belum terpasang
        val newFoundDevices = foundDevices.filter { found ->
            !pairedDevices.any { paired -> paired.address == found.address }
        }
        combinedList.addAll(newFoundDevices.distinctBy { it.address })

        // Urutkan: terpasang di atas, lalu berdasarkan nama
        combinedList.sortWith(compareBy<BluetoothDeviceItem> { !it.isPaired }.thenBy { it.name })

        // Perbarui adapter di thread utama
        lifecycleScope.launch(Dispatchers.Main) {
            deviceAdapter.updateDevices(combinedList)
        }
    }

    /**
     * Logika untuk memfilter apakah sebuah perangkat kemungkinan adalah printer.
     */
    @SuppressLint("MissingPermission")
    private fun isDeviceLikelyPrinter(device: BluetoothDevice): Boolean {
        val majorDeviceClass = device.bluetoothClass.majorDeviceClass
        val deviceClass = device.bluetoothClass.deviceClass

//        val DEVICE_CLASS_PRINTER_PORTABLE = 1664

        val PRINTER_DEVICE_CLASSES = listOf(1664, 1668) // Tambahkan jika tahu kode lainnya

        val isPrinterClass = majorDeviceClass == BluetoothClass.Device.Major.IMAGING &&
                deviceClass in PRINTER_DEVICE_CLASSES


//        // Kelas perangkat yang sering digunakan oleh printer
//        val isPrinterClass = majorDeviceClass == BluetoothClass.Device.Major.IMAGING &&
//                (deviceClass == BluetoothClass.Device.Major.IMAGING || deviceClass == 1664) // 1664 adalah kode untuk printer portabel
//
        // Heuristik berdasarkan nama perangkat
        val deviceName = device.name ?: ""
        val isNameLikelyPrinter = deviceName.contains("printer", ignoreCase = true) ||
                                 deviceName.contains("pos", ignoreCase = true) ||
                                 deviceName.contains("mpt", ignoreCase = true) ||
                                 deviceName.contains("thermal", ignoreCase = true) ||
                                 deviceName.startsWith("RPP", ignoreCase = true)

        return isPrinterClass || isNameLikelyPrinter
    }

    /**
     * Callback saat item perangkat di RecyclerView diklik.
     */
    @SuppressLint("MissingPermission")
    override fun onDeviceClick(deviceItem: BluetoothDeviceItem) {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        try {
            val remoteDevice = bluetoothAdapter.getRemoteDevice(deviceItem.address)

            if (remoteDevice.bondState == BluetoothDevice.BOND_BONDED) {
                // Jika sudah dipasangkan, langsung coba hubungkan
                Log.d("BluetoothFragment", "Perangkat sudah dipasangkan. Mencoba menghubungkan ke ${deviceItem.name}.")
                connectToDevice(remoteDevice)
            } else {
                // Jika belum dipasangkan, mulai proses pemasangan
                Log.d("BluetoothFragment", "Perangkat belum dipasangkan. Memulai proses pemasangan dengan ${deviceItem.name}.")
                updateDeviceStatus(deviceItem.address, isPaired = false, isConnecting = true, isConnected = false)
                remoteDevice.createBond()
            }
        } catch (e: SecurityException) {
            Log.e("BluetoothFragment", "SecurityException saat onDeviceClick: ${e.message}", e)
            Snackbar.make(binding.root, "Izin Bluetooth CONNECT ditolak.", Snackbar.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            Log.e("BluetoothFragment", "Alamat Bluetooth tidak valid: ${deviceItem.address}", e)
            Snackbar.make(binding.root, "Alamat perangkat tidak valid.", Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Menghubungkan ke perangkat Bluetooth di thread IO.
     */
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        updateDeviceStatus(device.address, isPaired = true, isConnecting = true, isConnected = false)

        lifecycleScope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                // Membuat socket RFCOMM untuk koneksi
                socket = device.createRfcommSocketToServiceRecord(sppUuid)

                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Menghubungkan ke ${device.name}...", Snackbar.LENGTH_SHORT).show()
                }

                socket.connect() // Operasi blocking

                // Jika koneksi berhasil
                withContext(Dispatchers.Main) {
                    Log.d("BluetoothFragment", "Berhasil terhubung ke ${device.name}")
                    Snackbar.make(binding.root, "Terhubung ke ${device.name}", Snackbar.LENGTH_SHORT).show()
                    updateDeviceStatus(device.address, isPaired = true, isConnecting = false, isConnected = true)

                    // Simpan koneksi printer untuk digunakan di fragment lain
                    // (Contoh menggunakan Singleton atau ViewModel)
                    // PrinterConnectionManager.setConnection(BluetoothConnection(device))
                }

            } catch (e: IOException) {
                // Gagal terhubung
                withContext(Dispatchers.Main) {
                    Log.e("BluetoothFragment", "IOException saat menghubungkan: ${e.message}", e)
                    Snackbar.make(binding.root, "Gagal terhubung. Pastikan printer aktif dan dekat.", Snackbar.LENGTH_LONG).show()
                    updateDeviceStatus(device.address, isPaired = true, isConnecting = false, isConnected = false)
                }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    Log.e("BluetoothFragment", "SecurityException saat menghubungkan: ${e.message}", e)
                    updateDeviceStatus(device.address, isPaired = true, isConnecting = false, isConnected = false)
                }
            } finally {
                // Jangan tutup socket di sini jika akan digunakan oleh library ESC/POS.
                // Library tersebut yang akan mengelola siklus hidup socket.
                // try { socket?.close() } catch (e: IOException) { Log.e("BluetoothFragment", "Gagal menutup socket.", e) }
            }
        }
    }

    // --- Helper & Utility Functions ---

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Untuk Android 11 (R) ke bawah, lokasi juga diperlukan
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = ContextCompat.getSystemService(requireContext(), LocationManager::class.java)
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
               locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    private fun showScanningStatus(isScanning: Boolean, statusText: String) {
        binding.progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE
        binding.statusTextView.text = statusText
        binding.scanButton.text = if (isScanning) "Berhenti Memindai" else "Pindai Printer"
    }

    private fun updateDeviceStatus(address: String, isPaired: Boolean, isConnecting: Boolean, isConnected: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            deviceAdapter.updateDeviceStatus(address, isPaired, isConnecting, isConnected)
        }
    }

    private fun getBondStateString(state: Int): String {
        return when (state) {
            BluetoothDevice.BOND_NONE -> "NONE"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            BluetoothDevice.BOND_BONDED -> "BONDED"
            else -> "ERROR"
        }
    }
}
