package dev.ferynnd.baguslaundry.ui.user

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dantsu.escposprinter.EscPosPrinterCommands
import com.dantsu.escposprinter.textparser.PrinterTextParserImg


class PrintPreviewFragment : Fragment() {

    private lateinit var  binding: FragmentPrintPreviewBinding

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
        binding = FragmentPrintPreviewBinding.inflate(inflater, container, false)
        sharePreferences = SharePrefrenceHelper(requireContext())
        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE
        binding.printButton.setOnClickListener {
                if (transactionId != null) {
                     printReceiptFromWebView()
                } else {
                    Snackbar.make(binding.root, "ID transaksi tidak tersedia", Snackbar.LENGTH_LONG).show()
                }
            }

         binding.backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, KurirTransactionReportFragment())
                .addToBackStack(null) // opsional, jika ingin bisa kembali
                .commit()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Toast.makeText(
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
                      val htmlContent = generateReceiptHtml(result.data)
                      setupReceiptPreview(htmlContent)

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



private suspend fun generateReceiptHtml(data: LaundryPrintTransaction): String {
    val idUser = sharePreferences.getString("PREF_USER_ID")
    val userData = userViewModel.getUserById(idUser?.toInt() ?: 0)
    val filterBranch = branchViewModel.branches.value
        ?.find { it.id_branch == userData.data.id_branch_user }
    val storeAddress = filterBranch?.full_address_branch ?: "Alamat tidak tersedia"

    // Ambil logo dan convert ke Base64
    val logoBitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.logobgs)
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
                    <div><b>Tanggal:</b><br>${formatDate(data.first_date_transaction_laundry)}</div>
                    <div><b>Status:</b><br>${data.status_transaction_laundry}</div>
                  </div>""")

        append("""<table class="items-table">
                  <thead><tr><th>Layanan</th><th>Harga</th></tr></thead><tbody>""")
        data.list_transaction_laundry.forEach { item ->
            val name = getLaundryServiceItemName(item.id_item_laundry)
            append("""<tr>
                        <td>${name}<br><small>${formatWeight(item.weight_list_transaction_laundry)}</small></td>
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
    binding.receiptWebView.settings.javaScriptEnabled = true
    binding.receiptWebView.settings.loadWithOverviewMode = true
    binding.receiptWebView.settings.useWideViewPort = true
    binding.receiptWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

    // tombol hanya aktif setelah WebView selesai render
    binding.receiptWebView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.printButton.isEnabled = true
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

        // Ambil screenshot dari WebView yang sudah ditampilkan di layar
        val bitmap = captureWebView(binding.receiptWebView)

        val escposPrinter = EscPosPrinter(printerConnection, 203, 57f, 32)
        val hexImage = PrinterTextParserImg.bitmapToHexadecimalString(
            escposPrinter,
            bitmap,
            false
        )
        escposPrinter.printFormattedText("[C]<img>$hexImage</img>\n")

    } catch (e: Exception) {
        Snackbar.make(binding.root, "Gagal mencetak: ${e.message}", Snackbar.LENGTH_LONG).show()
        Log.e("PrinterError", e.stackTraceToString())
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

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.VISIBLE
    }
}
