package dev.ferynnd.baguslaundry.ui.admin.report.laundry

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.LaundryReportAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListReportLaundryBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ExportReportLaundry
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminListReportLaundryFragment : Fragment() {

    private lateinit var binding: FragmentAdminListReportLaundryBinding
    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var laundryReportAdapter: LaundryReportAdapter
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var userViewModel: UserViewModel

    // Variables to store filter values
    private var selectedFilterBranch: Branch? = null
    private var selectedFilterMonth: String? = null
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        laundryReportViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java]
        laundryReportViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        branchViewModel.init(requireContext())
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListReportLaundryBinding.inflate(layoutInflater)

        laundryReportAdapter = LaundryReportAdapter(
            onDetail = { transactionReport ->
                onDetail(transactionReport)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryReportAdapter
        }

        // Updated filter button to show new filter bottom sheet
        binding.btnRoutes.setOnClickListener {
            showFilterBottomSheet(requireContext()) { selectedBranch, selectedMonth ->
                filterReports(selectedBranch, selectedMonth)
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { laundryReportViewModel.searchLaundryReports(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                laundryReportViewModel.searchLaundryReports(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                laundryReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                    binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
                }
                laundryReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                    if (errorMessage.isNotBlank()) {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        laundryReportViewModel.resetErrorMessage()
                    }
                }

                laundryReportViewModel.filteredLaundryReports.observe(viewLifecycleOwner) { filteredReports ->
                    // Only show reports if branch is selected
                    if (selectedFilterBranch != null) {
                        laundryReportAdapter.submitList(filteredReports)
                    } else {
                        laundryReportAdapter.submitList(emptyList())
                    }
                }

                branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                    laundryReportAdapter.setBranches(branches)
                    laundryReportViewModel.setBranches(branches)

                    // Show filter dialog on first load after branches are loaded
                    if (isFirstLoad && branches.isNotEmpty()) {
                        isFirstLoad = false
                        showFilterBottomSheet(requireContext()) { selectedBranch, selectedMonth ->
                            filterReports(selectedBranch, selectedMonth)
                        }
                    }
                }

                userViewModel.users.observe(viewLifecycleOwner) { users ->
                    laundryReportViewModel.setUsers(users)
                    laundryReportAdapter.setUsers(users)
                }

                laundryReportViewModel.laundryReports.observe(viewLifecycleOwner) { products ->
                    // Only set reports if branch is selected
                    if (selectedFilterBranch != null) {
                        setReportLaundry(products)
                    } else {
                        laundryReportAdapter.submitList(emptyList())
                    }
                }
            } catch (e: Exception) {
                throw e
            }
        }

        binding.iconExel.setOnClickListener {
            if (selectedFilterBranch == null) {
                Toast.makeText(requireContext(), "Silakan pilih cabang terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCetakDialog(requireContext())
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminDashboardFragment())
                .commit()
        }

        return binding.root
    }

    private fun onDetail(productLaundry: ReportLaundry) {
        val bundle = Bundle().apply {
            putInt("transactionLaundryID", productLaundry.id_transaction_laundry ?: 0)
        }

        val detailTransactionReport = AdminDetailListReportLaundryFragment()
        detailTransactionReport.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_admin, detailTransactionReport)
            .addToBackStack(null)
            .commit()
    }

    private fun setReportLaundry(newReportLaundrys: List<ReportLaundry>) {
        val tempGroupedData = mutableListOf<Any>()

        if (newReportLaundrys.isEmpty()) {
            laundryReportAdapter.submitList(tempGroupedData)
            return
        }

        val localeID = Locale("id", "ID")
        val parser = SimpleDateFormat("yyyy-MM-dd", localeID)
        val monthFormatter = SimpleDateFormat("MMMM yyyy", localeID)
        val monthKeyFormatter = SimpleDateFormat("yyyy-MM", localeID)

        val sortedReports = newReportLaundrys.sortedByDescending { it.first_date_transaction_laundry }

        val groupedByMonth = sortedReports.groupBy { report ->
            report.first_date_transaction_laundry?.let { dateStr ->
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

        laundryReportAdapter.submitList(tempGroupedData)
    }

    // Updated filter function that handles branch and month filtering
    private fun filterReports(selectedBranch: Branch?, selectedMonth: String?) {
        // Update stored filter values
        selectedFilterBranch = selectedBranch
        selectedFilterMonth = selectedMonth

        if (selectedBranch == null) {
            // If no branch selected, show empty list
            laundryReportAdapter.submitList(emptyList())
            return
        }

        val branchId = selectedBranch.id_branch
        val monthYear = selectedMonth?.let { convertMonthYearToFormat(it) }

        // Filter by branch
        laundryReportViewModel.filterClient(branchId)

        // If month is selected, apply additional filtering
        if (monthYear != null && monthYear.isNotEmpty()) {
            laundryReportViewModel.filteredLaundryReports.observe(viewLifecycleOwner) { reports ->
                val filteredByMonth = reports.filter { report ->
                    report.first_date_transaction_laundry?.startsWith(monthYear) == true
                }
                setReportLaundry(filteredByMonth)
            }
        }
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
        dialog.setContentView(R.layout.dialog_export_excle)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setCancelable(true)

        val spinnerCalendar = dialog.findViewById<Spinner>(R.id.selectCalender)
        val spinnerBranch = dialog.findViewById<Spinner>(R.id.selectBranch)
        val inputNote = dialog.findViewById<TextInputEditText>(R.id.inputTex)
        val addButton = dialog.findViewById<ImageView>(R.id.buttonAdd)
        val descriptionText = dialog.findViewById<TextView>(R.id.inputDescription)
        val cetakButton = dialog.findViewById<Button>(R.id.buttonCetak)
        val backButton = dialog.findViewById<Button>(R.id.buttonBack)

        val notesList = mutableListOf<String>()
        val branchList = mutableListOf<Branch>()

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
            spinnerBranch.adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, branchNames).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

            if (selectedDate.isEmpty() || selectedBranch.isEmpty()) {
                Toast.makeText(context, "Pastikan semua pilihan telah dipilih.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val (monthName, year) = selectedDate.split(" - ")
            val monthNumber = months.indexOf(monthName) + 1
            val formattedMonth = String.format("%02d", monthNumber)
            val formattedDate = "$year-$formattedMonth"

            val branchId = branchList.find { it.name_branch == selectedBranch }?.id_branch ?: 0

            val requestData = ExportReportLaundry(
                month = formattedDate,
                id_branch = branchId,
                notes = notesList
            )

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = laundryReportViewModel.exportLaundryMonthly(requestData)
                    if (response.success) {
                        val downloadUrl = response.data?.download_url.orEmpty()
                        if (downloadUrl.isNotEmpty()) {
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

    // Updated filter bottom sheet with branch (mandatory) and month-year (optional) filtering
    private fun showFilterBottomSheet(
        context: Context,
        onFilterSelected: (Branch?, String?) -> Unit
    ) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_filter_laundry_report, null)

        // Month-Year Spinner
        val spinnerMonthYear = view.findViewById<AutoCompleteTextView>(R.id.inputBulanTahun)
        val spinnerBranch = view.findViewById<AutoCompleteTextView>(R.id.inputNamaCabang)
        val applyButton = view.findViewById<Button>(R.id.buttonApply)
        val exitButton = view.findViewById<ImageView>(R.id.btnExit)

        var tempSelectedBranch: Branch? = selectedFilterBranch
        var tempSelectedMonth: String? = selectedFilterMonth

        // Restore previous selections
        selectedFilterBranch?.let { branch ->
            spinnerBranch.setText(branch.name_branch, false)
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
            }
        }

        applyButton?.setOnClickListener {
            if (tempSelectedBranch == null) {
                Toast.makeText(context, "Cabang wajib dipilih!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onFilterSelected(tempSelectedBranch, tempSelectedMonth)
            bottomSheetDialog.dismiss()
        }

        exitButton?.setOnClickListener {
            // Only allow closing if branch is selected (except for first time)
            if (selectedFilterBranch != null || !isFirstLoad) {
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(context, "Cabang wajib dipilih sebelum menutup filter!", Toast.LENGTH_SHORT).show()
            }
        }

        // Make dialog non-cancelable for first load
        bottomSheetDialog.setCancelable(selectedFilterBranch != null)

        bottomSheetDialog.setContentView(view)
        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams

        bottomSheetDialog.show()
    }
}