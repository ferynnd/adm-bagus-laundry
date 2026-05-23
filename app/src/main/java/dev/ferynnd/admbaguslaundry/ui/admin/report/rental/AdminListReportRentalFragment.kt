package dev.ferynnd.admbaguslaundry.ui.admin.report.rental

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
import android.widget.LinearLayout
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
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.controller.RentalReportAdapter
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminListReportRentalBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.Client
import dev.ferynnd.admbaguslaundry.model.ExportReportRental
import dev.ferynnd.admbaguslaundry.model.ListTransactionRental
import dev.ferynnd.admbaguslaundry.model.ProductRental
import dev.ferynnd.admbaguslaundry.model.RentalTransactionItem
import dev.ferynnd.admbaguslaundry.model.ReportRental
import dev.ferynnd.admbaguslaundry.model.UpdateRentalFullRequest
import dev.ferynnd.admbaguslaundry.model.UpdateRentalTransactionData
import dev.ferynnd.admbaguslaundry.ui.PrintPreviewRentalFragment
import dev.ferynnd.admbaguslaundry.ui.admin.AdminDashboardFragment
import dev.ferynnd.admbaguslaundry.ui.openAdminFragment
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

    private var selectedFilterBranch: Branch? = null
    private var selectedFilterClient: Client? = null
    private var selectedFilterMonth: String? = null

    private var currentPage = 1
    private var lastPage = 1

    companion object {
        private const val TAG = "RentalReport"
    }

    data class FilterOption(
        val value: String,
        val displayName: String
    )

    private val statusOptions = listOf(
        FilterOption("in", "Masuk"),
        FilterOption("out", "Keluar"),
        FilterOption("cancelled", "Dibatalkan")
    )

    private val conditionOptions = listOf(
        FilterOption("clean", "Bersih"),
        FilterOption("dirty", "Kotor"),
        FilterOption("damaged", "Rusak")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        rentalReportViewModel.init(requireContext())

        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        branchViewModel.init(requireContext())

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())

        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())

        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
        rentalProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAdminListReportRentalBinding.inflate(inflater, container, false)

        rentalReportAdapter = RentalReportAdapter(
            onEdit = { report -> showEditDialog(report) },
            onDelete = { report -> showDeleteDialog(report) },
            onPrint = { report -> openPrintPreview(report) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalReportAdapter
        }

        setupObservers()
        setupClickListeners()

        rentalReportViewModel.getReportRental(1)

        return binding.root
    }

    private fun setupObservers() {
        rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        rentalReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                rentalReportViewModel.resetErrorMessage()
            }
        }

        rentalReportViewModel.pagination.observe(viewLifecycleOwner) { pagination ->
            currentPage = pagination.current_page
            lastPage = pagination.last_page

            binding.textPageInfo.text = "$currentPage / $lastPage"

            binding.btnPrevPage.isEnabled = currentPage > 1
            binding.btnNextPage.isEnabled = currentPage < lastPage

            binding.btnPrevPage.alpha = if (currentPage > 1) 1f else 0.4f
            binding.btnNextPage.alpha = if (currentPage < lastPage) 1f else 0.4f
        }

        rentalReportViewModel.filteredRentalReports.observe(viewLifecycleOwner) { reports ->
            Log.d(TAG, "Filtered reports: ${reports.size}")
            setReportRental(reports)
        }

        rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { products ->
            rentalReportAdapter.setRentalProducts(products)
        }

        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            rentalReportAdapter.setBranches(branches)
            rentalReportViewModel.setBranches(branches)
        }

        userViewModel.users.observe(viewLifecycleOwner) { users ->
            rentalReportAdapter.setSender(users)
            rentalReportViewModel.setUsers(users)
        }

        clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
            rentalReportAdapter.setClient(clients)
            rentalReportViewModel.setClient(clients)
        }

        rentalReportViewModel.updateTransactionResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.success) {
                    Toast.makeText(requireContext(), "Transaksi berhasil diupdate", Toast.LENGTH_SHORT).show()
                    rentalReportViewModel.resetUpdateTransactionResponse()
                    rentalReportViewModel.getReportRental(currentPage)
                } else {
                    Toast.makeText(requireContext(), "Gagal update: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rentalReportViewModel.deleteTransactionResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.success) {
                    Toast.makeText(requireContext(), "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                    rentalReportViewModel.resetDeleteTransactionResponse()
                    rentalReportViewModel.getReportRental(currentPage)
                } else {
                    Toast.makeText(requireContext(), "Gagal hapus: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                rentalReportViewModel.getReportRental(currentPage - 1)
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < lastPage) {
                rentalReportViewModel.getReportRental(currentPage + 1)
            }
        }

        binding.btnRoutes.setOnClickListener {
            showFilterBottomSheet(requireContext()) { selectedBranch, selectedClient, selectedMonth ->
                filterReports(selectedBranch, selectedClient, selectedMonth)
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                rentalReportViewModel.searchRentalReports(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                rentalReportViewModel.searchRentalReports(newText.orEmpty())
                return true
            }
        })

        binding.iconExel.setOnClickListener {
            showCetakDialog(requireContext())
        }

        binding.iconPdf.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListInvoiceRentalFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.arrowBack.setOnClickListener {
            openAdminFragment(AdminDashboardFragment(), "AdminDashboard")
        }
    }

    private fun setReportRental(newReportRentals: List<ReportRental>) {
        val groupedData = mutableListOf<Any>()

        if (newReportRentals.isEmpty()) {
            rentalReportAdapter.submitList(emptyList())
            return
        }

        val localeID = Locale("id", "ID")
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", localeID)
        val monthFormatter = SimpleDateFormat("MMMM yyyy", localeID)
        val monthKeyFormatter = SimpleDateFormat("yyyy-MM", localeID)

        val sortedReports = newReportRentals.sortedByDescending {
            try {
                parser.parse(it.time_transaction_rental ?: "")
            } catch (e: Exception) {
                null
            }
        }

        val groupedByMonth = sortedReports.groupBy { report ->
            report.time_transaction_rental?.let { dateStr ->
                try {
                    val date = parser.parse(dateStr)
                    monthKeyFormatter.format(date ?: Date())
                } catch (e: Exception) {
                    "0000-00"
                }
            } ?: "0000-00"
        }

        groupedByMonth.keys.sortedDescending().forEach { monthKey ->
            val readableMonth = try {
                val date = monthKeyFormatter.parse(monthKey)
                monthFormatter.format(date ?: Date())
            } catch (e: Exception) {
                "Unknown Date"
            }

            groupedData.add(readableMonth)
            groupedData.addAll(groupedByMonth[monthKey] ?: emptyList())
        }

        rentalReportAdapter.submitList(groupedData)
    }

    private fun filterReports(
        selectedBranch: Branch?,
        selectedClient: Client?,
        selectedMonth: String?
    ) {
        selectedFilterBranch = selectedBranch
        selectedFilterClient = selectedClient
        selectedFilterMonth = selectedMonth

        val allReports = rentalReportViewModel.rentalReports.value ?: emptyList()
        val monthYear = selectedMonth?.let { convertMonthYearToFormat(it) }

        val filtered = allReports.filter { report ->
            val matchBranch = selectedBranch?.id_branch?.let {
                report.id_branch_transaction_rental == it
            } ?: true

            val matchClient = selectedClient?.id_client?.let {
                report.id_client_transaction_rental == it
            } ?: true

            val matchMonth = if (!monthYear.isNullOrEmpty()) {
                report.time_transaction_rental?.startsWith(monthYear) == true
            } else {
                true
            }

            matchBranch && matchClient && matchMonth
        }

        setReportRental(filtered)
    }

    private fun convertMonthYearToFormat(monthYearString: String): String {
        return try {
            val parts = monthYearString.split(" - ")
            if (parts.size != 2) return ""

            val months = listOf(
                "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
                "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
            )

            val monthNumber = months.indexOf(parts[0]) + 1
            if (monthNumber <= 0) return ""

            "${parts[1]}-${String.format("%02d", monthNumber)}"
        } catch (e: Exception) {
            ""
        }
    }

    private fun showEditDialog(report: ReportRental) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_rental)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val inputNumber = dialog.findViewById<TextInputEditText>(R.id.inputNumberTransaction)
        val inputRecipient = dialog.findViewById<TextInputEditText>(R.id.inputRecipientName)
        val inputDate = dialog.findViewById<TextInputEditText>(R.id.inputTransactionDate)
        val inputNotes = dialog.findViewById<TextInputEditText>(R.id.inputNotes)
        val containerRentalItems = dialog.findViewById<LinearLayout>(R.id.containerRentalItems)
        val btnAddNewItem = dialog.findViewById<ImageView>(R.id.btnAddNewItem)
        val buttonSave = dialog.findViewById<Button>(R.id.buttonSave)
        val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)

        inputNumber.setText(report.number_transaction_rental.toString())
        inputRecipient.setText(report.recipient_name_transaction_rental ?: "")
        inputNotes.setText(report.notes_transaction_rental ?: "")
        inputDate.setText(report.time_transaction_rental ?: "")

        inputDate.setOnClickListener {
            showDateTimePicker { selectedDateTime ->
                inputDate.setText(selectedDateTime)
            }
        }

        val productList = rentalProductViewModel.rentalProducts.value ?: emptyList()
        val filteredProducts = productList.filter {
            it.id_branch_rental_item == report.id_branch_transaction_rental
        }

        if (filteredProducts.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Item rental untuk cabang ini belum tersedia",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (report.list_transaction_rentals.isNullOrEmpty()) {
            addRentalItemToDialog(containerRentalItems, filteredProducts, null)
        } else {
            report.list_transaction_rentals.forEach { item ->
                addRentalItemToDialog(containerRentalItems, filteredProducts, item)
            }
        }

        btnAddNewItem.setOnClickListener {
            addRentalItemToDialog(containerRentalItems, filteredProducts, null)
        }

        buttonSave.setOnClickListener {
            val numberText = inputNumber.text.toString().trim()

            if (numberText.isBlank()) {
                Toast.makeText(requireContext(), "Nomor transaksi wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val listItems = mutableListOf<RentalTransactionItem>()

            for (i in 0 until containerRentalItems.childCount) {
                val itemView = containerRentalItems.getChildAt(i)

                val spinnerItem = itemView.findViewById<Spinner>(R.id.spinnerRentalItem)
                val inputStatus = itemView.findViewById<AutoCompleteTextView>(R.id.inputStatus)
                val inputCondition = itemView.findViewById<AutoCompleteTextView>(R.id.inputCondition)
                val inputCount = itemView.findViewById<TextInputEditText>(R.id.inputCount)
                val inputWeight = itemView.findViewById<TextInputEditText>(R.id.inputWeight)

                val selectedItemId = spinnerItem.getTag(R.id.spinnerRentalItem) as? Int

                val statusValue = statusOptions.find {
                    it.displayName == inputStatus.text.toString()
                }?.value

                val conditionValue = conditionOptions.find {
                    it.displayName == inputCondition.text.toString()
                }?.value

                val count = inputCount.text.toString().toIntOrNull() ?: 0
                val weight = inputWeight.text.toString().toDoubleOrNull() ?: 0.0

                if (selectedItemId == null || statusValue == null || conditionValue == null || count <= 0) {
                    Toast.makeText(
                        requireContext(),
                        "Lengkapi item rental ke-${i + 1}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                listItems.add(
                    RentalTransactionItem(
                        id_item_rental = selectedItemId,
                        status_list_transaction_rental = statusValue,
                        condition_list_transaction_rental = conditionValue,
                        count_list_transaction_rental = count,
                        weight_list_transaction_rental = weight
                    )
                )
            }

            if (listItems.isEmpty()) {
                Toast.makeText(requireContext(), "Minimal harus ada 1 item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateRequest = UpdateRentalFullRequest(
                transaction = UpdateRentalTransactionData(
                    id_kurir_transaction_rental = report.id_kurir_transaction_rental ?: 0,
                    id_branch_transaction_rental = report.id_branch_transaction_rental ?: 0,
                    id_client_transaction_rental = report.id_client_transaction_rental ?: 0,
                    recipient_name_transaction_rental = inputRecipient.text.toString().trim(),
                    number_transaction_rental = numberText.toIntOrNull() ?: 0,
                    notes_transaction_rental = inputNotes.text.toString().trim().ifBlank { null }
                ),
                list_items = listItems
            )

            rentalReportViewModel.updateRentalTransaction(
                report.id_transaction_rental ?: 0,
                updateRequest
            )

            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addRentalItemToDialog(
        container: LinearLayout,
        productList: List<ProductRental>,
        existingItem: ListTransactionRental?
    ) {
        val itemView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_edit_rental_item,
            container,
            false
        )

        val spinnerItem = itemView.findViewById<Spinner>(R.id.spinnerRentalItem)
        val inputStatus = itemView.findViewById<AutoCompleteTextView>(R.id.inputStatus)
        val inputCondition = itemView.findViewById<AutoCompleteTextView>(R.id.inputCondition)
        val inputCount = itemView.findViewById<TextInputEditText>(R.id.inputCount)
        val inputWeight = itemView.findViewById<TextInputEditText>(R.id.inputWeight)
        val btnDeleteItem = itemView.findViewById<ImageView>(R.id.btnDeleteItem)

        val itemNames = productList.map {
            it.name_rental_item ?: "Item #${it.id_rental_item}"
        }

        spinnerItem.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            itemNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerItem.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (productList.isNotEmpty() && position in productList.indices) {
                    spinnerItem.setTag(
                        R.id.spinnerRentalItem,
                        productList[position].id_rental_item
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        inputStatus.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statusOptions.map { it.displayName }
            )
        )

        inputCondition.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                conditionOptions.map { it.displayName }
            )
        )

        existingItem?.let { item ->
            val selectedIndex = productList.indexOfFirst {
                it.id_rental_item == item.id_item_rental
            }

            if (selectedIndex >= 0) {
                spinnerItem.setSelection(selectedIndex)
            }

            statusOptions.find {
                it.value == item.status_list_transaction_rental.name.lowercase()
            }?.let {
                inputStatus.setText(it.displayName, false)
            }

            conditionOptions.find {
                it.value == item.condition_list_transaction_rental.name.lowercase()
            }?.let {
                inputCondition.setText(it.displayName, false)
            }

            inputCount.setText(item.count_list_transaction_rental.toString())
            inputWeight.setText(item.weight_list_transaction_rental.toString())
        }

        btnDeleteItem.setOnClickListener {
            if (container.childCount > 1) {
                container.removeView(itemView)
            } else {
                Toast.makeText(requireContext(), "Minimal harus ada 1 item", Toast.LENGTH_SHORT).show()
            }
        }

        container.addView(itemView)
    }

    private fun showDeleteDialog(report: ReportRental) {
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
            rentalReportViewModel.deleteRentalTransaction(report.id_transaction_rental ?: 0)
            dialog.dismiss()
        }

        buttonPermanentDelete.setOnClickListener {
            rentalReportViewModel.forceDeleteRentalTransaction(report.id_transaction_rental ?: 0)
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val timePickerDialog = android.app.TimePickerDialog(
                    requireContext(),
                    { _, selectedHour, selectedMinute ->
                        val formattedDateTime = String.format(
                            "%04d-%02d-%02d %02d:%02d:00",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay,
                            selectedHour,
                            selectedMinute
                        )
                        onDateTimeSelected(formattedDateTime)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun showCetakDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_export_excle_rental)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val spinnerCalendar = dialog.findViewById<Spinner>(R.id.selectCalender)
        val spinnerClient = dialog.findViewById<Spinner>(R.id.selectBranch)
        val spinnerDescription = dialog.findViewById<Spinner>(R.id.selectDescription)
        val inputNote = dialog.findViewById<TextInputEditText>(R.id.inputTex)
        val inputStock = dialog.findViewById<TextInputEditText>(R.id.inputTextStock)
        val addButton = dialog.findViewById<ImageView>(R.id.buttonAdd)
        val descriptionText = dialog.findViewById<TextView>(R.id.inputDescription)
        val cetakButton = dialog.findViewById<Button>(R.id.buttonCetak)
        val backButton = dialog.findViewById<Button>(R.id.buttonBack)

        val notesList = mutableListOf<String>()
        val clients = clientViewModel.clients.value ?: emptyList()
        val products = rentalProductViewModel.rentalProducts.value ?: emptyList()

        val months = listOf(
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        )

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val monthItems = mutableListOf<String>()
        for (year in 2025..currentYear) {
            val maxMonth = if (year == currentYear) currentMonth else 11
            for (monthIndex in 0..maxMonth) {
                monthItems.add("${months[monthIndex]} - $year")
            }
        }

        spinnerCalendar.adapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_item, monthItems).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        spinnerClient.adapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_item, clients.map { it.name_client ?: "-" }).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        spinnerClient.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedClient = clients.getOrNull(position)

                val filteredProducts = products.filter {
                    it.id_branch_rental_item == selectedClient?.id_branch_client
                }

                spinnerDescription.adapter =
                    ArrayAdapter(
                        context,
                        android.R.layout.simple_spinner_item,
                        filteredProducts.map { it.name_rental_item }
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            val selectedDate = spinnerCalendar.selectedItem?.toString().orEmpty()
            val selectedClient = spinnerClient.selectedItem?.toString().orEmpty()
            val selectedProduct = spinnerDescription.selectedItem?.toString().orEmpty()

            if (selectedDate.isEmpty() || selectedClient.isEmpty() || selectedProduct.isEmpty()) {
                Toast.makeText(
                    context,
                     "Semua data wajib dipilih.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val parts = selectedDate.split(" - ")
            if (parts.size != 2) {
                Toast.makeText(context, "Format bulan tidak valid.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val monthName = parts[0]
            val year = parts[1]
            val monthNumber = months.indexOf(monthName) + 1

            if (monthNumber <= 0) {
                Toast.makeText(context, "Bulan tidak valid.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val formattedMonth = String.format("%02d", monthNumber)
            val formattedDate = "$year-$formattedMonth"

            val clientData = clients.find {
                it.name_client == selectedClient
            }

            val clientId = clientData?.id_client ?: 0
            val clientBranchId = clientData?.id_branch_client ?: 0

            if (clientId == 0) {
                Toast.makeText(context, "Client tidak valid.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedProductData = products.find {
                it.name_rental_item == selectedProduct &&
                it.id_branch_rental_item == clientBranchId
            }

            val itemRentalId = selectedProductData?.id_rental_item ?: 0

            if (itemRentalId == 0) {
                Toast.makeText(
                    context,
                    "Item rental tidak ditemukan.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val stock = inputStock.text.toString().toIntOrNull() ?: 0

            val requestData = ExportReportRental(
                month = formattedDate,
                location = clientId,
                notes = notesList,
                id_item_rental = itemRentalId,
                initial_stock = stock
            )

            lifecycleScope.launch {
                try {
                    Log.d("RentalReport", "EXPORT RENTAL REQUEST: $requestData")

                    val response = rentalReportViewModel.exportRentalMonthly(requestData)

                    Log.d("RentalReport", "EXPORT RENTAL RESPONSE: $response")

                    if (response.success) {
                        val downloadUrl = response.data.download_url

                        if (!downloadUrl.isNullOrEmpty()) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(downloadUrl)
                                )
                            )

                            Toast.makeText(
                                context,
                                "File berhasil dibuat dan sedang diunduh.",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, "Download URL kosong.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Gagal cetak laporan: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Log.e("RentalReport", "ERROR EXPORT RENTAL", e)
                    Toast.makeText(
                        context,
                        "Terjadi kesalahan: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        backButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openPrintPreview(report: ReportRental) {
        val fragment = PrintPreviewRentalFragment()

        fragment.arguments = Bundle().apply {
            putInt("transactionId", report.id_transaction_rental ?: 0)
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_admin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showFilterBottomSheet(
        context: Context,
        onFilterSelected: (Branch?, Client?, String?) -> Unit
    ) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_filter_rental_report, null)

        val inputMonth = view.findViewById<AutoCompleteTextView>(R.id.inputBulanTahun)
        val inputBranch = view.findViewById<AutoCompleteTextView>(R.id.inputNamaCabang)
        val inputClient = view.findViewById<AutoCompleteTextView>(R.id.inputNamaClient)
        val applyButton = view.findViewById<Button>(R.id.buttonApply)
        val exitButton = view.findViewById<ImageView>(R.id.btnExit)

        val branches = branchViewModel.branches.value ?: emptyList()
        val clients = clientViewModel.clients.value ?: emptyList()

        var tempBranch: Branch? = selectedFilterBranch
        var tempClient: Client? = selectedFilterClient
        var tempMonth: String? = selectedFilterMonth

        val months = listOf(
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        )

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val monthItems = mutableListOf<String>()
        for (year in 2025..currentYear) {
            val maxMonth = if (year == currentYear) currentMonth else 11
            for (monthIndex in 0..maxMonth) {
                monthItems.add("${months[monthIndex]} - $year")
            }
        }

        inputMonth.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, monthItems)
        )

        inputBranch.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, branches.map { it.name_branch })
        )

        inputClient.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, clients.map { it.name_client ?: "" })
        )

        selectedFilterBranch?.let { inputBranch.setText(it.name_branch, false) }
        selectedFilterClient?.let { inputClient.setText(it.name_client, false) }
        selectedFilterMonth?.let { inputMonth.setText(it, false) }

        inputMonth.setOnItemClickListener { _, _, position, _ ->
            tempMonth = monthItems[position]
        }

        inputBranch.setOnItemClickListener { _, _, position, _ ->
            tempBranch = branches[position]
            tempClient = null
            inputClient.text?.clear()

            val filteredClients = clients.filter {
                it.id_branch_client == tempBranch?.id_branch
            }

            inputClient.setAdapter(
                ArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    filteredClients.map { it.name_client ?: "" }
                )
            )
        }

        inputClient.setOnItemClickListener { _, _, position, _ ->
            val filteredClients = tempBranch?.let { branch ->
                clients.filter { it.id_branch_client == branch.id_branch }
            } ?: clients

            tempClient = filteredClients.getOrNull(position)
        }

        applyButton.setOnClickListener {
            onFilterSelected(tempBranch, tempClient, tempMonth)
            bottomSheetDialog.dismiss()
        }

        exitButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.setContentView(view)

        bottomSheetDialog.window?.attributes = bottomSheetDialog.window?.attributes?.apply {
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }

        bottomSheetDialog.show()
    }
}