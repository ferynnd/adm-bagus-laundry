package dev.ferynnd.baguslaundry.ui.user

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentPrintPreviewBinding
import dev.ferynnd.baguslaundry.model.LaundryPrintTransaction
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PrintPreviewFragment : Fragment() {

    private var _binding: FragmentPrintPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var productLaundryViewModel: LaundryProductViewModel

    private lateinit var sharePreferences: SharePrefrenceHelper

    private var transactionId: Int? = null

    private var productLaundryList: List<ProductLaundry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        laundryReportViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        productLaundryViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java]
        productLaundryViewModel.init(requireContext())
        laundryReportViewModel.init(requireContext())
        branchViewModel.init(requireContext())

        transactionId = arguments?.getInt("transactionId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrintPreviewBinding.inflate(inflater, container, false)
        sharePreferences = SharePrefrenceHelper(requireContext())
        hideBottomNavigationView()
        binding.printButton.setOnClickListener {
            printReceiptToThermalPrinter()
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
            laundryReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            // Ambil semua product laundry terlebih dahulu
            productLaundryViewModel.laundryProducts.observe(viewLifecycleOwner) { list ->
                productLaundryList = list
                fetchTransactionData(transactionId!!)
            }

            productLaundryViewModel.getProductLaundry() // Memulai fetch product laundry

        }
    }

    private fun fetchTransactionData(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = laundryReportViewModel.getLaundryPrint(id)
                if (result.success && result.data != null) {
                    laundryReportViewModel.postPrintData(result.data)
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
                    "Terjadi kesalahan saat memuat data",
                    Snackbar.LENGTH_LONG
                ).show()
                binding.printButton.isEnabled = false
            }
        }
    }

    private fun populateReceiptData(data: LaundryPrintTransaction) =
        with(binding.receiptLayoutInclude) {
            textStoreName.text = "BAGUS LAUNDRY"

            val idUser = sharePreferences.getString("PREF_USER_ID")
            viewLifecycleOwner.lifecycleScope.launch {
                val userData = userViewModel.getUserById(idUser?.toInt() ?: 0)
                val filterBranch =
                    branchViewModel.branches.value?.find { it.id_branch == userData.data.id_branch_user }
                textStoreAddress.text = filterBranch?.full_address_branch ?: "Alamat tidak tersedia"
            }

            textInvoiceNumber.text = data.number_transaction_laundry.toString()
            textClientName.text =
                data.name_client_transaction_laundry.uppercase(Locale.getDefault())
            textOrderDate.text = formatDate(data.first_date_transaction_laundry)

            textStatus.text = when (data.status_transaction_laundry?.uppercase()) {
                StatusReportLaundry.completed.name.uppercase() -> "Selesai"
                StatusReportLaundry.unpaid.name.uppercase() -> "Belum Bayar"
                StatusReportLaundry.paid.name.uppercase() -> "Sudah Bayar"
                StatusReportLaundry.cancelled.name.uppercase() -> "Dibatalkan"
                else -> "Status Tidak Diketahui"
            }

            layoutItemList.removeAllViews()
            data.list_transaction_laundry.forEach { item ->
                val itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_receipt_row, layoutItemList, false)
                val name = getLaundryServiceItemName(item.id_item_laundry)
                itemView.findViewById<TextView>(R.id.text_item_name_and_calc).text =
                    "$name (${formatWeight(item.weight_list_transaction_laundry)} kg)"
                itemView.findViewById<TextView>(R.id.text_item_total_price).text =
                    formatCurrency(item.total_price_list_transaction_laundry)
                layoutItemList.addView(itemView)
            }

            textSubtotalPrice.text = formatCurrency(data.total_price_transaction_laundry)
            textPromo.text = "- ${formatCurrency(data.promo_transaction_laundry)}"
            textAdditionalCost.text = formatCurrency(data.additional_cost_transaction_laundry)
            textTotalPayment.text = formatCurrency(data.total_transaction_laundry)
            textCashPaid.text = formatCurrency(data.cash_transaction_laundry)
            textChangeMoney.text = formatCurrency(data.change_money_transaction_laundry)

            if (!data.notes_transaction_laundry.isNullOrBlank()) {
                layoutNotes.visibility = View.VISIBLE
                textNotesLabel.visibility = View.VISIBLE
                textNotes.visibility = View.VISIBLE
                textNotes.text = data.notes_transaction_laundry
            } else {
                layoutNotes.visibility = View.GONE
                textNotesLabel.visibility = View.GONE
                textNotes.visibility = View.GONE
            }
        }

    private fun getLaundryServiceItemName(itemId: Int): String {
        return productLaundryList.find { it.id_laundry_item == itemId }?.name_laundry_item
            ?: "Layanan Tidak Dikenal ($itemId)"
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        return formatter.format(amount).replace("Rp", "Rp ")
    }

    private fun formatWeight(weight: Double): String {
        return String.format(Locale.getDefault(), "%.1f", weight)
    }

    private fun formatDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: ParseException) {
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

            val printer = EscPosPrinter(printerConnection, 203, 48f, 32)

            val previewData = generateReceiptText() // Fungsi untuk buat string preview
            printer.printFormattedText(previewData)

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Gagal mencetak: ${e.message}", Snackbar.LENGTH_LONG).show()
            Log.e("PrinterError", e.stackTraceToString())
        }
    }

    private fun generateReceiptText(): String {
        val data = laundryReportViewModel.printData.value ?: return ""

        val storeName = "[C]<b>BAGUS LAUNDRY</b>\n"
        val storeAddress = "[C]${binding.receiptLayoutInclude.textStoreAddress.text}\n"
        val invoice = "[L]No. Invoice: ${data.number_transaction_laundry}\n"
        val client = "[L]Nama: ${data.name_client_transaction_laundry.uppercase()}\n"
        val date = "[L]Tanggal: ${formatDate(data.first_date_transaction_laundry)}\n"
        val status = "[L]Status: ${binding.receiptLayoutInclude.textStatus.text}\n"
        val divider = "------------------------------\n"

        val items = buildString {
            data.list_transaction_laundry.forEach {
                val name = getLaundryServiceItemName(it.id_item_laundry)
                val line = "[L]$name (${formatWeight(it.weight_list_transaction_laundry)} kg)"
                val price = "[R]${formatCurrency(it.total_price_list_transaction_laundry)}"
                append("$line$price\n")
            }
        }

        val subtotal =
            "[L]Subtotal:              [R]${formatCurrency(data.total_price_transaction_laundry)}\n"
        val promo =
            "[L]Promo:                 [R]- ${formatCurrency(data.promo_transaction_laundry)}\n"
        val addCost =
            "[L]Biaya Tambahan:        [R]${formatCurrency(data.additional_cost_transaction_laundry)}\n"
        val total =
            "[L]<b>Total:              [R]${formatCurrency(data.total_transaction_laundry)}</b>\n"
        val cash = "[L]Tunai:                 [R]${formatCurrency(data.cash_transaction_laundry)}\n"
        val change =
            "[L]Kembalian:             [R]${formatCurrency(data.change_money_transaction_laundry)}\n"
        val notes = if (!data.notes_transaction_laundry.isNullOrBlank())
            "[L]Catatan: ${data.notes_transaction_laundry}\n" else ""

        val thanks = "\n[C]--- TERIMA KASIH ---\n"

        return storeName + storeAddress + invoice + client + date + status + divider +
                items + divider + subtotal + promo + addCost + total + cash + change + notes + thanks
    }


    private fun hideBottomNavigationView() {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
        _binding = null
    }
}
