package dev.ferynnd.baguslaundry.ui.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_NAME
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentUserDashboardBinding
import dev.ferynnd.baguslaundry.ui.LoginActivity
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.CreateListTransaksiRentalFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.ferynnd.baguslaundry.ui.BluetoothPairingFragment
import dev.ferynnd.baguslaundry.controller.user.KurirLatestTransactionLaundryAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.ui.openUserFragment
import dev.ferynnd.baguslaundry.ui.showAlert
import dev.ferynnd.baguslaundry.ui.showConfirmationAlert
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class UserDashboardFragment : Fragment(), KurirLatestTransactionLaundryAdapter.OnTransactionActionListener {

    private lateinit var binding: KurirFragmentUserDashboardBinding
    private lateinit var sharePrefrences: SharePrefrenceHelper
    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var kurirLatestTransactionLaundryAdapter: KurirLatestTransactionLaundryAdapter
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var branchViewModel: BranchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java].apply {
            init(requireContext())
        }
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java].apply {
            init(requireContext())
        }
        laundryReportViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java].apply {
            init(requireContext())
        }
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = KurirFragmentUserDashboardBinding.inflate(inflater, container, false)
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE

        kurirLatestTransactionLaundryAdapter = KurirLatestTransactionLaundryAdapter()
        kurirLatestTransactionLaundryAdapter.setOnTransactionActionListener(this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kurirLatestTransactionLaundryAdapter
        }

        sharePrefrences = SharePrefrenceHelper(requireContext())
        val userId = sharePrefrences.getString("PREF_USER_ID")?.toInt()
        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                laundryReportViewModel.getReportLatestLaundry()
            }
            laundryReportViewModel.laundryReports.observe(viewLifecycleOwner) { transactions ->
                if (transactions != null) {
                    kurirLatestTransactionLaundryAdapter.submitList(transactions)
                } else {
                    showAlert(
                        title = "Peringatan!",
                        message = "Gagal memuat transaksi.",
                        backgroundColorRes = R.color.primary,
                    )
                }
            }
        } else {
            Toast.makeText(requireContext(), "User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        laundryReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

         // Amati response dari update status
        laundryReportViewModel.updateTransactionResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.success) {
                    showAlert(
                        title = "Berhasil!",
                        message = "Transaksi berhasil diselesaikan.",
                        backgroundColorRes = R.color.primary,
                        iconRes = R.drawable.success
                    )
                } else {
                    showAlert(
                        title = "Gagal!",
                        message = "Transaksi gagal diselesaikan.",
                        backgroundColorRes = R.color.red600,
                        iconRes = R.drawable.failed
                    )
                }
                laundryReportViewModel.clearUpdateTransactionResponse()
            }
        }

        laundryReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                showAlert(
                    title = "Gagal!",
                    message = errorMessage,
                    backgroundColorRes = R.color.red600,
                )
                laundryReportViewModel.clearError()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val nameUser = sharePrefrences.getString(PREF_USER_NAME, null)
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
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_user, UserProfileFragment())
                            .addToBackStack("UserProfile")
                            .commit()
                        true
                    }
                    R.id.menu_bluetooth -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_user, BluetoothPairingFragment())
                            .addToBackStack("Bluetooth")
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

        binding.menuTransaksiLaundry.setOnClickListener {
            openUserFragment(ListItemTransactionLaundryFragment(), "ListItemTransactionLaundry")
        }

        binding.menuTransaksiRental.setOnClickListener {
            openUserFragment(CreateListTransaksiRentalFragment(), "CreateTransactionRental")
        }

        return binding.root

    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCompleteTransactionClicked(reportLaundry: ReportLaundry) {
        showConfirmationAlert(
            title = "Konfirmasi!",
            message = "Apakah Anda yakin ingin menyelesaikan transaksi ini?",
            confirmText = "SELESAIKAN",
            cancelText = "BATAL",
        ) {
            try {
                viewLifecycleOwner.lifecycleScope.launch {
                    val transactionId = reportLaundry.id_transaction_laundry
                    if (transactionId != null) {

                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                        val currentDateTime = LocalDateTime.now().format(formatter)
                        val updatedReport = reportLaundry.copy(status_transaction_laundry = StatusReportLaundry.completed, notes_transaction_laundry = " ")

                        laundryReportViewModel.updateTransactionStatus(
                            updatedReport
                        )
                        showAlert(
                            title = "Berhasil",
                            message = "Transaksi dengan id ${transactionId}, telah diselesaikan",
                            backgroundColorRes = R.color.primary,
                            iconRes = R.drawable.success
                        )
                    } else {
                        showAlert(
                            title = "Gagal!",
                            message = "ID Transaksi tidak valid.",
                            backgroundColorRes = R.color.red600,
                            iconRes = R.drawable.failed
                        )
                    }
                }
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

    private fun logoutDialog() {
         showConfirmationAlert(
            title = "Konfirmasi Keluar",
            message = "Apakah kamu yakin ingin logout?",
            confirmText = "KELUAR",
            cancelText = "BATAL",
        ) {
            try {
                sharePrefrences.clear()
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



}