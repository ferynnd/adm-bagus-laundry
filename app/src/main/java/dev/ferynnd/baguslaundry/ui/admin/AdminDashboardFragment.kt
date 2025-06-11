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
import dev.ferynnd.baguslaundry.data.viewmodel.DashboardViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminDashboardBinding
import dev.ferynnd.baguslaundry.ui.LoginActivity
import dev.ferynnd.baguslaundry.ui.admin.product.laundry.AdminListProductLaundryFragment
import dev.ferynnd.baguslaundry.ui.admin.product.rental.AdminListProductRentalFragment
import dev.ferynnd.baguslaundry.ui.admin.report.laundry.AdminListReportLaundryFragment
import dev.ferynnd.baguslaundry.ui.admin.report.rental.AdminListReportRentalFragment
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private lateinit var binding: FragmentAdminDashboardBinding
    private lateinit var sharePreferences: SharePrefrenceHelper
    private lateinit var userViewModel: UserViewModel

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var dashboardAdapter: DashboardAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
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
        // Observe the latestTransactions LiveData
        viewModel.latestTransactions.observe(viewLifecycleOwner) { transactions ->
            dashboardAdapter.submitList(transactions)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val nameUser = sharePreferences.getString(PREF_USER_NAME, null)
                binding.headerName.text = nameUser
            } catch (e: Exception) {
                throw e
            }
        }


        binding.menuIcon.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.menu_popup, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_setting -> {
                        // Aksi ke halaman setting
                        Toast.makeText(requireContext(), "Menu: Setting", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_admin, AdminProfileFragment())
                            .addToBackStack("setting")
                            .commit()
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListBranchFragment())
                .addToBackStack("branch")
                .commit()
        }

        binding.menuClient.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListClientFragment())
                .addToBackStack("client")
                .commit()
        }

        binding.menuEmployment.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminListUserFragment())
                .addToBackStack("user")
                .commit()
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
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah kamu yakin ingin logout?")
            .setPositiveButton("Ya") { dialog, _ ->
                sharePreferences.clear()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
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
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_admin, AdminListReportRentalFragment())
                        .addToBackStack("rental")
                        .commit()
                    dialog.dismiss()
                }

                val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
                btnLaundry.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_admin, AdminListReportLaundryFragment())
                        .addToBackStack("laundry")
                        .commit()
                    dialog.dismiss()
                }
            }

            " PRODUK" -> {
                val btnRental: LinearLayout = dialog.findViewById(R.id.iconProductRental)
                btnRental.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_admin, AdminListProductRentalFragment())
                        .addToBackStack("rental")
                        .commit()
                    dialog.dismiss()
                }

                val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
                btnLaundry.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_admin, AdminListProductLaundryFragment())
                        .addToBackStack("laundry")
                        .commit()
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