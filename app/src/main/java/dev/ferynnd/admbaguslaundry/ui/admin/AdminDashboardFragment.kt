package dev.ferynnd.admbaguslaundry.ui.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.controller.DashboardAdapter
import dev.ferynnd.admbaguslaundry.data.helper.Constant.Companion.PREF_USER_NAME
import dev.ferynnd.admbaguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.DashboardViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminDashboardBinding
import dev.ferynnd.admbaguslaundry.ui.LoginActivity
import dev.ferynnd.admbaguslaundry.ui.admin.product.laundry.AdminListProductLaundryFragment
import dev.ferynnd.admbaguslaundry.ui.admin.product.rental.AdminListProductRentalFragment
import dev.ferynnd.admbaguslaundry.ui.admin.report.laundry.AdminListReportLaundryFragment
import dev.ferynnd.admbaguslaundry.ui.admin.report.rental.AdminListReportRentalFragment
import dev.ferynnd.admbaguslaundry.ui.openAdminFragment
import dev.ferynnd.admbaguslaundry.ui.showAlert
import dev.ferynnd.admbaguslaundry.ui.showConfirmationAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private lateinit var binding: FragmentAdminDashboardBinding
    private lateinit var sharePreferences: SharePrefrenceHelper
    private lateinit var userViewModel: UserViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var clientViewModel: ClientViewModel

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var dashboardAdapter: DashboardAdapter

    private val TAG = "AdminDashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: init ViewModel")

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        sharePreferences = SharePrefrenceHelper(requireContext())

        Log.d(TAG, "onCreateView: setup dashboard")

        dashboardAdapter = DashboardAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dashboardAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val nameUser = sharePreferences.getString(PREF_USER_NAME, null)
                binding.headerName.text = nameUser
                Log.d(TAG, "User login: $nameUser")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengambil nama user: ${e.message}", e)
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Loading state: $isLoading")
            binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            Log.d(TAG, "Branch observe size: ${branches?.size ?: 0}")
            branches?.let {
                dashboardAdapter.setBranch(it)
            }
        }

        clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
            Log.d(TAG, "Client observe size: ${clients?.size ?: 0}")
            clients?.let {
                dashboardAdapter.setClient(it)
            }
        }

        viewModel.latestTransactions.observe(viewLifecycleOwner) { transactions ->
            Log.d(TAG, "Latest transaction observe size: ${transactions?.size ?: 0}")

            transactions?.forEachIndexed { index, item ->
                Log.d(TAG, "Transaction[$index]: $item")
            }

            lifecycleScope.launch(Dispatchers.Main) {
                if (!transactions.isNullOrEmpty()) {
                    Log.d(TAG, "Submit dashboard list: ${transactions.size}")
                    dashboardAdapter.submitList(transactions)
                } else {
                    Log.d(TAG, "Submit dashboard list: empty")
                    dashboardAdapter.submitList(emptyList())
                }
            }
        }

        loadDashboardData()

        binding.menuIcon.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.menu_popup, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_setting -> {
                        openAdminFragment(AdminProfileFragment(), "AdminProfile")
                        true
                    }

                    R.id.menu_logout -> {
                        logoutDialog()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }

        binding.menuBranch.setOnClickListener {
            openAdminFragment(AdminListBranchFragment(), "AdminBranch")
        }

        binding.menuClient.setOnClickListener {
            openAdminFragment(AdminListClientFragment(), "AdminClient")
        }

        binding.menuEmployment.setOnClickListener {
            openAdminFragment(AdminListUserFragment(), "AdminUser")
        }

        binding.menuProduct.setOnClickListener {
            showDialogMEnu(" PRODUK")
        }

        binding.menuReport.setOnClickListener {
            showDialogMEnu(" LAPORAN")
        }

        return binding.root
    }

    private fun loadDashboardData() {
        Log.d(TAG, "loadDashboardData: start")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Init branchViewModel")
                branchViewModel.init(requireContext())

                Log.d(TAG, "Init clientViewModel")
                clientViewModel.init(requireContext())

                Log.d(TAG, "loadDashboardData: success")
            } catch (e: Exception) {
                Log.e(TAG, "loadDashboardData error: ${e.message}", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: refresh dashboard data")
        loadDashboardData()
    }

    private fun logoutDialog() {
        showConfirmationAlert(
            title = "Konfirmasi Keluar",
            message = "Apakah kamu yakin ingin logout?",
            confirmText = "KELUAR",
            cancelText = "BATAL",
        ) {
            try {
                sharePreferences.clear()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                showAlert(
                    title = "Berhasil!",
                    message = "berhasil keluar dari akun",
                    iconRes = R.drawable.success
                )
            } catch (e: Exception) {
                showAlert(
                    title = "Gagal!",
                    message = "Terjadi kesalahan: ${e.message}",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                    duration = 5000
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDialogMEnu(textMenu: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_menu_product)

        dialog.findViewById<TextView>(R.id.textHeaderSecond).text = textMenu

        when (textMenu) {
            " LAPORAN" -> {
                val btnRental: LinearLayout = dialog.findViewById(R.id.iconProductRental)
                btnRental.setOnClickListener {
                    openAdminFragment(AdminListReportRentalFragment(), "AdminListReportRental")
                    dialog.dismiss()
                }

                val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
                btnLaundry.setOnClickListener {
                    openAdminFragment(AdminListReportLaundryFragment(), "AdminListReportLaundry")
                    dialog.dismiss()
                }
            }

            " PRODUK" -> {
                val btnRental: LinearLayout = dialog.findViewById(R.id.iconProductRental)
                btnRental.setOnClickListener {
                    openAdminFragment(AdminListProductRentalFragment(), "AdminListProductRental")
                    dialog.dismiss()
                }

                val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
                btnLaundry.setOnClickListener {
                    openAdminFragment(AdminListProductLaundryFragment(), "AdminListProductLaundry")
                    dialog.dismiss()
                }
            }
        }

        dialog.show()

        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}