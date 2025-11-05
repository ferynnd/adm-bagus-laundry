package dev.ferynnd.baguslaundry.ui.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.DashboardAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_NAME
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.DashboardViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminDashboardBinding
import dev.ferynnd.baguslaundry.ui.LoginActivity
import dev.ferynnd.baguslaundry.ui.admin.product.laundry.AdminListProductLaundryFragment
import dev.ferynnd.baguslaundry.ui.admin.product.rental.AdminListProductRentalFragment
import dev.ferynnd.baguslaundry.ui.admin.report.laundry.AdminListReportLaundryFragment
import dev.ferynnd.baguslaundry.ui.admin.report.rental.AdminListReportRentalFragment
import dev.ferynnd.baguslaundry.ui.openAdminFragment
import dev.ferynnd.baguslaundry.ui.showAlert
import dev.ferynnd.baguslaundry.ui.showConfirmationAlert
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)

        sharePreferences = SharePrefrenceHelper(requireContext())

        dashboardAdapter = DashboardAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dashboardAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val nameUser = sharePreferences.getString(PREF_USER_NAME, null)
                binding.headerName.text = nameUser
            } catch (e: Exception) {
                throw e
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe loading state
            viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }

            branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                branches?.let {
                    dashboardAdapter.setBranch(it)
                }
            }

            clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
                clients?.let {
                    dashboardAdapter.setClient(it)
                }
            }

            viewModel.latestTransactions.observe(viewLifecycleOwner) { transactions ->
                transactions?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (it.isNotEmpty()) { // Gunakan 'it' untuk data LiveData
                             dashboardAdapter.submitList(transactions)
                        } else {
                            dashboardAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }


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