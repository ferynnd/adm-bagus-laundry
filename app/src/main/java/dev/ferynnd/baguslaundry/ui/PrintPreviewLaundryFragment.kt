package dev.ferynnd.baguslaundry.ui

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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.google.android.material.snackbar.Snackbar
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BottomNavViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentPrintPreviewBinding
import dev.ferynnd.admbaguslaundry.ui.toBranchTime
import dev.ferynnd.baguslaundry.model.LaundryPrintTransaction
import dev.ferynnd.baguslaundry.model.ProductLaundry
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class PrintPreviewLaundryFragment : Fragment() {

    private lateinit var binding: FragmentPrintPreviewBinding

    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var productLaundryViewModel: LaundryProductViewModel

    private val bottomNavViewModel : BottomNavViewModel by activityViewModels()

    private lateinit var sharePreferences: SharePrefrenceHelper

    private var transactionId: Int? = null
    private var currentTransactionData: LaundryPrintTransaction? = null
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
        binding = FragmentPrintPreviewBinding.inflate(inflater, container, false)
        sharePreferences =
            _root_ide_package_.dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper(
                requireContext()
            )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.postDelayed({
            bottomNavViewModel.hide()
        }, 300)

        binding.printButton.setOnClickListener {
            if (transactionId != null) {
                printReceiptFromWebView()
            } else {
                Snackbar.make(binding.root, "ID transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.kirimButton.setOnClickListener {
            shareReceipt()
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (transactionId == null) {
            Snackbar.make(binding.root, "ID transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
            binding.printButton.isEnabled = false
            binding.kirimButton.isEnabled = false
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            laundryReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
            productLaundryViewModel.laundryProducts.observe(viewLifecycleOwner) { list ->
                productLaundryList = list
                fetchTransactionData(transactionId!!)
            }

            productLaundryViewModel.getProductLaundryPage() // Memulai fetch product laundry
        }
    }

    private fun fetchTransactionData(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = laundryReportViewModel.getLaundryPrint(id)
                if (result.success && result.data != null) {
                    currentTransactionData = result.data // Simpan data transaksi
                    laundryReportViewModel.postPrintData(result.data)
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
                Snackbar.make(
                    binding.root,
                    "Terjadi kesalahan saat memuat data: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                binding.printButton.isEnabled = false
                binding.kirimButton.isEnabled = false
                Log.e("FetchDataError", "Error fetching transaction data: ${e.stackTraceToString()}")
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
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
        return String.Companion.format(Locale.getDefault(), "%.1f", weight)
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

    private fun generateReceiptHtml(data: LaundryPrintTransaction): String {
        // Ambil daftar cabang dari ViewModel
        val branchList = branchViewModel.branches.value.orEmpty()

        // Filter cabang sesuai ID cabang dari transaksi
        val selectedBranch = branchList.find { it.id_branch == data.id_branch_transaction_laundry }

        val storeAddress = selectedBranch?.full_address_branch ?: "Alamat tidak tersedia"
        val branchTimezone = selectedBranch?.timezone_branch ?: "Asia/Jakarta" // fallback

        val logoBitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.logo_bagus)
        val scaledLogo = Bitmap.createScaledBitmap(
            logoBitmap,
            200,
            (200.0 / logoBitmap.width * logoBitmap.height).toInt(),
            true
        )
        val logoBase64 = bitmapToBase64(scaledLogo)

        return buildString {
            append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=384, initial-scale=1.0">
                    <style>
                        * { margin:0; padding:0; box-sizing:border-box; font-family:Arial,sans-serif; }
                        body { width: 384px; background-color:white; color:black; line-height:1.4; padding:12px; margin: 0 auto; }
                        .invoice-container { width:100%; margin:0 auto; }
                        .header { text-align:center; margin-bottom:10px; border-bottom:2px solid black; padding-bottom:5px; }
                        .header img { max-width:100px; margin-bottom:5px; }
                        .invoice-info { display:grid; grid-template-columns:1fr 1fr; gap:5px; font-size:14px; margin-bottom:10px; }
                        .items-table { width:100%; border-collapse:collapse; margin-bottom:10px; font-size:14px; }
                        .items-table th { border-bottom:2px solid black; padding:5px; text-align:left; }
                        .items-table td { padding:5px; border-bottom:1px solid #ddd; }
                        .item-price { text-align:right; }
                        .summary-row { display:flex; justify-content:space-between; margin-bottom:3px; }
                        .summary-total { border-top:2px solid black; font-weight:bold; font-size:16px; padding-top:5px; margin-top:5px; }
                        .payment-info { display:grid; grid-template-columns:1fr 1fr; gap:5px; margin-bottom:10px; }
                        .payment-item { border:1px solid black; padding:5px; }
                        .notes { border:1px solid black; padding:5px; margin-bottom:10px; font-size:12px; }
                        .footer { text-align:center; border-top:2px solid black; font-weight:bold; padding-top:5px; }
                    </style>
                </head>
                <body>
            """.trimIndent())

            append("""<div class="invoice-container">""")
            append("""<div class="header">
                          <img src="data:image/png;base64,$logoBase64"/>
                          <p>Telp/WA : 082329197772</p>
                      </div>""")

            append("""<div class="invoice-info">
                        <div><b>No. Invoice:</b><br>${data.number_transaction_laundry}</div>
                        <div><b>Nama:</b><br>${data.name_client_transaction_laundry.uppercase()}</div>
                        <div><b>Tanggal:</b><br>${data.first_date_transaction_laundry.toBranchTime(branchTimezone)}</div>
                        <div><b>Status:</b><br>${data.status_transaction_laundry}</div>
                      </div>""")

            append("""<table class="items-table">
                      <thead><tr><th>Layanan</th><th>Harga</th></tr></thead><tbody>""")
            data.list_transaction_laundry.forEach { item ->
                val name = getLaundryServiceItemName(item.id_item_laundry)
                append("""<tr>
                            <td>${name}<br><small>${formatWeight(item.weight_list_transaction_laundry)} Kg</small></td>
                            <td class="item-price">${formatCurrency(item.total_price_list_transaction_laundry)}</td>
                         </tr>""")
            }
            append("</tbody></table>")

            append("""<div class="summary">
                        <div class="summary-row"><span>Subtotal</span><span>${formatCurrency(data.total_price_transaction_laundry)}</span></div>
                        <div class="summary-row"><span>Promo</span><span>- ${formatCurrency(data.promo_transaction_laundry)}</span></div>
                        <div class="summary-row"><span>Biaya Tambahan</span><span>${formatCurrency(data.additional_cost_transaction_laundry)}</span></div>
                        <div class="summary-row summary-total"><span>Total</span><span>${formatCurrency(data.total_transaction_laundry)}</span></div>
                      </div>""")

            append("""<div class="payment-info">
                        <div class="payment-item"><b>Tunai</b><br>${formatCurrency(data.cash_transaction_laundry)}</div>
                        <div class="payment-item"><b>Kembalian</b><br>${formatCurrency(data.change_money_transaction_laundry)}</div>
                      </div>""")

            if (!data.notes_transaction_laundry.isNullOrBlank()) {
                append("""<div class="notes"><b>Catatan:</b> ${data.notes_transaction_laundry}</div>""")
            }

            append("""<div class="notes">
                        <b>PERHATIAN:</b><br>
                        1 Cucian rusak karena sifat bahan/kain bukan tanggung jawab kami<br>
                        2 Cucian luntur yang tidak diberitahukan kepada kami diluar tanggung jawab kami<br>
                        3 Apabila konsumen tidak menghitung cucian, jumlah yang kami hitung kami anggap benar<br>
                        4 Pengajuan klaim tidak lebih dari 24 jam setelah diterima<br>
                        5 Benda berharga/barang yang tertinggal dalam cucian apabila hilang/rusak bukan tanggung jawab kami<br>
                        6 Barang yang tidak diambil lebih dari 1 bulan bukan tanggung jawab kami
                      </div>""")

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

        // Set initial scale pada WebView langsung (bukan di WebSettings)
        binding.receiptWebView.setInitialScale(100)

        binding.receiptWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

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
            Log.e("PrinterError", e.stackTraceToString())
        }
    }


    private fun generateReceiptEscPosText(
        data: LaundryPrintTransaction
    ): String {
        val branchList = branchViewModel.branches.value.orEmpty()
        val selectedBranch = branchList.find {
            it.id_branch == data.id_branch_transaction_laundry
        }

        val branchTimezone = selectedBranch?.timezone_branch ?: "Asia/Jakarta"

        val invoiceNumber = safeEscPos(data.number_transaction_laundry?.toString() ?: "-")
        val clientName = safeEscPos(data.name_client_transaction_laundry.uppercase())
        val transactionDate = safeEscPos(
            data.first_date_transaction_laundry.toBranchTime(branchTimezone)
        )
        val status = safeEscPos(data.status_transaction_laundry ?: "-")

        val subtotal = formatCurrency(data.total_price_transaction_laundry)
        val promo = formatCurrency(data.promo_transaction_laundry)
        val additionalCost = formatCurrency(data.additional_cost_transaction_laundry)
        val total = formatCurrency(data.total_transaction_laundry)
        val cash = formatCurrency(data.cash_transaction_laundry)
        val change = formatCurrency(data.change_money_transaction_laundry)

        val SEP = "[C]------------------------------\n"
        val SEPL = "[C]==============================\n"

        return buildString {
            append("[C]BAGUS LAUNDRY\n")
            append("[C]Telp/WA : 082329197772\n")
            append(SEP)
            append("[C]STRUK LAUNDRY\n")
            append(SEP)

            append("[L]No Inv  : $invoiceNumber\n")
            append("[L]Nama    : ${limitText(clientName, 20)}\n")
            append("[L]Status  : ${limitText(status, 20)}\n")

            wrapText("Tanggal : $transactionDate", 30).forEachIndexed { i, line ->
                if (i == 0) append("[L]$line\n")
                else append("[L]          $line\n")
            }

            append(SEP)
            append("[C]DAFTAR LAYANAN\n")
            append(SEP)

            data.list_transaction_laundry.forEachIndexed { index, item ->
                val itemName = safeEscPos(getLaundryServiceItemName(item.id_item_laundry))
                val weight = formatWeight(item.weight_list_transaction_laundry)
                val price = formatCurrency(item.total_price_list_transaction_laundry)

                wrapText("${index + 1}. $itemName", 30).forEach { line ->
                    append("[L]$line\n")
                }

                append("[L]   Berat : $weight kg\n")
                append("[L]   Harga : $price\n")
                append(SEP)
            }

            append("[C]RINGKASAN\n")
            append(SEP)
            appendPriceLine("Subtotal", subtotal)
            appendPriceLine("Promo", "- $promo")
            appendPriceLine("Biaya Tambahan", additionalCost)
            append(SEPL)
            appendPriceLine("TOTAL", total)
            append(SEPL)

            append("[C]PEMBAYARAN\n")
            append(SEP)
            appendPriceLine("Tunai", cash)
            appendPriceLine("Kembalian", change)
            append(SEP)

            if (!data.notes_transaction_laundry.isNullOrBlank()) {
                append("[L]Catatan :\n")
                wrapText(data.notes_transaction_laundry, 28).forEach { line ->
                    append("[L]  $line\n")
                }
                append(SEP)
            }

            append("[L]PERHATIAN:\n")
            val rules = listOf(
                "1 Cucian rusak karena sifat bahan/kain bukan tanggung jawab kami",
                "2 Cucian luntur yang tidak diberitahukan kepada kami diluar tanggung jawab kami",
                "3 Apabila konsumen tidak menghitung cucian, jumlah yang kami hitung kami anggap benar",
                "4 Pengajuan klaim tidak lebih dari 24 jam setelah diterima",
                "5 Benda berharga/barang yang tertinggal dalam cucian apabila hilang/rusak bukan tanggung jawab kami",
                "6 Barang yang tidak diambil lebih dari 1 bulan bukan tanggung jawab kami"
            )
            rules.forEach { rule ->
                wrapText(rule, 30).forEach { line ->
                    append("[L]$line\n")
                }
            }
            append(SEP)
            append("[C]TERIMA KASIH\n")
            append(SEP)
        }
    }

    /**
     * Helper: cetak "Label   : Nilai" 1 baris jika muat,
     * atau wrap nilai ke baris berikutnya dengan indent.
     * Label max 9 char + " : " (3) = 12 char prefix → nilai max 20 char per baris.
     */

    private fun StringBuilder.appendPriceLine(label: String, value: String) {
        val cleanLabel = safeEscPos(label)
        val cleanValue = safeEscPos(value)

        val maxWidth = 30
        val labelWidth = 14
        val valueWidth = maxWidth - labelWidth

        append("[L]")
        append(cleanLabel.take(labelWidth).padEnd(labelWidth))
        append(cleanValue.take(valueWidth).padStart(valueWidth))
        append("\n")
    }

    /**
     * Membersihkan karakter yang bisa merusak format ESC/POS.
     */

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
        return if (text.length > maxLength) {
            text.take(maxLength)
        } else {
            text
        }
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
                // Ambil daftar cabang dari ViewModel
                val branchList = branchViewModel.branches.value.orEmpty()
                // Filter cabang sesuai ID cabang dari transaksi
                val selectedBranch = branchList.find { it.id_branch == data.id_branch_transaction_laundry }
                val branchTimezone = selectedBranch?.timezone_branch ?: "Asia/Jakarta" // fallback
                // Tampilkan loading
                binding.progressBar.visibility = View.VISIBLE
                binding.kirimButton.isEnabled = false

                // Capture screenshot dari WebView
                val bitmap = captureWebView(binding.receiptWebView)

                // Simpan bitmap ke file
                val imageFile = saveBitmapToFile(bitmap)

                if (imageFile == null) {
                    Snackbar.make(binding.root, "Gagal menyimpan gambar struk", Snackbar.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.kirimButton.isEnabled = true
                    return
                }

                // Buat URI dari file menggunakan FileProvider
                val imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    imageFile
                )

                // Buat pesan teks yang informatif
                val message = buildString {
                    append("Halo,\n\n")
                    append("Berikut adalah struk transaksi laundry:\n\n")
                    append("📋 No. Invoice: ${data.number_transaction_laundry}\n")
                    append("👤 Nama: ${data.name_client_transaction_laundry}\n")
                    append("📅 Tanggal: ${formatDate(data.first_date_transaction_laundry.toBranchTime(branchTimezone))}\n")
                    append("💰 Total: ${formatCurrency(data.total_transaction_laundry)}\n")
                    append("📊 Status: ${data.status_transaction_laundry}\n\n")
                    append("Terima kasih telah menggunakan layanan kami. 🙏")
                }

                // Buat intent untuk share (tanpa spesifik ke WhatsApp)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra(Intent.EXTRA_SUBJECT, "Struk Transaksi Laundry - ${data.number_transaction_laundry}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // Buat chooser agar user bisa pilih aplikasi
                val chooserIntent = Intent.createChooser(shareIntent, "Bagikan struk melalui")

                try {
                    startActivity(chooserIntent)
                    Snackbar.make(binding.root, "Pilih aplikasi untuk membagikan struk", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Tidak ada aplikasi yang tersedia untuk membagikan", Snackbar.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Gagal membagikan struk: ${e.message}", Snackbar.LENGTH_LONG).show()
                Log.e("ShareError", e.stackTraceToString())
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.kirimButton.isEnabled = true
            }
        } ?: run {
            Snackbar.make(binding.root, "Data transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Fungsi untuk capture WebView untuk keperluan share/preview
     */
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
            // Buat folder cache jika belum ada
            val cacheDir = File(requireContext().cacheDir, "receipts")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Buat file dengan nama unik
            val fileName = "receipt_laundry_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, fileName)

            // Simpan bitmap ke file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file
        } catch (e: Exception) {
            Log.e("SaveFileError", "Error saving bitmap to file: ${e.message}")
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNavViewModel.show()

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
            Log.e("CleanupError", "Error cleaning up cache files: ${e.message}")
        }
    }
}