package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.RentalReportAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListReportRentalBinding
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import kotlinx.coroutines.launch
import java.util.Date

class AdminListReportRentalFragment : Fragment() {


    private lateinit var binding: FragmentAdminListReportRentalBinding
    private lateinit var rentalReportViewModel : RentalReportViewModel
    private lateinit var rentalReportAdapter: RentalReportAdapter
    private lateinit var branchViewModel: BranchViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
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
                rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { products ->
                    setReportRental(products)
                }
            } catch (e : Exception) {
                throw e
            }

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

            // Step 1: Urutkan berdasarkan tanggal (optional, biar rapi)
            val sortedReports = newReportRentals.sortedBy { it.created_at }

            // Step 2: Group berdasarkan bulan
           val groupedByMonth = sortedReports.groupBy { report ->
                val firstDate = report.created_at
                if (firstDate != null) {
                    formatDateToMonth(firstDate)
                } else {
                    "Unknown Date"
                }
            }

            // Step 3: Masukkan ke dalam list dengan format Header + Items
            for ((month, reports) in groupedByMonth) {
                tempGroupedData.add(month) // Ini header bulan, String
                tempGroupedData.addAll(reports) // Ini list ReportRental di bulan itu
            }

            rentalReportAdapter.submitList(tempGroupedData)
        }

        private fun formatDateToMonth(dateString: String): String {
            return try {
                val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                val date = parser.parse(dateString ?: "")
                formatter.format(date ?: Date())
            } catch (e: Exception) {
                "Unknown Date"
            }
        }

}