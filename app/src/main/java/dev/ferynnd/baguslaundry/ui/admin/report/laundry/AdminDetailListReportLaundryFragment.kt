package dev.ferynnd.baguslaundry.ui.admin.report.laundry

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
import dev.ferynnd.baguslaundry.databinding.FragmentAdminDetailListReportLaundryBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


class AdminDetailListReportLaundryFragment : Fragment() {

    private lateinit var binding: FragmentAdminDetailListReportLaundryBinding
    private lateinit var listTransactionReportLaundryViewModel: ListTransactionReportLaundryViewModel
    private lateinit var reportLaundryViewModel: LaundryReportViewModel
    private lateinit var detailLaundryReportAdapter: DetailLaundryReportAdapter
    private lateinit var laundryProductViewModel: LaundryProductViewModel

    private lateinit var branchViewModel: BranchViewModel
    private lateinit var userViewModel: UserViewModel

    private var branches: List<Branch> = emptyList()
    private var users: List<User> = emptyList()


    private var transactionReportID: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listTransactionReportLaundryViewModel =
            ViewModelProvider(this).get(ListTransactionReportLaundryViewModel::class.java)
        listTransactionReportLaundryViewModel.init(requireContext())
        reportLaundryViewModel = ViewModelProvider(this).get(LaundryReportViewModel::class.java)
        reportLaundryViewModel.init(requireContext())
        laundryProductViewModel = ViewModelProvider(this).get(LaundryProductViewModel::class.java)
        laundryProductViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminDetailListReportLaundryBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        detailLaundryReportAdapter = DetailLaundryReportAdapter()

        transactionReportID = arguments?.getInt("transactionLaundryID")

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
                    listTransactionReportLaundryViewModel.listTransactionLaundryReports.observe(
                        viewLifecycleOwner
                    ) { listTransactionItem ->
                        listTransactionItem?.let {
                            detailLaundryReportAdapter.submitList(listTransactionItem)
                        }
                    }

                    val dataReport =
                        reportLaundryViewModel.getReportLaundryById(transactionReportID!!).data


                    branchViewModel.branches.observe(viewLifecycleOwner) { branch ->
                        branches = branch
                    }
                    userViewModel.users.observe(viewLifecycleOwner) { user ->
                        users = user
                    }


                    binding.apply {
                        inputEmployment.text = dataReport.id_kurir_transaction_laundry.toString()
                        inputBranch.text = dataReport.id_branch_transaction_laundry.toString()
                        inputCustommer.text = dataReport.name_client_transaction_laundry
                        inputNotes.text = dataReport.notes_transaction_laundry
                        inputWeight.text = dataReport.total_weight_transaction_laundry.toString()
                        inputIsActive.text = dataReport.is_active_transaction_laundry.toString()
                        val localeID = Locale("in", "ID")
                        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)

                        inputCountItem.text =
                            dataReport.count_item_transaction_laundry.toString()
                        inputCash.text = formatRupiah.format(dataReport.cash_transaction_laundry)
                        inputTotalPrice.text =
                            formatRupiah.format(dataReport.total_price_transaction_laundry)
                        inputTotalPriceTransaction.text =
                            formatRupiah.format(dataReport.total_transaction_laundry)

                        val branchName =
                            branches.find { it.id_branch == dataReport.id_branch_transaction_laundry }?.name_branch
                                ?: "Unknown"
                        inputBranch.text = branchName
                        val employeeName =
                            users.find { it.id_user == dataReport.id_kurir_transaction_laundry }?.fullname_user
                                ?: "Unknown"
                        inputEmployment.text = employeeName
                        val dataStatus = when (dataReport.status_transaction_laundry) {
                            StatusReportLaundry.paid -> "Sudah Bayar"
                            StatusReportLaundry.unpaid -> "Belum Bayar"
                            StatusReportLaundry.completed -> "Selesai"
                            StatusReportLaundry.cancelled -> "DiBatalkan"
                        }
                        inputStatus.text = dataStatus
                        inputTimeIn.text = dataReport.first_date_transaction_laundry
                        inputTimeOut.text = dataReport.last_date_transaction_laundry


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
                .replace(R.id.host_fragment_admin, AdminListReportLaundryFragment())
                .commit()
        }

        return binding.root
    }

}