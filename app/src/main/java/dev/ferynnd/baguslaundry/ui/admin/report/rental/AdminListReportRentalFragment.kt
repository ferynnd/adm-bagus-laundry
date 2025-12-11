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

    private var filteredRentalListByBranch: List<ProductRental> = listOf()
    private var fullRentalList: List<ProductRental> = listOf()
    private var selectedFilterBranch: Branch? = null
    private var selectedFilterClient: Client? = null
    private var selectedFilterMonth: String? = null
    private var isFirstLoad = true
    private var hasShownFilterDialog = false // ✅ Tambahan untuk tracking dialog

    // ✅ TAMBAHAN: Key untuk SharedPreferences
    companion object {
        private const val PREF_FILTER_BRANCH_ID = "rental_filter_branch_id"
        private const val PREF_FILTER_BRANCH_NAME = "rental_filter_branch_name"
        private const val PREF_FILTER_CLIENT_ID = "rental_filter_client_id"
        private const val PREF_FILTER_CLIENT_NAME = "rental_filter_client_name"
        private const val PREF_FILTER_MONTH = "rental_filter_month"
        private const val PREF_HAS_SELECTED_FILTER = "rental_has_selected_filter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        rentalReportViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        branchViewModel.init(requireContext())
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())
        clientViewModel =
            ViewModelProvider(this)[ClientViewModel::class.java].apply { init(requireContext()) }
        rentalProductViewModel =
            ViewModelProvider(this)[RentalProductViewModel::class.java].apply { init(requireContext()) }
        listTransactionReportRentalViewModel =
            ViewModelProvider(this)[ListTransactionReportRentalViewModel::class.java]
        listTransactionReportRentalViewModel.init(requireContext())

        // ✅ PERBAIKAN: Load filter dari SharedPreferences terlebih dahulu
        loadFilterFromPreferences()

        // Kemudian restore dari ViewModel
        if (selectedFilterBranch == null) {
            selectedFilterBranch = rentalReportViewModel.currentFilterBranch
        }
        if (selectedFilterClient == null) {
            selectedFilterClient = rentalReportViewModel.currentFilterClient
        }
        if (selectedFilterMonth == null) {
            selectedFilterMonth = rentalReportViewModel.currentFilterMonth
        }

        Log.d(
            "RentalFragment",
            "🔄 Restored filter - Branch: ${selectedFilterBranch?.name_branch}, Client: ${selectedFilterClient?.name_client}, Month: $selectedFilterMonth"
        )
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
                hasShownFilterDialog = true // Sudah ada filter, jangan tampilkan dialog
            }
        }

        setupObservers()
        setupClickListeners()

        return binding.root
    }

    // ✅ TAMBAHAN: Method untuk load filter dari SharedPreferences
    private fun loadFilterFromPreferences() {
        val prefs = requireContext().getSharedPreferences("RentalFilterPrefs", Context.MODE_PRIVATE)
        val hasSelectedFilter = prefs.getBoolean(PREF_HAS_SELECTED_FILTER, false)

        if (hasSelectedFilter) {
            Log.d("RentalFragment", "📂 Loading filter from SharedPreferences...")

            val branchId = prefs.getInt(PREF_FILTER_BRANCH_ID, -1)
            val branchName = prefs.getString(PREF_FILTER_BRANCH_NAME, null)
            val clientId = prefs.getInt(PREF_FILTER_CLIENT_ID, -1)
            val clientName = prefs.getString(PREF_FILTER_CLIENT_NAME, null)
            val month = prefs.getString(PREF_FILTER_MONTH, null)

            if (branchId != -1 && branchName != null) {
                selectedFilterBranch = Branch(id_branch = branchId, name_branch = branchName)
            }

            if (clientId != -1 && clientName != null) {
                selectedFilterClient = Client(id_client = clientId, name_client = clientName)
            }

            selectedFilterMonth = month
            hasShownFilterDialog = true // Sudah pernah pilih filter

            Log.d(
                "RentalFragment",
                "✅ Loaded - Branch: $branchName, Client: $clientName, Month: $month"
            )
        } else {
            Log.d("RentalFragment", "ℹ️ No saved filter found")
        }
    }

    // ✅ TAMBAHAN: Method untuk save filter ke SharedPreferences
    private fun saveFilterToPreferences() {
        val prefs = requireContext().getSharedPreferences("RentalFilterPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (selectedFilterClient != null) {
            editor.putBoolean(PREF_HAS_SELECTED_FILTER, true)
            editor.putInt(PREF_FILTER_BRANCH_ID, selectedFilterBranch?.id_branch ?: -1)
            editor.putString(PREF_FILTER_BRANCH_NAME, selectedFilterBranch?.name_branch)
            editor.putInt(PREF_FILTER_CLIENT_ID, selectedFilterClient?.id_client ?: -1)
            editor.putString(PREF_FILTER_CLIENT_NAME, selectedFilterClient?.name_client)
            editor.putString(PREF_FILTER_MONTH, selectedFilterMonth)
            Log.d("RentalFragment", "💾 Filter saved to SharedPreferences")
        } else {
            editor.clear()
            Log.d("RentalFragment", "🗑️ Filter cleared from SharedPreferences")
        }

        editor.apply()
    }

    // ✅ TAMBAHAN: Save state saat onPause
    override fun onPause() {
        super.onPause()
        saveFilterToPreferences()
        Log.d("RentalFragment", "⏸️ onPause - Filter saved")
    }

    // ✅ TAMBAHAN: Restore state saat onResume
    override fun onResume() {
        super.onResume()
        Log.d("RentalFragment", "▶️ onResume - Filter restored")

        // Jika sudah ada filter tersimpan, re-apply
        if (selectedFilterClient != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(300) // Delay kecil untuk memastikan data sudah ready
                filterReports(selectedFilterBranch, selectedFilterClient, selectedFilterMonth)
            }
        }
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
                Toast.makeText(
                    requireContext(),
                    "Silakan pilih klien terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            showCetakDialog(requireContext())
        }

        binding.iconPdf.setOnClickListener {
            if (selectedFilterClient == null) {
                Toast.makeText(
                    requireContext(),
                    "Silakan pilih klien terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
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
                        fullRentalList = it
                    }
                }

                rentalReportViewModel.filteredRentalReports.observe(viewLifecycleOwner) { filteredReports ->
                    Log.d("RentalRepot", "📋 Filtered reports received: ${filteredReports.size}")
                    Log.d("RentalRepot", "👤 Selected client: ${selectedFilterClient?.name_client}")

                    if (selectedFilterClient != null && filteredReports.isNotEmpty()) {
                        setReportRental(filteredReports)
                    } else if (selectedFilterClient != null) {
                        Log.w("RentalRepot", "⚠️ Client selected but no data")
                        rentalReportAdapter.submitList(emptyList())
                    } else {
                        Log.d("RentalRepot", "ℹ️ No client selected yet")
                        rentalReportAdapter.submitList(emptyList())
                    }
                }

                rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { reports ->
                    Log.d("RentalRepot", "📊 Raw reports loaded: ${reports.size}")

                    if (selectedFilterClient != null) {
                        Log.d("RentalFragment", "🔄 Auto re-applying filter...")
                        filterReports(
                            selectedFilterBranch,
                            selectedFilterClient,
                            selectedFilterMonth
                        )
                    }
                }

                branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                    Log.d("RentalRepot", "🏢 Branches loaded: ${branches.size}")
                    rentalReportAdapter.setBranches(branches)
                    rentalReportViewModel.setBranches(branches)

                    // ✅ PERBAIKAN: Hanya tampilkan dialog jika belum pernah pilih filter
                    if (isFirstLoad && branches.isNotEmpty() && !hasShownFilterDialog) {
                        isFirstLoad = false
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(500)
                            Log.d("RentalRepot", "🎯 Showing filter dialog (first time)...")
                            showFilterBottomSheet(requireContext()) { selectedBranch, selectedClient, selectedMonth ->
                                filterReports(selectedBranch, selectedClient, selectedMonth)
                                hasShownFilterDialog = true
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

                listTransactionReportRentalViewModel.listTransactionRentalReports.observe(
                    viewLifecycleOwner
                ) { listTransaksi ->
                    Log.d("RentalRepot", "📝 List transactions loaded: ${listTransaksi.size}")
//                    fullRentalList = listTransaksi
                    rentalReportAdapter.setRentalList(listTransaksi)
                }

                rentalReportViewModel.updateTransactionResponse.observe(viewLifecycleOwner) { response ->
                    response?.let {
                        if (it.success) {
                            Log.d("RentalRepot", "✅ Update success")
                            Toast.makeText(
                                requireContext(),
                                "Transaksi berhasil diupdate",
                                Toast.LENGTH_SHORT
                            ).show()
                            rentalReportViewModel.resetUpdateTransactionResponse()
                        } else {
                            Log.e("RentalRepot", "❌ Update failed: ${it.message}")
                            Toast.makeText(
                                requireContext(),
                                "Gagal update: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                rentalReportViewModel.deleteTransactionResponse.observe(viewLifecycleOwner) { response ->
                    response?.let {
                        if (it.success) {
                            Log.d("RentalRepot", "✅ Delete success")
                            Toast.makeText(
                                requireContext(),
                                "Transaksi berhasil dihapus",
                                Toast.LENGTH_SHORT
                            ).show()
                            rentalReportViewModel.resetDeleteTransactionResponse()
                        } else {
                            Log.e("RentalRepot", "❌ Delete failed: ${it.message}")
                            Toast.makeText(
                                requireContext(),
                                "Gagal hapus: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
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

        // ✅ PERBAIKAN: Filter rental items berdasarkan branch dari transaksi
//        filteredRentalListByBranch = fullRentalList.filter {
//            it.id_branch_rental_item == report.id_branch_transaction_rental
//        }
//
//        Log.d("RentalRepot", "🔍 Branch ID: ${report.id_branch_transaction_rental}")
//        Log.d("RentalRepot", "📦 Total rental items: ${fullRentalList.size}")
//        Log.d(
//            "RentalRepot",
//            "✅ Filtered rental items for this branch: ${filteredRentalListByBranch.size}"
//        )
//
//        if (filteredRentalListByBranch.isEmpty()) {
//            Toast.makeText(
//                requireContext(),
//                "Tidak ada item rental untuk cabang ini",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }

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
//        val containerRentalItems = dialog.findViewById<LinearLayout>(R.id.containerRentalItems)
//        val btnAddNewItem = dialog.findViewById<ImageView>(R.id.btnAddNewItem)
        val buttonSave = dialog.findViewById<Button>(R.id.buttonSave)
        val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)

        // Set data awal
        inputNumber.setText(report.number_transaction_rental.toString())
        inputRecipient.setText(report.recipient_name_transaction_rental)
        inputNotes.setText(report.notes_transaction_rental)
        inputDate.setText(report.time_transaction_rental ?: "")

        // ✅ PERBAIKAN: DateTime Picker untuk tanggal DAN waktu
        inputDate.setOnClickListener {
            showDateTimePicker { selectedDateTime ->
                inputDate.setText(selectedDateTime)
            }
        }

        // ✅ PERBAIKAN: Load existing items - pastikan ada data
//        if (report.list_transaction_rentals.isNullOrEmpty()) {
//            Log.w("RentalRepot", "⚠️ No existing rental items in this transaction")
//            // Tambah 1 item kosong sebagai default
//            addRentalItemToDialog(dialog, containerRentalItems, null)
//        } else {
//            Log.d("RentalRepot", "📋 Loading ${report.list_transaction_rentals.size} existing items")
//            report.list_transaction_rentals.forEach { listItem ->
//                addRentalItemToDialog(dialog, containerRentalItems, listItem)
//            }
//        }

        // ✅ Tambah item baru (gunakan filtered list)
//        btnAddNewItem.setOnClickListener {
//            if (filteredRentalListByBranch.isNotEmpty()) {
//                addRentalItemToDialog(dialog, containerRentalItems, null)
//                Toast.makeText(requireContext(), "Item baru ditambahkan", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(
//                    requireContext(),
//                    "Tidak ada item rental tersedia untuk cabang ini",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }

        buttonSave.setOnClickListener {
            val numberText = inputNumber.text.toString()
            val recipientText = inputRecipient.text.toString()
            val dateText = inputDate.text.toString()
            val notesText = inputNotes.text.toString()

            if (numberText.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Nomor transaksi tidak boleh kosong",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (dateText.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Tanggal transaksi tidak boleh kosong",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // ✅ Collect all rental items from dialog
            val rentalItems = mutableListOf<RentalTransactionItem>()
//            for (i in 0 until containerRentalItems.childCount) {
//                val itemView = containerRentalItems.getChildAt(i)
//                val spinner = itemView.findViewById<Spinner>(R.id.spinnerRentalItem)
//                val inputStatus = itemView.findViewById<AutoCompleteTextView>(R.id.inputStatus)
//                val inputCondition =
//                    itemView.findViewById<AutoCompleteTextView>(R.id.inputCondition)
//                val inputCount = itemView.findViewById<TextInputEditText>(R.id.inputCount)
//                val inputWeight = itemView.findViewById<TextInputEditText>(R.id.inputWeight)
//
//                val selectedItemId = spinner.getTag(R.id.spinnerRentalItem) as? Int
//                if (selectedItemId == null) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Pilih item rental pada item ke-${i + 1}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return@setOnClickListener
//                }
//
//                val statusValue =
//                    statusOptions.find { it.displayName == inputStatus.text.toString() }?.value
//                val conditionValue =
//                    conditionOptions.find { it.displayName == inputCondition.text.toString() }?.value
//                val count = inputCount.text.toString().toIntOrNull() ?: 0
//                val weight = inputWeight.text.toString().toDoubleOrNull() ?: 0.0
//
//                if (statusValue == null || conditionValue == null || count <= 0) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Lengkapi data item rental ke-${i + 1}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return@setOnClickListener
//                }
//
//                rentalItems.add(
//                    RentalTransactionItem(
//                        id_item_rental = selectedItemId,
//                        status_list_transaction_rental = statusValue,
//                        condition_list_transaction_rental = conditionValue,
//                        count_list_transaction_rental = count,
//                        weight_list_transaction_rental = weight
//                    )
//                )
//            }

            if (rentalItems.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Minimal harus ada 1 item rental",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val updateRequest = UpdateRentalTransactionRequest(
                id_kurir_transaction_rental = report.id_kurir_transaction_rental ?: 0,
                id_branch_transaction_rental = report.id_branch_transaction_rental ?: 0,
                id_client_transaction_rental = report.id_client_transaction_rental ?: 0,
                recipient_name_transaction_rental = recipientText.ifBlank { null },
                number_transaction_rental = numberText.toIntOrNull() ?: 0,
                time_transaction_rental = dateText,
                notes_transaction_rental = notesText.ifBlank { null },
//                list_transaction_rentals = rentalItems
            )

            Log.d("RentalRepot", "💾 Updating transaction: ${report.id_transaction_rental}")
            Log.d("RentalRepot", "📦 Total items: ${rentalItems.size}")
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

    // ✅ PERBAIKAN: Gunakan filteredRentalListByBranch instead of fullRentalList
    private fun addRentalItemToDialog(
        dialog: Dialog,
        container: LinearLayout,
        existingItem: ListTransactionRental?
    ) {
        val itemView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_edit_rental_item,
            container,
            false
        )

        val spinner = itemView.findViewById<Spinner>(R.id.spinnerRentalItem)
        val inputStatus = itemView.findViewById<AutoCompleteTextView>(R.id.inputStatus)
        val inputCondition = itemView.findViewById<AutoCompleteTextView>(R.id.inputCondition)
        val inputCount = itemView.findViewById<TextInputEditText>(R.id.inputCount)
        val inputWeight = itemView.findViewById<TextInputEditText>(R.id.inputWeight)
        val btnDelete = itemView.findViewById<ImageView>(R.id.btnDeleteItem)

        // ✅ PERBAIKAN: Setup spinner dengan filtered list (hanya item dari branch yang sama)
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filteredRentalListByBranch.map { it.name_rental_item }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // ✅ PERBAIKAN: Ambil ID dari filtered list
                val selectedItem = filteredRentalListByBranch[position]
                spinner.setTag(R.id.spinnerRentalItem, selectedItem.id_rental_item)
                Log.d(
                    "RentalRepot",
                    "🎯 Selected item: ${selectedItem.name_rental_item} (ID: ${selectedItem.id_rental_item})"
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup status dropdown
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statusOptions.map { it.displayName }
        )
        inputStatus.setAdapter(statusAdapter)

        // Setup condition dropdown
        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            conditionOptions.map { it.displayName }
        )
        inputCondition.setAdapter(conditionAdapter)

        // ✅ Set data jika edit existing item
        if (existingItem != null) {
            // ✅ PERBAIKAN: Cari index dari filtered list
            val itemIndex = filteredRentalListByBranch.indexOfFirst {
                it.id_rental_item == existingItem.id_item_rental
            }

            if (itemIndex != -1) {
                spinner.setSelection(itemIndex)
                Log.d("RentalRepot", "📝 Loading existing item at index $itemIndex")
            } else {
                Log.w(
                    "RentalRepot",
                    "⚠️ Item ID ${existingItem.id_item_rental} not found in filtered list"
                )
            }

            statusOptions.find { it.value == existingItem.status_list_transaction_rental.name.lowercase() }
                ?.let {
                    inputStatus.setText(it.displayName, false)
                }

            conditionOptions.find { it.value == existingItem.condition_list_transaction_rental.name.lowercase() }
                ?.let {
                    inputCondition.setText(it.displayName, false)
                }

            inputCount.setText(existingItem.count_list_transaction_rental.toString())
            inputWeight.setText(existingItem.weight_list_transaction_rental.toString())
        } else {
            // ✅ TAMBAHAN: Set default values untuk item baru
            Log.d("RentalRepot", "➕ Adding new empty item")
        }

        // Delete button
        btnDelete.setOnClickListener {
            if (container.childCount > 1) {
                container.removeView(itemView)
                Toast.makeText(requireContext(), "Item dihapus", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Minimal harus ada 1 item rental",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        container.addView(itemView)
    }

    // ✅ TAMBAHAN: DateTime Picker (Tanggal + Waktu)
    private fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Tampilkan Date Picker dulu
        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Setelah pilih tanggal, tampilkan Time Picker
                val timePickerDialog = android.app.TimePickerDialog(
                    requireContext(),
                    { _, selectedHour, selectedMinute ->
                        // Format: yyyy-MM-dd HH:mm:ss
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
                    hour,
                    minute,
                    true // 24-hour format
                )
                timePickerDialog.show()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    // ✅ Tetap pertahankan showDatePicker untuk keperluan lain (jika masih dipakai)
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate =
                    String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                onDateSelected(formattedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    // ✅ Filter options untuk status dan condition
    data class FilterOption(val value: String, val displayName: String)

    private val statusOptions = listOf(
        FilterOption("out", "Keluar"),
        FilterOption("in", "Masuk"),
        FilterOption("cancelled", "Dibatalkan")
    )

    private val conditionOptions = listOf(
        FilterOption("clean", "Bersih"),
        FilterOption("dirty", "Kotor"),
        FilterOption("damaged", "Rusak")
    )

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
                Log.d(
                    "RentalRepot",
                    "⚠️ Force deleting transaction: ${report.id_transaction_rental}"
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    rentalReportViewModel.forceDeleteRentalTransaction(
                        report.id_transaction_rental ?: 0
                    )
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

    private fun filterReports(
        selectedBranch: Branch?,
        selectedClient: Client?,
        selectedMonth: String?
    ) {
        Log.d(
            "RentalRepot",
            "🔍 Filtering reports - Branch: ${selectedBranch?.name_branch}, Client: ${selectedClient?.name_client}, Month: $selectedMonth"
        )

        selectedFilterBranch = selectedBranch
        selectedFilterClient = selectedClient
        selectedFilterMonth = selectedMonth

        // ✅ Simpan filter ke SharedPreferences
        saveFilterToPreferences()

        if (selectedClient == null) {
            Log.w("RentalRepot", "⚠️ No client selected")
            rentalReportAdapter.submitList(emptyList())
            return
        }

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
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
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
                Toast.makeText(context, "Pastikan semua pilihan telah dipilih.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val (monthName, year) = selectedDate.split(" - ")
            val monthNumber = months.indexOf(monthName) + 1
            val formattedMonth = String.format("%02d", monthNumber)
            val formattedDate = "$year-$formattedMonth"

            val productRental =
                productList.find { it.name_rental_item == selectedDescription }?.id_rental_item ?: 0
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
                            Toast.makeText(
                                context,
                                "File berhasil dibuat dan sedang diunduh.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "Download URL kosong.", Toast.LENGTH_SHORT)
                                .show()
                        }
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "Gagal cetak laporan.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG)
                        .show()
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

        val monthYearAdapter =
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, monthYearItems)
        spinnerMonthYear.setAdapter(monthYearAdapter)
        spinnerMonthYear.setOnItemClickListener { _, _, position, _ ->
            tempSelectedMonth = monthYearItems[position]
        }

        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            val branchAdapter =
                ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, branches)
            spinnerBranch.setAdapter(branchAdapter)
            spinnerBranch.setOnItemClickListener { _, _, position, _ ->
                tempSelectedBranch = branches[position]

                clientViewModel.clients.observe(viewLifecycleOwner) { allClients ->
                    val filteredClients = allClients.filter { client ->
                        client.id_branch_client == tempSelectedBranch?.id_branch
                    }

                    val clientAdapter = ArrayAdapter(
                        context, android.R.layout.simple_dropdown_item_1line,
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
                Toast.makeText(
                    context,
                    "Klien wajib dipilih sebelum menutup filter!",
                    Toast.LENGTH_SHORT
                ).show()
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