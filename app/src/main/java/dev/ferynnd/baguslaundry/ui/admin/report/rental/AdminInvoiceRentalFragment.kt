package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.RentalInvoiceAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminInvoiceRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ListInvoiceRentalItem
import dev.ferynnd.baguslaundry.model.PostInvoiceRentalRequest
import dev.ferynnd.baguslaundry.model.ReportRental
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AdminInvoiceRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminInvoiceRentalBinding
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var rentalInvoiceAdapter: RentalInvoiceAdapter

    private val branchList = mutableListOf<Branch>()
    private val clientList = mutableListOf<Client>()
    private lateinit var allReports: List<ReportRental>

    private var selectedBranchId: Int? = null
    private var selectedClientId: Int? = null
    private var selectedMonth: Int? = null // format: 1 - 12


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)
        clientViewModel.init(requireContext())
        rentalReportViewModel = ViewModelProvider(this).get(RentalReportViewModel::class.java)
        rentalReportViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminInvoiceRentalBinding.inflate(inflater, container, false)

        rentalInvoiceAdapter = RentalInvoiceAdapter()

        binding.listItemTrasaksi.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalInvoiceAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                branchList.clear()
                branchList.addAll(branches)
                val branchNames = branches.map { it.name_branch }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    branchNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                binding.selectBranch.adapter = adapter
                Log.d("SpinnerLog", "Data cabang dimuat ke Spinner. Jumlah: ${branches.size}")
            }
            clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
                clientList.clear()
                clientList.addAll(clients)
                val clientNames = clients.map { it.name_client }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    clientNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                binding.selectClient.adapter = adapter
                Log.d("SpinnerLog", "Data klien dimuat ke Spinner. Jumlah: ${clients.size}")
            }
        }

//        binding.selectBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>, view: View?, position: Int, id: Long
//            ) {
//                val selectedBranch = parent.getItemAtPosition(position) as String
//                val branchId = branchList.find { it.name_branch == selectedBranch }?.id_branch ?: 0
//
//                rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { transactions ->
//                    val dataRental = transactions.filter { it.id_branch_transaction_rental == branchId }
//                    allReports = dataRental
//                    rentalInvoiceAdapter.submitList(dataRental)
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                rentalInvoiceAdapter.submitList(emptyList())
//            }
//        }

        binding.selectBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val branchName = parent.getItemAtPosition(position) as String
                selectedBranchId = branchList.find { it.name_branch == branchName }?.id_branch
                Log.d("SpinnerLog", "Cabang dipilih: $branchName, ID: $selectedBranchId")
                filterRentalReports()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d("SpinnerLog", "Tidak ada cabang yang dipilih")
            }
        }

        binding.selectClient.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val clientName = parent.getItemAtPosition(position) as String
                selectedClientId = clientList.find { it.name_client == clientName }?.id_client
                Log.d("SpinnerLog", "Klien dipilih: $clientName, ID: $selectedClientId")
                filterRentalReports()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d("SpinnerLog", "Tidak ada klien yang dipilih")
            }
        }

        val months = listOf(
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        )

        // Generate list bulan-tahun
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

        // Set adapter ke spinner kalender
        binding.selectDate.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Default pilih bulan sekarang
        val currentItem = "${months[currentMonth]} - $currentYear"
        binding.selectDate.setSelection(items.indexOf(currentItem))

        binding.selectDate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedDate = parent?.getItemAtPosition(position) as String
                val monthString = selectedDate.split(" - ")[0]
                selectedMonth = months.indexOf(monthString) + 1
                Log.d("SpinnerLog", "Bulan dipilih: $monthString, Nomor: $selectedMonth")
                filterRentalReports()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("SpinnerLog", "Tidak ada tanggal yang dipilih")
            }
        }


        binding.buttonCetak.setOnClickListener {
            val selectedBranch = binding.selectBranch.selectedItem as String
            val branchId = branchList.find { it.name_branch == selectedBranch }?.id_branch ?: 0
            val selectedClient = binding.selectClient.selectedItem as String
            val clientId = clientList.find { it.name_client == selectedClient }?.id_client ?: 0
            val note = binding.inputTextNotes.text.toString()

            val selectedInvoices = rentalInvoiceAdapter.getSelectedInvoiceData()

            // Hitung total weight dari ID yang dipilih
//            val selectedReports = allReports.filter { report ->
//                selectedInvoices.any { it.id_rental_transaction == report.id_transaction_rental }
//            }

            val invoiceItems = selectedInvoices.map {
                ListInvoiceRentalItem(
                    id_rental_transaction = it.id_rental_transaction,
                    status_list_invoice_rental = it.status_list_invoice_rental,
                    note_list_invoice_rental = it.note_list_invoice_rental
                )
            }

            val request = PostInvoiceRentalRequest(
                id_branch_invoice = branchId,
                id_client_invoice = clientId,
                notes_invoice_rental = note,
                list_invoice_rentals = invoiceItems
            )

            lifecycleScope.launch {
                try {
                    if (selectedInvoices.isNotEmpty()) {
                        rentalReportViewModel.createInvoiceRental(request)
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal membuat invoice rental",
                        Toast.LENGTH_SHORT
                    ).show()
                    throw e
                }
            }
        }

        binding.buttonBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListInvoiceRentalFragment())
                .commit()
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterRentalReports() {
        val branchId = selectedBranchId
        val clientId = selectedClientId
        val month = selectedMonth

        if (branchId != null && clientId != null && month != null) {
            rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { transactions ->
                val filtered = transactions.filter { report ->
                    report.id_branch_transaction_rental == branchId &&
                            report.id_client_transaction_rental == clientId &&
                            isInMonth(report.time_transaction_rental, month)
                }
                allReports = filtered
                rentalInvoiceAdapter.submitList(filtered)
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Pilih cabang, klien, dan bulan terlebih dahulu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isInMonth(dateString: String?, month: Int): Boolean {
        if (dateString == null) {
            return false
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return try {
            val dateTime = LocalDate.parse(dateString, formatter)
            dateTime.monthValue == month
        } catch (e: Exception) {
            false
        }
    }

}