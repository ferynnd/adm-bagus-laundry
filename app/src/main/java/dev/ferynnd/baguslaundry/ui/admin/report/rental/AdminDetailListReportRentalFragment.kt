package dev.ferynnd.baguslaundry.ui.admin.report.rental

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.DetailLaundryReportAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.ListTransactionReportLaundryViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminDetailListReportRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.StatusTransactionRental
import dev.ferynnd.baguslaundry.model.User
import dev.ferynnd.baguslaundry.ui.admin.report.laundry.AdminDetailListReportLaundryFragment
import dev.ferynnd.baguslaundry.ui.admin.report.laundry.AdminListReportLaundryFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


class AdminDetailListReportRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminDetailListReportRentalBinding
    private lateinit var listTransactionReportLaundryViewModel: ListTransactionReportLaundryViewModel
    private lateinit var reportViewModel: RentalReportViewModel
    private lateinit var detailLaundryReportAdapter: DetailLaundryReportAdapter
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var clientViewModel: ClientViewModel


    private var branch : List<Branch> = emptyList()
    private var sender : List<User> = emptyList()
    private var client : List<Client> = emptyList()

    private var transactionReportID: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listTransactionReportLaundryViewModel =
            ViewModelProvider(this).get(ListTransactionReportLaundryViewModel::class.java)
        reportViewModel = ViewModelProvider(this).get(RentalReportViewModel::class.java)
        laundryProductViewModel = ViewModelProvider(this).get(LaundryProductViewModel::class.java)
        laundryProductViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminDetailListReportRentalBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        detailLaundryReportAdapter = DetailLaundryReportAdapter()

        transactionReportID = arguments?.getInt("transactionRentalID")

        binding.recyclerViewListItem.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detailLaundryReportAdapter
            setOnTouchListener { _, _ -> true }
        }

        try {
            if (transactionReportID != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { product ->
                        product?.let {
                            detailLaundryReportAdapter.setProductLaundry(product)
                        }

                    }
                    branchViewModel.branches.observe(viewLifecycleOwner){ branches ->
                        branch = branches
                    }

                    userViewModel.users.observe(viewLifecycleOwner){ users ->
                        sender = users
                    }
                    clientViewModel.clients.observe(viewLifecycleOwner){ clients ->
                        client = clients
                    }

                    listTransactionReportLaundryViewModel.listTransactionLaundryReports.observe(
                        viewLifecycleOwner
                    ) { listTransactionItem ->
                        listTransactionItem?.let {
                            detailLaundryReportAdapter.submitList(listTransactionItem)
                        }
                    }

                    val dataReport =
                        reportViewModel.getReportRentalById(transactionReportID!!).data

                    binding.apply {
                        inputRecipient.text = dataReport.recipient_name_transaction_rental
                        inputWeight.text = dataReport.total_weight_transaction_rental.toString()
                        inputPcs.text = dataReport.total_pcs_transaction_rental.toString()

                        val localeID = Locale("in", "ID")
                        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)

                        inputAditionalCost.text = formatRupiah.format(dataReport.additional_cost_transaction_rental)
                        inputTotalPrice.text = formatRupiah.format(dataReport.total_price_transaction_rental)

                        inputNotes.text = dataReport.notes_transaction_rental

                        val dataStatus = when(dataReport.status_transaction_rental){
                            StatusTransactionRental.WAITING_FOR_APPROVAL -> "Menunggu Persetujuan"
                            StatusTransactionRental.APPROVED -> "Disetujui"
                            StatusTransactionRental.OUT -> "Keluar"
                            StatusTransactionRental.IN -> "Masuk"
                            StatusTransactionRental.CANCELLED -> "Dibatalkan"
                        }

                        inputStatus.text = dataStatus

                        val clientName = client.find { it.id_client == dataReport.id_client_transaction_rental }?.name_client ?: "Unknown"
                        inputCLient.text =clientName

                        val senderName = sender.find { it.id_user == dataReport.id_kurir_transaction_rental }?.fullname_user ?: "Unknown"
                        inputSender.text = senderName

                        val branchName = branch.find { it.id_branch == dataReport.id_branch_transaction_rental }?.name_branch ?: "Unknown"
                        inputBranch.text = branchName

                        inputTime.text = dataReport.time_transaction_rental

                    }

                }
            } else {
                Toast.makeText(requireContext(), "Detail Tidak Bisa Dimuat", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            throw e
        }



        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListReportRentalFragment())
                .commit()
        }

        return binding.root
    }

}