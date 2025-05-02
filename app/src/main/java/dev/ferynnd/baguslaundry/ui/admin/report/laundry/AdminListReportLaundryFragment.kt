package dev.ferynnd.baguslaundry.ui.admin.report.laundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.LaundryReportAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminDetailListReportLaundryBinding
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListReportLaundryBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import kotlinx.coroutines.launch
import java.util.Date

class AdminListReportLaundryFragment : Fragment() {


    private lateinit var binding: FragmentAdminListReportLaundryBinding
    private lateinit var laundryReportViewModel : LaundryReportViewModel
    private lateinit var laundryReportAdapter: LaundryReportAdapter

    private lateinit var branchViewModel: BranchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        laundryReportViewModel = ViewModelProvider(this).get(LaundryReportViewModel::class.java)
        laundryReportViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListReportLaundryBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
         laundryReportAdapter = LaundryReportAdapter(
             onDetail = { transactionReport ->
                 onDetail(transactionReport)
             }
         )


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryReportAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                    laundryReportAdapter.setBranches(branches)
                }
                laundryReportViewModel.laundryReports.observe(viewLifecycleOwner) { products ->
                    setReportLaundry(products)
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

            // Step 1: Urutkan berdasarkan tanggal (optional, biar rapi)
            val sortedReports = newReportLaundrys.sortedBy { it.first_date_transaction_laundry }

            // Step 2: Group berdasarkan bulan
           val groupedByMonth = sortedReports.groupBy { report ->
                val firstDate = report.first_date_transaction_laundry
                if (firstDate != null) {
                    formatDateToMonth(firstDate)
                } else {
                    "Unknown Date"
                }
            }

            // Step 3: Masukkan ke dalam list dengan format Header + Items
            for ((month, reports) in groupedByMonth) {
                tempGroupedData.add(month) // Ini header bulan, String
                tempGroupedData.addAll(reports) // Ini list ReportLaundry di bulan itu
            }

            laundryReportAdapter.submitList(tempGroupedData)
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