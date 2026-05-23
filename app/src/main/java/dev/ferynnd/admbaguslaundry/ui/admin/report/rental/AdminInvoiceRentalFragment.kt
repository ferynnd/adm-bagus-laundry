package dev.ferynnd.admbaguslaundry.ui.admin.report.rental

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
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.controller.RentalInvoiceAdapter
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminInvoiceRentalBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.Client
import dev.ferynnd.admbaguslaundry.model.ListInvoiceRentalItem
import dev.ferynnd.admbaguslaundry.model.PostInvoiceRentalRequest
import dev.ferynnd.admbaguslaundry.model.ProductRental
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
class AdminInvoiceRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminInvoiceRentalBinding
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var rentalInvoiceAdapter: RentalInvoiceAdapter
    private lateinit var rentalProductViewModel: RentalProductViewModel

    private val branchList = mutableListOf<Branch>()
    private val clientList = mutableListOf<Client>()
    private lateinit var allProducts : List<ProductRental>

    private var selectedBranchId: Int? = null
    private var selectedClientId: Int? = null
    private var selectedMonth: Int? = null // format: 1 - 12
    private var selectedMonthString: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)
        clientViewModel.init(requireContext())
        rentalReportViewModel = ViewModelProvider(this).get(RentalReportViewModel::class.java)
        rentalReportViewModel.init(requireContext())
        rentalProductViewModel = ViewModelProvider(this).get(RentalProductViewModel::class.java)
        rentalProductViewModel.init(requireContext())
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
            }
        }

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
                filterRentalReports()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
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
                val parts = selectedDate.split(" - ")
                if (parts.size == 2) {
                    val monthString = parts[0]
                    val yearString = parts[1]

                    selectedMonth = months.indexOf(monthString) + 1 // untuk filter laporan

                    val paddedMonth = selectedMonth.toString().padStart(2, '0')
                    selectedMonthString = "$yearString-$paddedMonth"

                }

                val monthString = selectedDate.split(" - ")[0]
                selectedMonth = months.indexOf(monthString) + 1
                filterRentalReports()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }


        binding.buttonCetak.setOnClickListener {
            val selectedBranch = binding.selectBranch.selectedItem as String
            val branchId = branchList.find { it.name_branch == selectedBranch }?.id_branch ?: 0

            val selectedClient = binding.selectClient.selectedItem as String
            val clientId = clientList.find { it.name_client == selectedClient }?.id_client ?: 0

            val diskon = binding.inputTextDiskon.text.toString().toDoubleOrNull() ?: 0.0
            val additionalCost = binding.inputTextAdditional.text.toString().toDoubleOrNull() ?: 0.0

            val selectedInvoices = rentalInvoiceAdapter.getSelectedInvoiceData()

            if (selectedInvoices.isEmpty()) {
                Toast.makeText(requireContext(), "Pilih item rental terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val invoiceItems = selectedInvoices.map {
                ListInvoiceRentalItem(
                    id_item_rental_invoice = it.id_item_rental_invoice
                )
            }

            val request = PostInvoiceRentalRequest(
                id_branch_invoice = branchId,
                id_client_invoice = clientId,
                month_invoice_rental = selectedMonthString ?: "",
                promo_invoice_rental = diskon,
                additional_cost_invoice_rental = additionalCost,
                list_invoice_rentals = invoiceItems
            )

            lifecycleScope.launch {
                try {
                    val result = rentalReportViewModel.createInvoiceRental(request)

                    if (result.success) {
                        Toast.makeText(requireContext(), "Invoice berhasil dibuat!", Toast.LENGTH_SHORT).show()

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_admin, AdminListInvoiceRentalFragment())
                            .commit()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Gagal membuat invoice: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal membuat invoice rental", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }

        binding.buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterRentalReports() {
        val branchId = selectedBranchId
        val clientId = selectedClientId
        val month = selectedMonth

        if (branchId != null && clientId != null && month != null) {
            rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { products ->
                val filtered = products.filter { rental ->
                    rental.id_branch_rental_item == branchId
                }
                allProducts = filtered
                rentalInvoiceAdapter.submitList(filtered)
            }
        } else {
            null
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