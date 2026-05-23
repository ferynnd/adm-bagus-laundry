package dev.ferynnd.admbaguslaundry.ui.admin.report.rental

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.admbaguslaundry.controller.InvoiceAdapter
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminListInvoiceRentalBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.ExportInvoicePdfRentalRequest
import dev.ferynnd.admbaguslaundry.model.InvoiceRentalResponse
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class AdminListInvoiceRentalFragment : Fragment() {

    private var _binding: FragmentAdminListInvoiceRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var branchViewModel: BranchViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var reportViewModel: RentalReportViewModel
    private lateinit var invoiceAdapter: InvoiceAdapter

    private var currentPage = 1
    private var lastPage = 1
    companion object {
        private const val TAG = "INVOICE_DEBUG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        reportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]

        branchViewModel.init(requireContext())
        clientViewModel.init(requireContext())
        reportViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminListInvoiceRentalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObserver()
        setupAction()

        reportViewModel.getInvoiceRental()
    }

    private fun setupRecyclerView() {
        invoiceAdapter = InvoiceAdapter { invoice ->
            onPrint(invoice)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = invoiceAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObserver() {
        reportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        reportViewModel.invoiceRental.observe(viewLifecycleOwner) { invoiceList ->
            Log.d(TAG, "TOTAL INVOICE MASUK FRAGMENT: ${invoiceList.size}")
            Log.d(TAG, "DATA INVOICE: $invoiceList")

            invoiceAdapter.submitList(invoiceList.toList())

            if (invoiceList.isEmpty()) {
                Toast.makeText(requireContext(), "Data invoice kosong", Toast.LENGTH_SHORT).show()
            }
        }

        reportViewModel.invoicePagination.observe(viewLifecycleOwner) { pagination ->
            currentPage = pagination.current_page
            lastPage = pagination.last_page

            binding.textPageInfo.text = "$currentPage / $lastPage"

            binding.btnPrevPage.isEnabled = currentPage > 1
            binding.btnNextPage.isEnabled = currentPage < lastPage

            binding.btnPrevPage.alpha = if (currentPage > 1) 1f else 0.4f
            binding.btnNextPage.alpha = if (currentPage < lastPage) 1f else 0.4f
        }

        branchViewModel.branches.observe(viewLifecycleOwner) { branchList ->
            Log.d(TAG, "TOTAL BRANCH: ${branchList.size}")
            invoiceAdapter.setBranches(branchList)
        }

        clientViewModel.clients.observe(viewLifecycleOwner) { clientList ->
            Log.d(TAG, "TOTAL CLIENT: ${clientList.size}")
            invoiceAdapter.setClients(clientList)
        }
    }

    private fun setupAction() {
        binding.buttonAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminInvoiceRentalFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                    Toast.makeText(
                        requireContext(),
                        "Filter cabang: ${selectedBranch.name_branch}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                reportViewModel.getInvoiceRental(currentPage - 1)
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < lastPage) {
                reportViewModel.getInvoiceRental(currentPage + 1)
            }
        }
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
        val buttonCetak = dialog.findViewById<Button>(R.id.buttonCetak)
        val buttonBack = dialog.findViewById<Button>(R.id.buttonBack)

        buttonCetak.setOnClickListener {
            val notes = inputNotes.text.toString().trim()

            if (notes.isEmpty()) {
                Toast.makeText(requireContext(), "Catatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dataRequest = ExportInvoicePdfRentalRequest(
                id_invoice_rental = invoice.id_invoice_rental,
                note = notes
            )

            lifecycleScope.launch {
                try {
                    Log.d(TAG, "EXPORT REQUEST: $dataRequest")

                    val response = reportViewModel.exportInvoiceRental(dataRequest)

                    Log.d(TAG, "EXPORT RESPONSE: $response")

                    if (response.success) {
                        val downloadUrl = response.data?.download_url

                        if (!downloadUrl.isNullOrEmpty()) {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(downloadUrl)
                                )
                            )

                            Toast.makeText(
                                requireContext(),
                                "File berhasil dibuat.",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Download URL kosong.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Gagal cetak invoice: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "ERROR EXPORT INVOICE: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Terjadi error saat export invoice",
                        Toast.LENGTH_SHORT
                    ).show()
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

        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams

        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}