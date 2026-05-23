package dev.ferynnd.admbaguslaundry.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.google.android.material.snackbar.Snackbar
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentPrintPreviewRentalBinding
import dev.ferynnd.admbaguslaundry.model.Client
import dev.ferynnd.admbaguslaundry.model.ProductRental
import dev.ferynnd.admbaguslaundry.model.RentalPrintTransaction
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
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
    private var currentTransactionData: RentalPrintTransaction? = null
    private var productRentalList: List<ProductRental> = emptyList()

    companion object {
        private const val TAG = "PRINT_RENTAL"
    }

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPrintPreviewRentalBinding.inflate(inflater, container, false)
        sharePreferences = SharePrefrenceHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideBottomNavigationView()

        binding.printButton.isEnabled = false
        binding.kirimButton.isEnabled = false

        Log.d(TAG, "TRANSACTION ID: $transactionId")

        binding.printButton.setOnClickListener {
            printReceiptFromWebView()
        }

        binding.kirimButton.setOnClickListener {
            shareReceipt()
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (transactionId == null || transactionId == 0) {
            Snackbar.make(binding.root, "ID transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
            return
        }

        rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { list ->
            productRentalList = list
            fetchTransactionData(transactionId!!)
        }

        rentalProductViewModel.getProductRentalPage()
    }

    private fun fetchTransactionData(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "FETCH PRINT DATA ID: $id")

                val result = rentalReportViewModel.getRentalPrint(id)

                Log.d(TAG, "PRINT RESPONSE: $result")

                if (result.success && result.data != null) {
                    currentTransactionData = result.data
                    rentalReportViewModel.postPrintData(result.data)

                    val htmlContent = generateReceiptHtml(result.data)
                    setupReceiptPreview(htmlContent)
                } else {
                    Snackbar.make(
                        binding.root,
                        result.message ?: "Data tidak ditemukan",
                        Snackbar.LENGTH_LONG
                    ).show()

                    binding.printButton.isEnabled = false
                    binding.kirimButton.isEnabled = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR FETCH PRINT DATA", e)

                Snackbar.make(
                    binding.root,
                    "Terjadi kesalahan saat memuat data: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()

                binding.printButton.isEnabled = false
                binding.kirimButton.isEnabled = false
            }
        }
    }

    private fun translateCondition(condition: String?): String {
        return when (condition?.lowercase(Locale.getDefault())) {
            "clean" -> "Bersih"
            "dirty" -> "Kotor"
            "damaged" -> "Rusak"
            else -> "-"
        }
    }

    private fun translateItemStatus(status: String?): String {
        return when (status?.lowercase(Locale.getDefault())) {
            "in" -> "Masuk"
            "out" -> "Keluar"
            "cancelled" -> "Dibatalkan"
            else -> "-"
        }
    }

    private fun getRentalServiceItemName(itemId: Int): String {
        return productRentalList.find { it.id_rental_item == itemId }?.name_rental_item
            ?: "Layanan Tidak Dikenal ($itemId)"
    }

    private fun formatWeight(weight: Double?): String {
        return String.format(Locale.getDefault(), "%.1f", weight ?: 0.0)
    }

    private fun formatDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())

        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: ParseException) {
            Log.e("DateFormatError", "Error parsing date: $dateString", e)
            dateString
        }
    }

    private fun getClientName(itemId: Int): String {
        val clients = clientViewModel.clients.value ?: return "Client Tidak Dikenal ($itemId)"
        return clients.find { it.id_client == itemId }?.name_client
            ?: "Client Tidak Dikenal ($itemId)"
    }

    private fun getClientData(clientId: Int): Client? {
        return clientViewModel.clients.value?.find { it.id_client == clientId }
    }

    private fun generateReceiptHtml(data: RentalPrintTransaction): String {
        val branchList = branchViewModel.branches.value.orEmpty()
        val selectedBranch = branchList.find { it.id_branch == data.id_branch_transaction_rental }
        val branchTimezone = selectedBranch?.timezone_branch ?: "Asia/Jakarta"

        val logoBitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.logo_bagus)
        val scaledLogo = Bitmap.createScaledBitmap(
            logoBitmap,
            120,
            (120.0 / logoBitmap.width * logoBitmap.height).toInt(),
            true
        )
        val logoBase64 = bitmapToBase64(scaledLogo)

        return buildString {
            append(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=384, initial-scale=1.0">
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                            font-family: Arial, sans-serif;
                        }
                        body {
                            background-color: white;
                            color: black;
                            width: 384px;
                            padding: 12px;
                            margin: 0 auto;
                        }
                        .invoice-container {
                            width: 100%;
                            padding: 8px;
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 8px;
                            border-bottom: 2px solid black;
                            padding-bottom: 6px;
                        }
                        .header img {
                            max-width: 120px;
                            margin-bottom: 4px;
                        }
                        .header p {
                            font-size: 14px;
                        }
                        .invoice-info {
                            display:grid;
                            grid-template-columns:1fr 1fr;
                            gap:5px;
                            margin-bottom:10px;
                            line-height: 1.5;
                            font-size: 12px;
                        }
                        .invoice-info > div {
                            margin-bottom: 4px;
                        }
                        .invoice-info b {
                            font-size: 14px;
                        }
                        .items-table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-bottom: 8px;
                            font-size: 12px;
                        }
                        .items-table th {
                            border-bottom: 2px solid black;
                            padding: 4px 2px;
                            text-align: left;
                            font-size: 12px;
                        }
                        .items-table td {
                            padding: 4px 2px;
                            border-bottom: 1px dashed #999;
                            vertical-align: top;
                        }
                        .item-name {
                            font-weight: bold;
                            font-size: 14px;
                            margin-bottom: 2px;
                        }
                        .item-details {
                            font-size: 12px;
                            color: #444;
                            line-height: 1.3;
                        }
                        .notes {
                            border: 1px solid black;
                            padding: 6px;
                            margin-bottom: 8px;
                            font-size: 11px;
                            line-height: 1.4;
                        }
                        .notes b {
                            font-size: 12px;
                        }
                        .footer {
                            text-align: center;
                            border-top: 2px solid black;
                            font-weight: bold;
                            padding-top: 6px;
                            font-size: 12px;
                            margin-top: 8px;
                        }
                        .col-service {
                            width: 50%;
                        }
                        .col-weight {
                            width: 25%;
                            font-size: 12px;
                        }
                        .col-qty {
                            width: 25%;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                """.trimIndent()
            )

            append("""<div class="invoice-container">""")

            append(
                """
                <div class="header">
                    <img src="data:image/png;base64,$logoBase64"/>
                    <p><b>Telp/WA: 082329197772</b></p>
                </div>
                """.trimIndent()
            )

            append(
                """
                <div class="invoice-info">
                    <div><b>No. Invoice:</b><br>${data.number_transaction_rental ?: "-"}</div>
                    <div><b>Klien:</b><br>${getClientName(data.id_client_transaction_rental ?: 0)}</div>
                    <div><b>Penerima:</b><br>${data.recipient_name_transaction_rental?.uppercase(Locale.getDefault()) ?: "-"}</div>
                    <div><b>Tanggal:</b><br>${data.time_transaction_rental?.toBranchTime(branchTimezone) ?: "-"}</div>
                </div>
                """.trimIndent()
            )

            append(
                """
                <table class="items-table">
                    <thead>
                        <tr>
                            <th class="col-service">Layanan</th>
                            <th class="col-weight">Berat</th>
                            <th class="col-qty">Jumlah</th>
                        </tr>
                    </thead>
                    <tbody>
                """.trimIndent()
            )

            data.list_transaction_rentals.forEach { item ->
                val name = getRentalServiceItemName(item.id_item_rental ?: 0)
                val condition = translateCondition(item.condition_list_transaction_rental)
                val status = translateItemStatus(item.status_list_transaction_rental)

                append(
                    """
                    <tr>
                        <td class="col-service">
                            <div class="item-name">$name</div>
                            <div class="item-details">$condition - $status</div>
                        </td>
                        <td class="col-weight">${formatWeight(item.weight_list_transaction_rental)} Kg</td>
                        <td class="col-qty">${item.count_list_transaction_rental ?: 0} PCS</td>
                    </tr>
                    """.trimIndent()
                )
            }

            append("</tbody></table>")

            if (!data.notes_transaction_rental.isNullOrBlank()) {
                append(
                    """
                    <div class="notes">
                        <b>Catatan:</b><br>${data.notes_transaction_rental}
                    </div>
                    """.trimIndent()
                )
            }

            append(
                """
                <div class="notes">
                    <b>PERHATIAN:</b><br>
                    1. Cucian rusak karena sifat bahan/kain bukan tanggung jawab kami<br>
                    2. Cucian luntur yang tidak diberitahukan kepada kami diluar tanggung jawab kami<br>
                    3. Apabila konsumen tidak menghitung cucian, jumlah yang kami hitung kami anggap benar<br>
                    4. Pengajuan klaim tidak lebih dari 24 jam setelah diterima<br>
                    5. Benda berharga/barang yang tertinggal dalam cucian apabila hilang/rusak bukan tanggung jawab kami<br>
                    6. Barang yang tidak diambil lebih dari 1 bulan bukan tanggung jawab kami
                </div>
                """.trimIndent()
            )

            append("""<div class="footer">— TERIMA KASIH —</div></div></body></html>""")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupReceiptPreview(htmlContent: String) {
        binding.receiptWebView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = false
            useWideViewPort = false
            builtInZoomControls = false
            displayZoomControls = false
        }

        binding.receiptWebView.setInitialScale(100)

        binding.receiptWebView.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )

        binding.receiptWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.printButton.isEnabled = true
                binding.kirimButton.isEnabled = true
            }
        }
    }

    private fun printReceiptFromWebView() {
        try {
            val printerAddress = requireContext()
                .getSharedPreferences("printer_pref", Context.MODE_PRIVATE)
                .getString("printer_address", null)

            if (printerAddress == null) {
                Snackbar.make(binding.root, "Printer belum dipilih", Snackbar.LENGTH_LONG).show()
                return
            }

            val printerConnection: DeviceConnection? =
                BluetoothPrintersConnections()
                    .list
                    ?.firstOrNull { it.device.address == printerAddress }

            if (printerConnection == null) {
                Snackbar.make(binding.root, "Tidak ada printer yang terhubung", Snackbar.LENGTH_LONG).show()
                return
            }

            val data = currentTransactionData
            if (data == null) {
                Snackbar.make(binding.root, "Data transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
                return
            }

            val printer = EscPosPrinter(
                printerConnection,
                203,
                48f,
                30
            )

            val printText = generateReceiptEscPosText(data)

            printer.printFormattedText(printText)

            showAlert(
                title = "Berhasil!",
                message = "Struk berhasil dicetak",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.success
            )
        } catch (e: Exception) {
            showAlert(
                title = "Gagal!",
                message = "Gagal mencetak: ${e.message}",
                backgroundColorRes = R.color.red600,
                iconRes = R.drawable.failed
            )
        }
    }

    private fun generateReceiptEscPosText(data: RentalPrintTransaction): String {
        val branchList = branchViewModel.branches.value.orEmpty()
        val selectedBranch = branchList.find {
            it.id_branch == data.id_branch_transaction_rental
        }

        val branchTimezone = selectedBranch?.timezone_branch ?: "Asia/Jakarta"

        val invoiceNumber = safeEscPos(data.number_transaction_rental?.toString() ?: "-")
        val clientName = safeEscPos(getClientName(data.id_client_transaction_rental ?: 0))
        val recipientName = safeEscPos(
            data.recipient_name_transaction_rental?.uppercase(Locale.getDefault()) ?: "-"
        )
        val transactionDate = safeEscPos(
            data.time_transaction_rental?.toBranchTime(branchTimezone) ?: "-"
        )

        val SEP = "[C]------------------------------\n"

        return buildString {
            append("[C]BAGUS LAUNDRY\n")
            append("[C]Telp/WA : 082329197772\n")
            append(SEP)
            append("[C]STRUK SEWA\n")
            append(SEP)

            append("[L]No Inv  : $invoiceNumber\n")
            append("[L]Klien   : ${limitText(clientName, 20)}\n")
            append("[L]Nama    : ${limitText(recipientName, 20)}\n")

            val dateLines = wrapText("Tanggal : $transactionDate", 30)
            dateLines.forEachIndexed { i, line ->
                if (i == 0) append("[L]$line\n")
                else append("[L]          $line\n")
            }

            append(SEP)
            append("[C]DAFTAR ITEM\n")
            append(SEP)

            data.list_transaction_rentals.forEachIndexed { index, item ->
                val itemName = safeEscPos(getRentalServiceItemName(item.id_item_rental ?: 0))
                val condition = safeEscPos(translateCondition(item.condition_list_transaction_rental))
                val status = safeEscPos(translateItemStatus(item.status_list_transaction_rental))
                val weight = formatWeight(item.weight_list_transaction_rental)
                val qty = "${item.count_list_transaction_rental ?: 0}"

                val nameLines = wrapText("${index + 1}. $itemName", 30)
                nameLines.forEach { line ->
                    append("[L]$line\n")
                }

                append("[L]   Berat : $weight kg[R]$qty pcs\n")
                append("[L]   $condition - $status\n")
                append(SEP)
            }

            if (!data.notes_transaction_rental.isNullOrBlank()) {
                append("[L]Catatan :\n")
                wrapText(data.notes_transaction_rental, 28).forEach { line ->
                    append("[L]  $line\n")
                }
                append(SEP)
            }

            append("[C]TERIMA KASIH\n")
            append(SEP)
        }
    }

    private fun safeEscPos(text: String?): String {
        return text
            ?.replace("<", "")
            ?.replace(">", "")
            ?.replace("[", "")
            ?.replace("]", "")
            ?.replace("&", "dan")
            ?.replace("\n", " ")
            ?.replace("\r", " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?: "-"
    }

    private fun limitText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) text.take(maxLength) else text
    }

    private fun wrapText(text: String, maxLength: Int): List<String> {
        val cleanText = safeEscPos(text)

        if (cleanText.length <= maxLength) {
            return listOf(cleanText)
        }

        val words = cleanText.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            if (word.length > maxLength) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = ""
                }

                word.chunked(maxLength).forEach {
                    lines.add(it)
                }
            } else if (currentLine.isEmpty()) {
                currentLine = word
            } else if (currentLine.length + 1 + word.length <= maxLength) {
                currentLine += " $word"
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun shareReceipt() {
        currentTransactionData?.let { data ->
            try {
                val branchList = branchViewModel.branches.value.orEmpty()
                val selectedBranch = branchList.find {
                    it.id_branch == data.id_branch_transaction_rental
                }
                val branchTimezone = selectedBranch?.timezone_branch ?: "Asia/Jakarta"

                binding.progressBar.visibility = View.VISIBLE
                binding.kirimButton.isEnabled = false

                val bitmap = captureWebView(binding.receiptWebView)
                val imageFile = saveBitmapToFile(bitmap)

                if (imageFile == null) {
                    Snackbar.make(binding.root, "Gagal menyimpan gambar struk", Snackbar.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.kirimButton.isEnabled = true
                    return
                }

                val imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    imageFile
                )

                val client = getClientData(data.id_client_transaction_rental ?: 0)

                val message = buildString {
                    append("Halo,\n\n")
                    append("Berikut adalah struk transaksi rental:\n\n")
                    append("📋 No. Invoice: ${data.number_transaction_rental}\n")
                    append("👤 Klien: ${client?.name_client ?: "Tidak tersedia"}\n")
                    append("📅 Tanggal: ${data.time_transaction_rental?.toBranchTime(branchTimezone)}\n\n")
                    append("Terima kasih telah menggunakan layanan kami. 🙏")
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra(Intent.EXTRA_SUBJECT, "Struk Transaksi Rental - ${data.number_transaction_rental}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Bagikan struk melalui")

                startActivity(chooserIntent)

            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Gagal membagikan struk: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                Log.e(TAG, "SHARE ERROR", e)
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.kirimButton.isEnabled = true
            }
        } ?: run {
            Snackbar.make(binding.root, "Data transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun captureWebView(webView: WebView): Bitmap {
        val specWidth = View.MeasureSpec.makeMeasureSpec(webView.width, View.MeasureSpec.EXACTLY)
        val specHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        webView.measure(specWidth, specHeight)
        webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            webView.measuredWidth,
            webView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        webView.draw(canvas)

        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val cacheDir = File(requireContext().cacheDir, "receipts")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val fileName = "receipt_rental_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file
        } catch (e: Exception) {
            Log.e(TAG, "SAVE BITMAP ERROR", e)
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun hideBottomNavigationView() {
        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.VISIBLE

        try {
            val cacheDir = File(requireContext().cacheDir, "receipts")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "CACHE CLEAN ERROR", e)
        }

        _binding = null
    }
}