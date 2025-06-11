package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.replace
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.baguslaundry.controller.InvoiceAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListInvoiceRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ExportInvoicePdfRentalRequest
import dev.ferynnd.baguslaundry.model.InvoiceRentalResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AdminListInvoiceRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminListInvoiceRentalBinding
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var reportViewModel: RentalReportViewModel

    private lateinit var invoiceAdapter: InvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)
        clientViewModel.init(requireContext())
        reportViewModel = ViewModelProvider(this).get(RentalReportViewModel::class.java)
        reportViewModel.init(requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAdminListInvoiceRentalBinding.inflate(layoutInflater)
        invoiceAdapter = InvoiceAdapter { invoice ->
            onPrint(invoice)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = invoiceAdapter
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                    if (selectedBranch.id_branch == -1) {
                        reportViewModel.filterClientInvoice(null) // Semua Cabang
                    } else {
                        reportViewModel.filterClientInvoice(selectedBranch.id_branch)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            reportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
            reportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
            reportViewModel.filteredInvoices.observe(viewLifecycleOwner) { filteredReports ->
                invoiceAdapter.submitList(filteredReports)
            }
            branchViewModel.branches.observe(viewLifecycleOwner) { branchList ->
                invoiceAdapter.setBranches(branchList)
            }
            clientViewModel.clients.observe(viewLifecycleOwner) { clientList ->
                invoiceAdapter.setClients(clientList)
            }
            reportViewModel.invoiceRental.observe(viewLifecycleOwner) { invoiceList ->
                invoiceList?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (invoiceList.isNotEmpty()) {
                            invoiceAdapter.submitList(invoiceList)
                        } else {
                            invoiceAdapter.submitList(emptyList())
                        }
                    }
                }
            }

        }

        binding.buttonAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminInvoiceRentalFragment())
                .commit()
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListReportRentalFragment())
                .commit()
        }
        return binding.root
    }

    private fun onPrint(invoice: InvoiceRentalResponse) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_export_invoice)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setCancelable(true)
        dialog.show()

        val inputNotes = dialog.findViewById<TextInputEditText>(R.id.inputTextNotes)
        val inputPayment = dialog.findViewById<TextInputEditText>(R.id.inputTextPayment)
        val buttonCetak = dialog.findViewById<Button>(R.id.buttonCetak)
        val buttonBack = dialog.findViewById<Button>(R.id.buttonBack)

        buttonCetak.setOnClickListener {
            if (inputNotes.text.toString().isNotEmpty() && inputPayment.text.toString()
                    .isNotEmpty()
            ) {
                val notes = inputNotes.text.toString()
                val payment = inputPayment.text.toString()
                val dataRequest = ExportInvoicePdfRentalRequest(
                    id_invoice_rental = invoice.id_invoice_rental,
                    note = notes,
                    payment = payment
                )
                lifecycleScope.launch {
                    try {
                        val response = reportViewModel.exportInvoiceRental(dataRequest)
                        if (response.success) {
                            val downloadUrl = response.data.download_url
                            if (downloadUrl.isNotEmpty()) {
                                context?.startActivity(
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
                            Toast.makeText(context, "Gagal cetak laporan.", Toast.LENGTH_SHORT)
                                .show()
                        }

                    } catch (e: Exception) {
                        throw e
                    }
                }
            }
        }

        buttonBack.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showFilterBottomSheet(
        context: Context,
        items: List<Branch>,
        onBranchSelected: (Branch) -> Unit
    ) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_filter_branch, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFilterBranch)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = FilterBranchAdapter { selectedBranch ->
            onBranchSelected(selectedBranch)
            bottomSheetDialog.dismiss()
        }

        recyclerView.adapter = adapter
        adapter.submitList(items)

        bottomSheetDialog.setContentView(view)
        // Menentukan tinggi bottom sheet menjadi sepertiga dari tinggi layar perangkat
        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams


        bottomSheetDialog.show()
    }

}