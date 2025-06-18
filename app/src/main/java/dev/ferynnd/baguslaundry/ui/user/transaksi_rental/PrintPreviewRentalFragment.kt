package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentPrintPreviewRentalBinding
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.RentalPrintTransaction
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PrintPreviewRentalFragment : Fragment() {

    private var _binding: FragmentPrintPreviewRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var clientViewModel: ClientViewModel

    private lateinit var sharePreferences: SharePrefrenceHelper

    private var transactionId: Int? = null

    private var productRentalList: List<ProductRental> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())
        rentalProductViewModel.init(requireContext())
        rentalReportViewModel.init(requireContext())
        branchViewModel.init(requireContext())

        transactionId = arguments?.getInt("transactionId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrintPreviewRentalBinding.inflate(inflater, container, false)
        sharePreferences = SharePrefrenceHelper(requireContext())
        hideBottomNavigationView()
        binding.printButton.setOnClickListener {
            printReceiptToThermalPrinter()
        }
        binding.backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, UserDashboardFragment())
                .addToBackStack(null) // opsional, jika ingin bisa kembali
                .commit()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (transactionId == null) {
            Snackbar.make(binding.root, "ID transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
            binding.printButton.isEnabled = false
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Loading observer
            rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            // Ambil semua product rental terlebih dahulu
            rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { list ->
                productRentalList = list
                fetchTransactionData(transactionId!!)
            }
            rentalProductViewModel.getProductRental() // Memulai fetch product rental
        }
    }

    private fun fetchTransactionData(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = rentalReportViewModel.getRentalPrint(id)
                if (result.success && result.data != null) {
                    rentalReportViewModel.postPrintData(result.data)
                    populateReceiptData(result.data)
                } else {
                    Snackbar.make(
                        binding.root,
                        result.message ?: "Data tidak ditemukan",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.printButton.isEnabled = false
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Terjadi kesalahan saat memuat data: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                binding.printButton.isEnabled = false
                Log.e(
                    "FetchDataError",
                    "Error fetching transaction data: ${e.stackTraceToString()}"
                )
            }
        }
    }

    private fun populateReceiptData(data: RentalPrintTransaction) =
        with(binding.receiptLayoutInclude) {
            // Header Section
            textStoreName.text = "BAGUS LAUNDRY"

            // Menggunakan lifecycleScope.launchWhenStarted untuk menghindari crash jika fragment/activity sudah dihancurkan
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                try {
                    val idUser = sharePreferences.getString("PREF_USER_ID")
                    val userDataResponse = userViewModel.getUserById(idUser?.toInt() ?: 0)
                    if (userDataResponse.success && userDataResponse.data != null) {
                        val filterBranch =
                            branchViewModel.branches.value?.find { it.id_branch == userDataResponse.data.id_branch_user }
                        textStoreAddress.text =
                            filterBranch?.full_address_branch ?: "Alamat tidak tersedia"
                    } else {
                        textStoreAddress.text = "Alamat tidak tersedia"
                    }
                } catch (e: Exception) {
                    Log.e("PopulateData", "Error fetching user/branch data: ${e.message}")
                    textStoreAddress.text = "Alamat tidak tersedia (Error)"
                }
            }

            // Invoice Information Section
            // Menggunakan ID baru text_invoice_number
            textInvoiceNumber.text =
                data.number_transaction_rental.toString() // Hanya nomor invoice, tanpa label "Invoice #:"
            textClientName.text =
                getClientName(data.id_client_transaction_rental!!)

            // Menambahkan text_penerima sesuai layout baru
            // Asumsi recipient_name_transaction_rental adalah nama penerima
            // Jika ada data klien terpisah, Anda perlu sesuaikan
            textPenerima.text =
                data.recipient_name_transaction_rental?.uppercase(Locale.getDefault())

            // Menggunakan formatDate dan menyesuaikan label "WAKTU"
            textOrderDate.text =
                formatDate(data.time_transaction_rental.toString()) // formatDate harus mengembalikan format yang diinginkan

            // Items List Container
            layoutItemList.removeAllViews() // Gunakan layout_item_list sesuai layout XML

            data.list_transaction_rentals.forEach { item ->
                // Menggunakan layout item yang lebih sederhana, sesuai layout XML terbaru
                val itemView = LayoutInflater.from(requireContext())
                    .inflate(
                        R.layout.item_receipt_row_rental,
                        layoutItemList,
                        false
                    ) // Misalkan nama layoutnya receipt_item_row_simple

                val itemNameTextView =
                    itemView.findViewById<TextView>(R.id.text_item_name) // ID TextView di layout item baru
                itemNameTextView.text = getRentalServiceItemName(item.id_item_rental!!)

                val itemCount = itemView.findViewById<TextView>(R.id.text_item_quantity)
                itemCount.text = "Jumlah: ${item.count_list_transaction_rental} PCS"
                val itemWeight = itemView.findViewById<TextView>(R.id.text_item_weight)
                itemWeight.text = "Berat: ${formatWeight(item.weight_list_transaction_rental)} Kg"

                layoutItemList.addView(itemView)
            }

            // Notes Section
            val layoutNotes = binding.receiptLayoutInclude.layoutNotes // Akses layout_notes
            if (!data.notes_transaction_rental.isNullOrBlank()) {
                textNotes.text = data.notes_transaction_rental
                layoutNotes.visibility = View.VISIBLE // Mengubah visibilitas LinearLayout induk
            } else {
                layoutNotes.visibility = View.GONE
            }

        }

    // --- Helper function untuk menerjemahkan kondisi item ---
    private fun translateCondition(condition: String?): String {
        return when (condition?.lowercase(Locale.getDefault())) {
            "clean" -> "Bersih"
            "dirty" -> "Kotor"
            "damaged" -> "Rusak"
            else -> "-"
        }
    }

    // --- Helper function untuk menerjemahkan status item (in/out/cancelled) ---
    private fun translateItemStatus(status: String?): String {
        return when (status?.lowercase(Locale.getDefault())) {
            "in" -> "Masuk"
            "out" -> "Keluar"
            "cancelled" -> "Dibatalkan" // Status item juga bisa dibatalkan
            else -> "-"
        }
    }


    private fun getRentalServiceItemName(itemId: Int): String {
        return productRentalList.find { it.id_rental_item == itemId }?.name_rental_item
            ?: "Layanan Tidak Dikenal ($itemId)"
    }

    private fun formatWeight(weight: Double?): String {
        return String.format(Locale.getDefault(), "%.1f", weight ?: 0.0) // Handle null weight
    }

    private fun formatDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat(
            "dd MMMM yyyy HH:mm",
            Locale.getDefault()
        ) // Format: 15 Juni 2025 12:56
        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: ParseException) {
            Log.e("DateFormatError", "Error parsing date: $dateString", e)
            dateString
        }
    }

    private fun printReceiptToThermalPrinter() {
        try {
            val printerConnection: DeviceConnection? =
                BluetoothPrintersConnections.selectFirstPaired()

            if (printerConnection == null) {
                Snackbar.make(
                    binding.root,
                    "Tidak ada printer yang terhubung",
                    Snackbar.LENGTH_LONG
                ).show()
                return
            }

            val printer = EscPosPrinter(printerConnection, 203, 57f, 32)

            val previewData = generateReceiptText()
            printer.printFormattedText(previewData)
            printer.disconnectPrinter()

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Gagal mencetak: ${e.message}", Snackbar.LENGTH_LONG).show()
            Log.e("PrinterError", e.stackTraceToString())
        }
    }

    private fun generateReceiptText(): String {
        val data = rentalReportViewModel.printData.value ?: return ""

        val storeName = "[C]<b>BAGUS LAUNDRY</b>\n"
        val storeAddress = "[C]<font size='normal'>${binding.receiptLayoutInclude.textStoreAddress.text}</font>\n"
        val dividerSolid = "--------------------------------\n"
        val dividerDashed = "--------------------------------\n"

        val header = buildString {
            append(storeName)
            append(storeAddress)
            append(dividerSolid)
            append("[L]<font size='normal'>INVOICE      : ${data.number_transaction_rental}</font>\n")
            append("[L]<font size='normal'>KLIEN        : ${getClientName(data.id_client_transaction_rental!!)}</font>\n")
            append("[L]<font size='normal'>PENERIMA     : ${data.recipient_name_transaction_rental?.uppercase() ?: "-"}</font>\n")
            append("[L]<font size='normal'>WAKTU        : ${formatDate(data.time_transaction_rental.toString())}</font>\n")
            append(dividerDashed)
            append("[L]<b>ITEM</b>\n\n")
        }

        val items = buildString {
            data.list_transaction_rentals.forEach { item ->
                val itemName = getRentalServiceItemName(item.id_item_rental!!)
                append("[L]$itemName\n")
                append("[L]<font size='normal'> Jumlah : ${item.count_list_transaction_rental} PCS</font>\n")
                append("[L]<font size='normal'> Berat  : ${formatWeight(item.weight_list_transaction_rental)} Kg</font>\n")
                append("[L]<font size='normal'> Kondisi: ${translateCondition(item.condition_list_transaction_rental)}</font>\n")
                append("[L]<font size='normal'> Status : ${translateItemStatus(item.status_list_transaction_rental)}</font>\n")
                append("[L]--------------------------------\n")
            }
        }

        val summary = buildString {
            append("[L]<font size='normal'><b>TOTAL PCS   : ${data.total_pcs_transaction_rental}</font>\n")
            append("[L]<font size='normal'><b>TOTAL BERAT : ${formatWeight(data.total_weight_transaction_rental)} Kg</font>\n")
            if (!data.notes_transaction_rental.isNullOrBlank()) {
                append("[L]<font size='normal'>CATATAN     : ${data.notes_transaction_rental}</font>\n")
            }
            append(dividerSolid)
            append("\n[C]<b>Terima Kasih</b>\n")
            append("[C]Telah Mempercayakan\n")
            append("[C]Laundry'an Anda Kepada Kami\n\n")
        }

        return header + items + summary
    }

     private fun getClientName(itemId: Int): String {
        val clients = clientViewModel.clients.value ?: return "Layanan Tidak Dikenal ($itemId)"
        return clients.find { it.id_client == itemId }?.name_client ?: "Layanan Tidak Dikenal ($itemId)"
    }


    private fun hideBottomNavigationView() {
        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.VISIBLE
        _binding = null
    }
}