package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.RentalReportAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.ListTransactionReportRentalViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListReportRentalBinding
import dev.ferynnd.baguslaundry.model.*
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import dev.ferynnd.baguslaundry.ui.openAdminFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class AdminListReportRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminListReportRentalBinding
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var rentalReportAdapter: RentalReportAdapter
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var listTransactionReportRentalViewModel: ListTransactionReportRentalViewModel

    private var selectedFilterBranch: Branch? = null
    private var selectedFilterClient: Client? = null
    private var selectedFilterMonth: String? = null
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        rentalReportViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        branchViewModel.init(requireContext())
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java].apply { init(requireContext()) }
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java].apply { init(requireContext()) }
        listTransactionReportRentalViewModel = ViewModelProvider(this)[ListTransactionReportRentalViewModel::class.java]
        listTransactionReportRentalViewModel.init(requireContext())

        // ✅ TAMBAH DI AKHIR onCreate()
        selectedFilterBranch = rentalReportViewModel.currentFilterBranch
        selectedFilterClient = rentalReportViewModel.currentFilterClient
        selectedFilterMonth = rentalReportViewModel.currentFilterMonth

        Log.d("RentalFragment", "🔄 Restored filter - Branch: ${selectedFilterBranch?.name_branch}, Client: ${selectedFilterClient?.name_client}, Month: $selectedFilterMonth")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListReportRentalBinding.inflate(layoutInflater)

        rentalReportAdapter = RentalReportAdapter(
            onDetail = { transactionReport -> onDetail(transactionReport) },
            onEdit = { transactionReport -> showEditDialog(transactionReport) },
            onDelete = { transactionReport -> showDeleteDialog(transactionReport) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalReportAdapter
        }

        Log.d("RentalRepot", "🔄 Initializing and fetching rental reports...")
        viewLifecycleOwner.lifecycleScope.launch {
            rentalReportViewModel.getReportRental()

            delay(500)
            if (selectedFilterClient != null) {
                Log.d("RentalFragment", "🎯 Re-applying saved filter on init...")
                filterReports(selectedFilterBranch, selectedFilterClient, selectedFilterMonth)
            }
        }

        setupObservers()
        setupClickListeners()

        return binding.root
    }

    private fun setupClickListeners() {
        binding.btnRoutes.setOnClickListener {
            showFilterBottomSheet(requireContext()) { selectedBranch, selectedClient, selectedMonth ->
                filterReports(selectedBranch, selectedClient, selectedMonth)
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { rentalReportViewModel.searchRentalReports(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                rentalReportViewModel.searchRentalReports(newText.orEmpty())
                return true
            }
        })

        binding.iconExel.setOnClickListener {
            if (selectedFilterClient == null) {
                Toast.makeText(requireContext(), "Silakan pilih klien terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCetakDialog(requireContext())
        }

        binding.iconPdf.setOnClickListener {
            if (selectedFilterClient == null) {
                Toast.makeText(requireContext(), "Silakan pilih klien terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListInvoiceRentalFragment())
                .commit()
        }

        binding.arrowBack.setOnClickListener {
            openAdminFragment(AdminDashboardFragment(), "AdminDashboard")
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                    Log.d("RentalRepot", "⏳ Loading state: $isLoading")
                    binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
                }

                rentalReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                    if (errorMessage.isNotBlank()) {
                        Log.e("RentalRepot", "❌ Error: $errorMessage")
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        rentalReportViewModel.resetErrorMessage()
                    }
                }

                rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { products ->
                    products?.let {
                        Log.d("RentalRepot", "📦 Rental products loaded: ${it.size}")
                        rentalReportAdapter.setRentalProducts(it)
                    }
                }

                // ✅ FIX 2: Gunakan hanya filteredRentalReports untuk display
                rentalReportViewModel.filteredRentalReports.observe(viewLifecycleOwner) { filteredReports ->
                    Log.d("RentalRepot", "📋 Filtered reports received: ${filteredReports.size}")
                    Log.d("RentalRepot", "👤 Selected client: ${selectedFilterClient?.name_client}")

                    if (selectedFilterClient != null && filteredReports.isNotEmpty()) {
                        setReportRental(filteredReports)
                    } else if (selectedFilterClient != null) {
                        // Client dipilih tapi tidak ada data
                        Log.w("RentalRepot", "⚠️ Client selected but no data")
                        rentalReportAdapter.submitList(emptyList())
                    } else {
                        // Belum pilih client
                        Log.d("RentalRepot", "ℹ️ No client selected yet")
                        rentalReportAdapter.submitList(emptyList())
                    }
                }

                // ✅ Observer untuk raw data (hanya untuk logging)
                rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { reports ->
                    Log.d("RentalRepot", "📊 Raw reports loaded: ${reports.size}")
                    
                    if (selectedFilterClient != null) {
                        Log.d("RentalFragment", "🔄 Auto re-applying filter...")
                        filterReports(selectedFilterBranch, selectedFilterClient, selectedFilterMonth)
                    }
                }

                branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                    Log.d("RentalRepot", "🏢 Branches loaded: ${branches.size}")
                    rentalReportAdapter.setBranches(branches)
                    rentalReportViewModel.setBranches(branches)

                    if (isFirstLoad && branches.isNotEmpty() && selectedFilterClient == null) {
                        isFirstLoad = false
                        // ✅ FIX 3: Delay kecil untuk memastikan data ready
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(500)
                            Log.d("RentalRepot", "🎯 Showing filter dialog...")
                            showFilterBottomSheet(requireContext()) { selectedBranch, selectedClient, selectedMonth ->
                                filterReports(selectedBranch, selectedClient, selectedMonth)
                            }
                        }
                    }
                }

                userViewModel.users.observe(viewLifecycleOwner) { users ->
                    Log.d("RentalRepot", "👥 Users loaded: ${users.size}")
                    rentalReportAdapter.setSender(users)
                    rentalReportViewModel.setUsers(users)
                }

                clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
                    Log.d("RentalRepot", "🏪 Clients loaded: ${clients.size}")
                    rentalReportAdapter.setClient(clients)
                    rentalReportViewModel.setClient(clients)
                }

                listTransactionReportRentalViewModel.listTransactionRentalReports.observe(viewLifecycleOwner) { listTransaksi ->
                    Log.d("RentalRepot", "📝 List transactions loaded: ${listTransaksi.size}")
                    rentalReportAdapter.setRentalList(listTransaksi)
                }

                // Observer untuk response update
                rentalReportViewModel.updateTransactionResponse.observe(viewLifecycleOwner) { response ->
                    response?.let {
                        if (it.success) {
                            Log.d("RentalRepot", "✅ Update success")
                            Toast.makeText(requireContext(), "Transaksi berhasil diupdate", Toast.LENGTH_SHORT).show()
                            rentalReportViewModel.resetUpdateTransactionResponse()
                        } else {
                            Log.e("RentalRepot", "❌ Update failed: ${it.message}")
                            Toast.makeText(requireContext(), "Gagal update: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Observer untuk response delete
                rentalReportViewModel.deleteTransactionResponse.observe(viewLifecycleOwner) { response ->
                    response?.let {
                        if (it.success) {
                            Log.d("RentalRepot", "✅ Delete success")
                            Toast.makeText(requireContext(), "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                            rentalReportViewModel.resetDeleteTransactionResponse()
                        } else {
                            Log.e("RentalRepot", "❌ Delete failed: ${it.message}")
                            Toast.makeText(requireContext(), "Gagal hapus: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RentalRepot", "❌ Error in observers", e)
                throw e
            }
        }
    }

    private fun showEditDialog(report: ReportRental) {
        Log.d("RentalRepot", "✏️ Opening edit dialog for: ${report.number_transaction_rental}")
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_rental)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val inputNumber = dialog.findViewById<TextInputEditText>(R.id.inputNumberTransaction)
        val inputRecipient = dialog.findViewById<TextInputEditText>(R.id.inputRecipientName)
        val inputNotes = dialog.findViewById<TextInputEditText>(R.id.inputNotes)
        val buttonSave = dialog.findViewById<Button>(R.id.buttonSave)
        val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)

        inputNumber.setText(report.number_transaction_rental.toString())
        inputRecipient.setText(report.recipient_name_transaction_rental)
        inputNotes.setText(report.notes_transaction_rental)

        buttonSave.setOnClickListener {
            val numberText = inputNumber.text.toString()
            val recipientText = inputRecipient.text.toString()
            val notesText = inputNotes.text.toString()

            if (numberText.isBlank()) {
                Toast.makeText(requireContext(), "Nomor transaksi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateRequest = UpdateRentalTransactionRequest(
                id_kurir_transaction_rental = report.id_kurir_transaction_rental ?: 0,
                id_branch_transaction_rental = report.id_branch_transaction_rental ?: 0,
                id_client_transaction_rental = report.id_client_transaction_rental ?: 0,
                recipient_name_transaction_rental = recipientText.ifBlank { null },
                number_transaction_rental = numberText.toIntOrNull() ?: 0,
                notes_transaction_rental = notesText.ifBlank { null }
            )

            Log.d("RentalRepot", "💾 Updating transaction: ${report.id_transaction_rental}")
            viewLifecycleOwner.lifecycleScope.launch {
                rentalReportViewModel.updateRentalTransaction(
                    report.id_transaction_rental ?: 0,
                    updateRequest
                )
            }

            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog(report: ReportRental) {
        Log.d("RentalRepot", "🗑️ Opening delete dialog for: ${report.number_transaction_rental}")
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_delete_rental)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val textTransactionNumber = dialog.findViewById<TextView>(R.id.textTransactionNumber)
        val textRecipient = dialog.findViewById<TextView>(R.id.textRecipient)
        val buttonSoftDelete = dialog.findViewById<Button>(R.id.buttonSoftDelete)
        val buttonPermanentDelete = dialog.findViewById<Button>(R.id.buttonPermanentDelete)
        val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)

        textTransactionNumber.text = "No. Transaksi: ${report.number_transaction_rental}"
        textRecipient.text = "Penerima: ${report.recipient_name_transaction_rental ?: "-"}"

        buttonSoftDelete.setOnClickListener {
            Log.d("RentalRepot", "📦 Soft deleting transaction: ${report.id_transaction_rental}")
            viewLifecycleOwner.lifecycleScope.launch {
                rentalReportViewModel.deleteRentalTransaction(report.id_transaction_rental ?: 0)
            }
            dialog.dismiss()
        }

        buttonPermanentDelete.setOnClickListener {
            val confirmDialog = Dialog(requireContext())
            val confirmView = LayoutInflater.from(requireContext()).inflate(
                R.layout.dialog_confirm_permanent_delete, null
            )
            confirmDialog.setContentView(confirmView)
            confirmDialog.window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            confirmView.findViewById<Button>(R.id.buttonConfirmYes)?.setOnClickListener {
                Log.d("RentalRepot", "⚠️ Force deleting transaction: ${report.id_transaction_rental}")
                viewLifecycleOwner.lifecycleScope.launch {
                    rentalReportViewModel.forceDeleteRentalTransaction(report.id_transaction_rental ?: 0)
                }
                confirmDialog.dismiss()
                dialog.dismiss()
            }

            confirmView.findViewById<Button>(R.id.buttonConfirmNo)?.setOnClickListener {
                confirmDialog.dismiss()
            }

            confirmDialog.show()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun onDetail(productRental: ReportRental) {
        val bundle = Bundle().apply {
            putInt("transactionRentalID", productRental.id_transaction_rental ?: 0)
        }

        val detailTransactionReport = AdminDetailListReportRentalFragment()
        detailTransactionReport.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_admin, detailTransactionReport)
            .addToBackStack(null)
            .commit()
    }

    private fun setReportRental(newReportRentals: List<ReportRental>) {
        Log.d("RentalRepot", "📦 Setting report rental, input size: ${newReportRentals.size}")
        val tempGroupedData = mutableListOf<Any>()

        if (newReportRentals.isEmpty()) {
            Log.w("RentalRepot", "⚠️ No reports to display")
            rentalReportAdapter.submitList(tempGroupedData)
            return
        }

        val localeID = Locale("id", "ID")
        val parser = SimpleDateFormat("yyyy-MM-dd", localeID)
        val monthFormatter = SimpleDateFormat("MMMM yyyy", localeID)
        val monthKeyFormatter = SimpleDateFormat("yyyy-MM", localeID)

        val sortedReports = newReportRentals.sortedByDescending { it.time_transaction_rental }

        val groupedByMonth: Map<String, List<ReportRental>> = sortedReports.groupBy { report ->
            report.time_transaction_rental?.let { dateStr ->
                try {
                    val date = parser.parse(dateStr)
                    monthKeyFormatter.format(date ?: Date())
                } catch (e: Exception) {
                    Log.e("RentalRepot", "Error parsing date: $dateStr", e)
                    "0000-00"
                }
            } ?: "0000-00"
        }

        val sortedMonthKeys = groupedByMonth.keys.sortedDescending()

        for (monthKey in sortedMonthKeys) {
            val readableMonth = try {
                val date = monthKeyFormatter.parse(monthKey)
                monthFormatter.format(date ?: Date())
            } catch (e: Exception) {
                "Unknown Date"
            }

            tempGroupedData.add(readableMonth)
            tempGroupedData.addAll(groupedByMonth[monthKey] ?: emptyList())
        }

        Log.d("RentalRepot", "📦 Grouped data size: ${tempGroupedData.size}")
        rentalReportAdapter.submitList(tempGroupedData)
    }

    private fun filterReports(selectedBranch: Branch?, selectedClient: Client?, selectedMonth: String?) {
        Log.d("RentalRepot", "🔍 Filtering reports - Branch: ${selectedBranch?.name_branch}, Client: ${selectedClient?.name_client}, Month: $selectedMonth")

        selectedFilterBranch = selectedBranch
        selectedFilterClient = selectedClient
        selectedFilterMonth = selectedMonth

        if (selectedClient == null) {
            Log.w("RentalRepot", "⚠️ No client selected")
            rentalReportAdapter.submitList(emptyList())
            return
        }

        // ✅ FIX: Langsung filter dengan data yang ada, tidak perlu cek kosong
        val clientId = selectedClient.id_client
        val monthYear = selectedMonth?.let { convertMonthYearToFormat(it) }

        Log.d("RentalRepot", "🎯 Applying filter - ClientID: $clientId, MonthYear: $monthYear")
        rentalReportViewModel.filterByClientAndMonth(clientId, monthYear)
        rentalReportViewModel.setCurrentFilter(selectedBranch, selectedClient, selectedMonth)
    }

    private fun convertMonthYearToFormat(monthYearString: String): String {
        return try {
            val parts = monthYearString.split(" - ")
            if (parts.size == 2) {
                val monthName = parts[0]
                val year = parts[1]
                val months = listOf(
                    "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
                    "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
                )
                val monthNumber = months.indexOf(monthName) + 1
                val formattedMonth = String.format("%02d", monthNumber)
                "$year-$formattedMonth"
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("RentalRepot", "Error converting month-year: $monthYearString", e)
            ""
        }
    }

    private fun showCetakDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_export_excle_rental)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setCancelable(true)

        val spinnerCalendar = dialog.findViewById<Spinner>(R.id.selectCalender)
        val spinnerBranch = dialog.findViewById<Spinner>(R.id.selectBranch)
        val spinnerDescription = dialog.findViewById<Spinner>(R.id.selectDescription)
        val inputNote = dialog.findViewById<TextInputEditText>(R.id.inputTex)
        val inputStock = dialog.findViewById<TextInputEditText>(R.id.inputTextStock)
        val addButton = dialog.findViewById<ImageView>(R.id.buttonAdd)
        val descriptionText = dialog.findViewById<TextView>(R.id.inputDescription)
        val cetakButton = dialog.findViewById<Button>(R.id.buttonCetak)
        val backButton = dialog.findViewById<Button>(R.id.buttonBack)

        val notesList = mutableListOf<String>()
        val branchList = mutableListOf<Branch>()
        val productList = mutableListOf<ProductRental>()

        val months = listOf(
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        )

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val items = mutableListOf<String>()
        for (year in 2025..currentYear) {
            val maxMonth = if (year == currentYear) currentMonth else 11
            for (monthIndex in 0..maxMonth) {
                items.add("${months[monthIndex]} - $year")
            }
        }

        spinnerCalendar.adapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_item, items).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val currentItem = "${months[currentMonth]} - $currentYear"
        spinnerCalendar.setSelection(items.indexOf(currentItem))

        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            branchList.clear()
            branchList.addAll(branches)

            val branchNames = branches.map { it.name_branch }
            spinnerBranch.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                branchNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            spinnerBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val selectedBranch = branches[position]
                    rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { products ->
                        val filteredProducts = products.filter { product ->
                            product.id_branch_rental_item == selectedBranch.id_branch
                        }

                        productList.clear()
                        productList.addAll(filteredProducts)

                        val productNames = filteredProducts.map { it.name_rental_item }
                        spinnerDescription.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            productNames
                        ).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    spinnerDescription.adapter = null
                }
            }
        }

        addButton.setOnClickListener {
            val note = inputNote.text.toString().trim()
            if (note.isNotEmpty()) {
                notesList.add(note)
                inputNote.text?.clear()
                descriptionText.text =
                    notesList.mapIndexed { i, v -> "${i + 1}. $v" }.joinToString("\n")
            }
        }

        cetakButton.setOnClickListener {
            val selectedDate = spinnerCalendar.selectedItem.toString()
            val selectedBranch = spinnerBranch.selectedItem.toString()
            val selectedDescription = spinnerDescription.selectedItem.toString()
            val stock = inputStock.text.toString().toInt()

            if (selectedDate.isEmpty() || selectedBranch.isEmpty()) {
                Toast.makeText(context, "Pastikan semua pilihan telah dipilih.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val (monthName, year) = selectedDate.split(" - ")
            val monthNumber = months.indexOf(monthName) + 1
            val formattedMonth = String.format("%02d", monthNumber)
            val formattedDate = "$year-$formattedMonth"

            val productRental = productList.find { it.name_rental_item == selectedDescription }?.id_rental_item ?: 0
            val branchId = branchList.find { it.name_branch == selectedBranch }?.id_branch ?: 0

            val requestData = ExportReportRental(
                month = formattedDate,
                location = branchId,
                id_item_rental = productRental,
                notes = notesList,
                initial_stock = stock,
            )

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = rentalReportViewModel.exportRentalMonthly(requestData)
                    if (response.success) {
                        val downloadUrl = response.data?.download_url.orEmpty()
                        if (downloadUrl.isNotEmpty()) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                            )
                            Toast.makeText(context, "File berhasil dibuat dan sedang diunduh.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Download URL kosong.", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "Gagal cetak laporan.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        backButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showFilterBottomSheet(
        context: Context,
        onFilterSelected: (Branch?, Client?, String?) -> Unit
    ) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_filter_rental_report, null)

        val spinnerMonthYear = view.findViewById<AutoCompleteTextView>(R.id.inputBulanTahun)
        val spinnerBranch = view.findViewById<AutoCompleteTextView>(R.id.inputNamaCabang)
        val spinnerClient = view.findViewById<AutoCompleteTextView>(R.id.inputNamaClient)
        val applyButton = view.findViewById<Button>(R.id.buttonApply)
        val exitButton = view.findViewById<ImageView>(R.id.btnExit)

        var tempSelectedBranch: Branch? = selectedFilterBranch
        var tempSelectedClient: Client? = selectedFilterClient
        var tempSelectedMonth: String? = selectedFilterMonth

        selectedFilterBranch?.let { branch ->
            spinnerBranch.setText(branch.name_branch, false)
        }
        selectedFilterClient?.let { client ->
            spinnerClient.setText(client.name_client, false)
        }
        selectedFilterMonth?.let { month ->
            spinnerMonthYear.setText(month, false)
        }

        val months = listOf(
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        )

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val monthYearItems = mutableListOf<String>()
        for (year in 2025..currentYear) {
            val maxMonth = if (year == currentYear) currentMonth else 11
            for (monthIndex in 0..maxMonth) {
                monthYearItems.add("${months[monthIndex]} - $year")
            }
        }

        val monthYearAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, monthYearItems)
        spinnerMonthYear.setAdapter(monthYearAdapter)
        spinnerMonthYear.setOnItemClickListener { _, _, position, _ ->
            tempSelectedMonth = monthYearItems[position]
        }

        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            val branchAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, branches)
            spinnerBranch.setAdapter(branchAdapter)
            spinnerBranch.setOnItemClickListener { _, _, position, _ ->
                tempSelectedBranch = branches[position]

                clientViewModel.clients.observe(viewLifecycleOwner) { allClients ->
                    val filteredClients = allClients.filter { client ->
                        client.id_branch_client == tempSelectedBranch?.id_branch
                    }

                    val clientAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line,
                        filteredClients.map { it.name_client ?: "" })
                    spinnerClient.setAdapter(clientAdapter)

                    if (tempSelectedBranch?.id_branch != selectedFilterBranch?.id_branch) {
                        spinnerClient.text?.clear()
                        tempSelectedClient = null
                    }
                }
            }
        }

        clientViewModel.clients.observe(viewLifecycleOwner) { allClients ->
            spinnerClient.setOnItemClickListener { _, _, position, _ ->
                val filteredClients = allClients.filter { client ->
                    client.id_branch_client == tempSelectedBranch?.id_branch
                }
                if (position < filteredClients.size) {
                    tempSelectedClient = filteredClients[position]
                }
            }
        }

        applyButton?.setOnClickListener {
            if (tempSelectedClient == null) {
                Toast.makeText(context, "Klien wajib dipilih!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("RentalRepot", "✅ Filter applied - Client: ${tempSelectedClient?.name_client}")
            onFilterSelected(tempSelectedBranch, tempSelectedClient, tempSelectedMonth)
            bottomSheetDialog.dismiss()
        }

        exitButton?.setOnClickListener {
            if (selectedFilterClient != null || isFirstLoad) {
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(context, "Klien wajib dipilih sebelum menutup filter!", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setCancelable(selectedFilterClient != null)

        bottomSheetDialog.setContentView(view)
        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams

        bottomSheetDialog.show()
    }
}