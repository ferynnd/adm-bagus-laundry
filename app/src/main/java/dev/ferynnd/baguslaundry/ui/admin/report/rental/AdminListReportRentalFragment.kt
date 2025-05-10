package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListReportRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ExportReportRental
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminListReportRentalFragment : Fragment() {


    private lateinit var binding: FragmentAdminListReportRentalBinding
    private lateinit var rentalReportViewModel : RentalReportViewModel
    private lateinit var rentalReportAdapter: RentalReportAdapter
    private lateinit var branchViewModel: BranchViewModel

    private lateinit var userViewModel: UserViewModel
    private lateinit var clientViewModel: ClientViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListReportRentalBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
         rentalReportAdapter = RentalReportAdapter(
             onDetail = { transactionReport ->
                 onDetail(transactionReport)
             }
         )


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalReportAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                branchViewModel.branches.observe(viewLifecycleOwner){ branches ->
                    rentalReportAdapter.setBranches(branches)
                }
                userViewModel.users.observe(viewLifecycleOwner){ users ->
                    rentalReportAdapter.setSender(users)
                }
                clientViewModel.clients.observe(viewLifecycleOwner){ clients ->
                    rentalReportAdapter.setClient(clients)
                }
                rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { products ->
                    setReportRental(products)
                }
            } catch (e : Exception) {
                throw e
            }

        }

        binding.iconExel.setOnClickListener {
            showCetakDialog(requireContext())
        }

        binding.iconPdf.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminInvoiceRentalFragment())
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
        val monthKeyFormatter = SimpleDateFormat("yyyy-MM", localeID) // untuk sorting bulan

        // Step 1: Urutkan berdasarkan tanggal DESCENDING
        val sortedReports = newReportRentals.sortedByDescending { it.time_transaction_rental }

        // Step 2: Group berdasarkan bulan (pakai kunci yyyy-MM untuk sorting)
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

        // Step 3: Urutkan kunci bulan dari terbaru → lama
        val sortedMonthKeys = groupedByMonth.keys.sortedDescending()

        // Step 4: Tambahkan Header + Items
        for (monthKey in sortedMonthKeys) {
            val readableMonth = try {
                val date = monthKeyFormatter.parse(monthKey)
                monthFormatter.format(date ?: Date())
            } catch (e: Exception) {
                "Unknown Date"
            }

            tempGroupedData.add(readableMonth) // Header
            tempGroupedData.addAll(groupedByMonth[monthKey] ?: emptyList()) // Data
        }

        rentalReportAdapter.submitList(tempGroupedData)
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
        spinnerCalendar.adapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_item, items).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Default pilih bulan sekarang
        val currentItem = "${months[currentMonth]} - $currentYear"
        spinnerCalendar.setSelection(items.indexOf(currentItem))

        // Ambil data branch
        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            branchList.clear()
            branchList.addAll(branches)

            val branchNames = branches.map { it.name_branch }
            spinnerBranch.adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, branchNames).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
        }

        // Tombol tambah catatan
        addButton.setOnClickListener {
            val note = inputNote.text.toString().trim()
            if (note.isNotEmpty()) {
                notesList.add(note)
                inputNote.text?.clear()
                descriptionText.text =
                    notesList.mapIndexed { i, v -> "${i + 1}. $v" }.joinToString("\n")
            }
        }

         val description = listOf("bath towel","hand towel","gorden","keset")

         spinnerDescription.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, description).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

        // Tombol Cetak
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

            val branchId = branchList.find { it.name_branch == selectedBranch }?.id_branch ?: 0

            val requestData = ExportReportRental(
                month = formattedDate,
                location = branchId,
                description = selectedDescription,
                initial_stock = stock,
                notes = notesList
            )

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = rentalReportViewModel.exportRentalMonthly(requestData)
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

}