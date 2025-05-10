package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.RentalInvoiceAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminInvoiceRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import kotlinx.coroutines.launch

class AdminInvoiceRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminInvoiceRentalBinding
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var rentalInvoiceAdapter: RentalInvoiceAdapter

    private val branchList = mutableListOf<Branch>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        rentalReportViewModel = ViewModelProvider(this).get(RentalReportViewModel::class.java)
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
        }

        binding.selectBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selectedBranch = parent.getItemAtPosition(position) as String
                val branchId = branchList.find { it.name_branch == selectedBranch }?.id_branch ?: 0

                rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { transactions ->
                    val dataRental = transactions.filter { it.id_branch_transaction_rental == branchId }
                    rentalInvoiceAdapter.submitList(dataRental)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                rentalInvoiceAdapter.submitList(emptyList())
            }
        }

        binding.buttonBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListReportRentalFragment())
                .commit()
        }

        return binding.root
    }
}
