package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.google.android.material.snackbar.Snackbar
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentPrintPreviewRentalBinding
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.RentalPrintTransaction
import dev.ferynnd.baguslaundry.ui.user.KurirTransactionReportFragment
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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
    private var currentTransactionData: RentalPrintTransaction? = null
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
            printReceiptFromWebView()
        }

        binding.kirimButton.setOnClickListener {
            shareReceipt()
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, KurirTransactionReportFragment())
                .addToBackStack(null)
                .commit()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (transactionId == null) {
            Snackbar.make(binding.root, "ID transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
            binding.printButton.isEnabled = false
            binding.kirimButton.isEnabled = false
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { list ->
                productRentalList = list
                fetchTransactionData(transactionId!!)
            }
            rentalProductViewModel.getProductRental()
        }
    }

    private fun fetchTransactionData(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = rentalReportViewModel.getRentalPrint(id)
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
                Snackbar.make(
                    binding.root,
                    "Terjadi kesalahan saat memuat data: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                binding.printButton.isEnabled = false
                binding.kirimButton.isEnabled = false
                Log.e(
                    "FetchDataError",
                    "Error fetching transaction data: ${e.stackTraceToString()}"
                )
            }
        }
    }

    private suspend fun generateReceiptHtml(data: RentalPrintTransaction): String {
        val idUser = sharePreferences.getString("PREF_USER_ID")
        val userData = userViewModel.getUserById(idUser?.toInt() ?: 0)
        val filterBranch = branchViewModel.branches.value
            ?.find { it.id_branch == userData.data.id_branch_user }
        val storeAddress = filterBranch?.full_address_branch ?: "Alamat tidak tersedia"

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
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        * { margin:0; padding:0; box-sizing:border-box; font-family:Arial,sans-serif; }
                        body { background-color:white; color:black; line-height:1.4; padding:10px; }
                        .invoice-container { width:100%; margin:0 auto; }
                        .header { text-align:center; margin-bottom:10px; border-bottom:2px solid black; padding-bottom:5px; }
                        .header img { max-width:100px; margin-bottom:5px; }
                        .invoice-info { display:grid; grid-template-columns:1fr 1fr; gap:5px; font-size:14px; margin-bottom:10px; }
                        .items-table { width:100%; border-collapse:collapse; margin-bottom:10px; font-size:14px; }
                        .items-table th { border-bottom:2px solid black; padding:5px; text-align:left; }
                        .items-table td { padding:5px; border-bottom:1px solid #ddd; }
                        .item-name { font-weight:bold; }
                        .item-details { font-size:12px; color:#666; margin-top:2px; }
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
                        <div><b>No. Invoice:</b><br>${data.number_transaction_rental}</div>
                        <div><b>Klien:</b><br>${getClientName(data.id_client_transaction_rental!!)}</div>
                        <div><b>Penerima:</b><br>${data.recipient_name_transaction_rental?.uppercase(Locale.getDefault())}</div>
                        <div><b>Tanggal:</b><br>${formatDate(data.time_transaction_rental.toString())}</div>
                      </div>""")

            append("""<table class="items-table">
                      <thead><tr><th>Layanan</th><th>Berat</th><th>Jumlah</th></tr></thead><tbody>""")
            data.list_transaction_rentals.forEach { item ->
                val name = getRentalServiceItemName(item.id_item_rental!!)
                val condition = translateCondition(item.condition_list_transaction_rental)
                val status = translateItemStatus(item.status_list_transaction_rental)

                append("""<tr>
                            <td>
                                <div class="item-name">${name}</div>
                                <div class="item-details">${condition} - ${status}</div>
                            </td>
                            <td>${formatWeight(item.weight_list_transaction_rental)} Kg</td>
                            <td>${item.count_list_transaction_rental} PCS</td>
                         </tr>""")
            }
            append("</tbody></table>")

            if (!data.notes_transaction_rental.isNullOrBlank()) {
                append("""<div class="notes"><b>Catatan:</b> ${data.notes_transaction_rental}</div>""")
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
        val clients = clientViewModel.clients.value ?: return "Layanan Tidak Dikenal ($itemId)"
        return clients.find { it.id_client == itemId }?.name_client ?: "Layanan Tidak Dikenal ($itemId)"
    }

    private fun getClientData(clientId: Int): Client? {
        return clientViewModel.clients.value?.find { it.id_client == clientId }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupReceiptPreview(htmlContent: String) {
        binding.receiptWebView.settings.javaScriptEnabled = true
        binding.receiptWebView.settings.loadWithOverviewMode = true
        binding.receiptWebView.settings.useWideViewPort = true
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
            val printerConnection: DeviceConnection? =
                BluetoothPrintersConnections.selectFirstPaired()

            if (printerConnection == null) {
                Snackbar.make(binding.root, "Tidak ada printer yang terhubung", Snackbar.LENGTH_LONG).show()
                return
            }

            // Capture WebView untuk print dengan ukuran penuh
            val bitmap = captureWebViewForPrint(binding.receiptWebView)

            val escposPrinter = EscPosPrinter(printerConnection, 203, 57f, 1)

            // Resize bitmap agar sesuai lebar kertas printer thermal 58mm
            // 58mm dengan 203 DPI = sekitar 384 pixel
            val printerWidthPx = 384
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                printerWidthPx,
                (bitmap.height.toFloat() / bitmap.width.toFloat() * printerWidthPx).toInt(),
                true
            )

            val hexImage = PrinterTextParserImg.bitmapToHexadecimalString(
                escposPrinter,
                scaledBitmap,
                false
            )
            escposPrinter.printFormattedText("[C]<img>$hexImage</img>\n")

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Gagal mencetak: ${e.message}", Snackbar.LENGTH_LONG).show()
            Log.e("PrinterError", e.stackTraceToString())
        }
    }

    /**
     * Fungsi untuk capture WebView dengan ukuran penuh untuk keperluan print
     */
    private fun captureWebViewForPrint(webView: WebView): Bitmap {
        // Simpan ukuran layout saat ini
        val originalWidth = webView.width
        val originalHeight = webView.height

        // Ukur WebView dengan ukuran penuh kontennya
        val widthSpec = View.MeasureSpec.makeMeasureSpec(webView.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        webView.measure(widthSpec, heightSpec)

        // Layout dengan ukuran penuh
        webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)

        // Buat bitmap dengan ukuran penuh dari konten WebView
        val bitmap = Bitmap.createBitmap(
            webView.measuredWidth,
            webView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        webView.draw(canvas)

        // Kembalikan layout ke ukuran semula
        webView.layout(0, 0, originalWidth, originalHeight)

        return bitmap
    }

    private fun shareReceipt() {
        currentTransactionData?.let { data ->
            try {
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

                // Ambil data klien untuk informasi di pesan
                val client = getClientData(data.id_client_transaction_rental!!)

                // Buat pesan teks yang informatif
                val message = buildString {
                    append("Halo,\n\n")
                    append("Berikut adalah struk transaksi rental:\n\n")
                    append("📋 No. Invoice: ${data.number_transaction_rental}\n")
                    append("👤 Klien: ${client?.name_client ?: "Tidak tersedia"}\n")
                    append("📅 Tanggal: ${formatDate(data.time_transaction_rental.toString())}\n\n")
                    append("Terima kasih telah menggunakan layanan kami. 🙏")
                }

                // Buat intent untuk share (tanpa spesifik ke WhatsApp)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra(Intent.EXTRA_SUBJECT, "Struk Transaksi Rental - ${data.number_transaction_rental}")
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
            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
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

        // Hapus file cache struk yang sudah tidak dipakai
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

        _binding = null
    }
}
