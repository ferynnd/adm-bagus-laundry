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
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class UserDashboardFragment : Fragment(), KurirLatestTransactionLaundryAdapter.OnTransactionActionListener {

    private lateinit var binding: KurirFragmentUserDashboardBinding
    private lateinit var sharePrefrences: SharePrefrenceHelper
    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var kurirLatestTransactionLaundryAdapter: KurirLatestTransactionLaundryAdapter
    private lateinit var clientViewModel: ClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        laundryReportViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java]
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

        binding.recyclerView.apply { // Sesuaikan ID RecyclerView
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kurirLatestTransactionLaundryAdapter
        }

        sharePrefrences = SharePrefrenceHelper(requireContext())
        val userId = sharePrefrences.getString("PREF_USER_ID")?.toInt()
        if (userId != null) {
            // Amati LiveData transaksi dari ViewModel
            laundryReportViewModel.laundryReports.observe(viewLifecycleOwner) { transactions ->
                if (transactions != null) {
                    kurirLatestTransactionLaundryAdapter.submitList(transactions)
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat transaksi", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

         // OBSERVER UNTUK LOADING STATE
        laundryReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Opsional: Sembunyikan atau tampilkan RecyclerView saat loading
            binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }


         // Amati response dari update status
        laundryReportViewModel.updateTransactionResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.success) {
                    Toast.makeText(requireContext(), "Transaksi berhasil diselesaikan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Gagal menyelesaikan transaksi", Toast.LENGTH_SHORT).show()
                }
                laundryReportViewModel.clearUpdateTransactionResponse() // Bersihkan response setelah digunakan
            }
        }

        // Amati error dari ViewModel
        laundryReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                laundryReportViewModel.clearError()
            }
        }

        // Muat data transaksi awal (semua dari cabang user)
        // Panggil getReportLaundry di sini untuk memicu pengambilan data
        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                laundryReportViewModel.getReportLatestLaundry() // Muat semua transaksi tanpa filter status awal
            }
        } else {
            Toast.makeText(requireContext(), "User ID tidak ditemukan, tidak dapat memuat transaksi.", Toast.LENGTH_LONG).show()
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
                            .addToBackStack("setting")
                            .commit()
                        true
                    }
                    R.id.menu_bluetooth -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_user, BluetoothPairingFragment())
                            .addToBackStack("bluetooth")
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, ListItemTransactionLaundryFragment())
                .addToBackStack("laundry")
                .commit()
        }

        binding.menuTransaksiRental.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, CreateListTransaksiRentalFragment())
                .addToBackStack("rental")
                .commit()
        }

        return binding.root

    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCompleteTransactionClicked(reportLaundry: ReportLaundry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin menyelesaikan transaksi ini?")
            .setPositiveButton("Ya") { dialog, which ->
                lifecycleScope.launch {
                    val transactionId = reportLaundry.id_transaction_laundry
                    if (transactionId != null) {
                        // Buat salinan objek reportLaundry dengan status yang diperbarui
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                        // Ambil waktu saat ini dan format ke String
                        val currentDateTime = LocalDateTime.now().format(formatter)
                        val updatedReport = reportLaundry.copy(status_transaction_laundry = StatusReportLaundry.completed, notes_transaction_laundry = " ")

                        Log.d("UserDashboardFragment", "Transaction ID: $transactionId")
                        Log.d("UserDashboardFragment", "Updated Report: $updatedReport")

                        laundryReportViewModel.updateTransactionStatus(
                            updatedReport // Kirim objek ReportLaundry yang sudah diperbarui statusnya
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "ID Transaksi tidak valid.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun logoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah kamu yakin ingin logout?")
            .setPositiveButton("Ya") { dialog, _ ->
                sharePrefrences.clear()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }



}