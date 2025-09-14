package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.baguslaundry.controller.RentalReportAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListReportRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ExportReportRental
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminListReportRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminListReportRentalBinding
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var rentalReportAdapter: RentalReportAdapter
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var rentalProductViewModel : RentalProductViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var clientViewModel: ClientViewModel

    // Variables to store filter values
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
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java].apply { init(requireContext()) }
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java].apply { init(requireContext()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListReportRentalBinding.inflate(layoutInflater)

        rentalReportAdapter = RentalReportAdapter(
            onDetail = { transactionReport ->
                onDetail(transactionReport)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalReportAdapter
        }

        // Updated filter button to show new filter bottom sheet
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                    binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
                }
                rentalReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                    if (errorMessage.isNotBlank()) {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        rentalReportViewModel.resetErrorMessage()
                    }
                }
                rentalReportViewModel.filteredRentalReports.observe(viewLifecycleOwner) { filteredReports ->
                    // Only show reports if client is selected
                    if (selectedFilterClient != null) {
                        rentalReportAdapter.submitList(filteredReports)
                    } else {
                        rentalReportAdapter.submitList(emptyList())
                    }
                }
                branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                    rentalReportAdapter.setBranches(branches)
                    rentalReportViewModel.setBranches(branches)

                    // Show filter dialog on first load after branches are loaded
                    if (isFirstLoad && branches.isNotEmpty()) {
                        isFirstLoad = false
                        showFilterBottomSheet(requireContext()) { selectedBranch, selectedClient, selectedMonth ->
                            filterReports(selectedBranch, selectedClient, selectedMonth)
                        }
                    }
                }
                userViewModel.users.observe(viewLifecycleOwner) { users ->
                    rentalReportAdapter.setSender(users)
                    rentalReportViewModel.setUsers(users)
                }
                clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
                    rentalReportAdapter.setClient(clients)
                    rentalReportViewModel.setClient(clients)
                }
                rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { products ->
                    // Only set reports if client is selected
                    if (selectedFilterClient != null) {
                        setReportRental(products)
                    } else {
                        rentalReportAdapter.submitList(emptyList())
                    }
                }
            } catch (e: Exception) {
                throw e
            }
        }

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
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminDashboardFragment())
                .commit()
        }

        return binding.root
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
        val tempGroupedData = mutableListOf<Any>()

        if (newReportRentals.isEmpty()) {
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

        rentalReportAdapter.submitList(tempGroupedData)
    }

    // Updated filter function that handles branch, client, and month filtering
    private fun filterReports(selectedBranch: Branch?, selectedClient: Client?, selectedMonth: String?) {
        // Update stored filter values
        selectedFilterBranch = selectedBranch
        selectedFilterClient = selectedClient
        selectedFilterMonth = selectedMonth

        if (selectedClient == null) {
            // If no client selected, show empty list
            rentalReportAdapter.submitList(emptyList())
            return
        }

        val clientId = selectedClient.id_client
        val monthYear = selectedMonth?.let { convertMonthYearToFormat(it) }

        // Call the ViewModel to filter by client and month
        rentalReportViewModel.filterByClientAndMonth(clientId, monthYear)
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

            Log.d("Request Data", requestData.toString())

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

    // Updated filter bottom sheet with client and month-year filtering
    private fun showFilterBottomSheet(
        context: Context,
        onFilterSelected: (Branch?, Client?, String?) -> Unit
    ) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_filter_rental_report, null)

        // Month-Year Spinner
        val spinnerMonthYear = view.findViewById<AutoCompleteTextView>(R.id.inputBulanTahun)
        val spinnerBranch = view.findViewById<AutoCompleteTextView>(R.id.inputNamaCabang)
        val spinnerClient = view.findViewById<AutoCompleteTextView>(R.id.inputNamaClient)
        val applyButton = view.findViewById<Button>(R.id.buttonApply)
        val exitButton = view.findViewById<ImageView>(R.id.btnExit)

        var tempSelectedBranch: Branch? = selectedFilterBranch
        var tempSelectedClient: Client? = selectedFilterClient
        var tempSelectedMonth: String? = selectedFilterMonth

        // Restore previous selections
        selectedFilterBranch?.let { branch ->
            spinnerBranch.setText(branch.name_branch, false)
        }
        selectedFilterClient?.let { client ->
            spinnerClient.setText(client.name_client, false)
        }
        selectedFilterMonth?.let { month ->
            spinnerMonthYear.setText(month, false)
        }

        // Setup Month-Year options
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

        // Setup Branch options
        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            val branchAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, branches)
            spinnerBranch.setAdapter(branchAdapter)
            spinnerBranch.setOnItemClickListener { _, _, position, _ ->
                tempSelectedBranch = branches[position]

                // Filter clients by selected branch
                clientViewModel.clients.observe(viewLifecycleOwner) { allClients ->
                    val filteredClients = allClients.filter { client ->
                        client.id_branch_client == tempSelectedBranch?.id_branch
                    }

                    val clientAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line,
                        filteredClients.map { it.name_client ?: "" })
                    spinnerClient.setAdapter(clientAdapter)

                    // Clear client selection if branch changed
                    if (tempSelectedBranch?.id_branch != selectedFilterBranch?.id_branch) {
                        spinnerClient.text?.clear()
                        tempSelectedClient = null
                    }
                }
            }
        }

        // Setup Client selection
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
            onFilterSelected(tempSelectedBranch, tempSelectedClient, tempSelectedMonth)
            bottomSheetDialog.dismiss()
        }

        exitButton?.setOnClickListener {
            // Only allow closing if client is selected (except for first time)
            if (selectedFilterClient != null || isFirstLoad) {
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(context, "Klien wajib dipilih sebelum menutup filter!", Toast.LENGTH_SHORT).show()
            }
        }

        // Make dialog non-cancelable for first load
        bottomSheetDialog.setCancelable(selectedFilterClient != null)

        bottomSheetDialog.setContentView(view)
        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams

        bottomSheetDialog.show()
    }
}