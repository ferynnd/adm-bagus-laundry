package dev.ferynnd.baguslaundry.ui.user

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import dev.ferynnd.baguslaundry.controller.KurirTransactionListAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class KurirTransactionReportFragment : Fragment() {

    private lateinit var binding: KurirFragmentListTransaksiLaundryBinding

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryReportViewModel : LaundryReportViewModel
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var kurirTransactionListAdapter: KurirTransactionListAdapter
    private lateinit var sharePrefrences: SharePrefrenceHelper
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var branchViewModel: BranchViewModel

    private var userId: Int = 0
    private var clientList : List<Client>? = null
    private var userList : List<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())

        laundryReportViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java].apply {
            init(requireContext())
        }
        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        rentalReportViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java].apply {
            init(requireContext())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = KurirFragmentListTransaksiLaundryBinding.inflate(inflater, container, false)
        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)?.toIntOrNull() ?: 0

        Log.d("KurirFragment", "onCreateView: userId = $userId")

        kurirTransactionListAdapter = KurirTransactionListAdapter(parentFragmentManager)
        binding.recyclerViewTransaksiLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kurirTransactionListAdapter
        }

        // Tab event
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                Log.d("KurirFragment", "Tab selected: ${tab.position}")
                fetchDataForTab(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d("KurirFragment", "Tab reselected: ${tab?.position}")
                tab?.position?.let { fetchDataForTab(it) }
            }
        })

        // Tambahkan listener pencarian
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                kurirTransactionListAdapter.filter(query.orEmpty())
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                kurirTransactionListAdapter.filter(newText.orEmpty())
                return true
            }
        })

        // Observer laundry reports - gunakan filteredLaundryReports
        laundryReportViewModel.filteredLaundryReports.observe(viewLifecycleOwner) { laundry ->
            Log.d("KurirFragment", "Laundry reports observed: ${laundry?.size ?: 0} items")
            if (isLaundryTabSelected()) {
                val filterStatus = laundry?.filter {
                    it.status_transaction_laundry == StatusReportLaundry.completed
                } ?: emptyList()

                Log.d("KurirFragment", "Completed laundry: ${filterStatus.size} items")
                updateTransactionReportList(filterStatus)
                updateCounter(filterStatus.size, "Laundry")
            }
        }

        // Observer rental reports - gunakan filteredRentalReports
        rentalReportViewModel.filteredRentalReports.observe(viewLifecycleOwner) { rental ->
            Log.d("KurirFragment", "Rental reports observed: ${rental?.size ?: 0} items")
            if (isRentalTabSelected()) {
                val filter = rental?.filter {
                    it.id_kurir_transaction_rental == userId
                } ?: emptyList()

                updateTransactionReportList(filter)
                updateCounter(filter.size , "Persewaan")
            }
        }

        // Loading observer
        laundryReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("KurirFragment", "Laundry loading: $isLoading")
            if (isLaundryTabSelected()) updateLoadingState(isLoading)
        }

        rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("KurirFragment", "Rental loading: $isLoading")
            if (isRentalTabSelected()) updateLoadingState(isLoading)
        }

        // Error observer
        laundryReportViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isLaundryTabSelected()) {
                Log.e("KurirFragment", "Laundry error: $error")
                showToast(error)
                laundryReportViewModel.resetErrorMessage()
            }
        }

        rentalReportViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isRentalTabSelected()) {
                Log.e("KurirFragment", "Rental error: $error")
                showToast(error)
                rentalReportViewModel.resetErrorMessage()
            }
        }

        // User dan Client observers
        userViewModel.users.observe(viewLifecycleOwner) { users ->
            Log.d("KurirFragment", "Users loaded: ${users?.size ?: 0}")
            userList = users
            kurirTransactionListAdapter.setUsers(users ?: emptyList())
        }

        clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
            Log.d("KurirFragment", "Clients loaded: ${clients?.size ?: 0}")
            clientList = clients
            kurirTransactionListAdapter.setClients(clients ?: emptyList())
        }

        // Load data tab pertama saat fragment tampil
        fetchDataForTab(binding.tabLayout.selectedTabPosition)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDataForTab(position: Int) {
        Log.d("KurirFragment", "fetchDataForTab: position = $position")

        // Reset pencarian setiap kali tab berpindah
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()

        // Tampilkan loading state
        updateLoadingState(true)

        when (position) {
            0 -> {
                Log.d("KurirFragment", "Fetching Laundry data...")
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        laundryReportViewModel.getReportLaundry()
                    } catch (e: Exception) {
                        Log.e("KurirFragment", "Error fetching laundry data", e)
                        updateLoadingState(false)
                        showToast("Gagal memuat data laundry: ${e.message}")
                    }
                }
            }
            1 -> {
                Log.d("KurirFragment", "Fetching Rental data...")
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        rentalReportViewModel.getReportRentalKurir()
                    } catch (e: Exception) {
                        Log.e("KurirFragment", "Error fetching rental data", e)
                        updateLoadingState(false)
                        showToast("Gagal memuat data rental: ${e.message}")
                    }
                }
            }
        }
    }

    private fun updateTransactionReportList(data: List<Any>) {
        Log.d("KurirFragment", "updateTransactionReportList: ${data.size} items")

        kurirTransactionListAdapter.submitList(data)

        val isEmpty = data.isEmpty()
        binding.recyclerViewTransaksiLaundry.visibility = if (!isEmpty) View.VISIBLE else View.GONE

        // Tampilkan pesan jika kosong
        if (isEmpty && !isLoadingState()) {
            showToast("Tidak ada data tersedia")
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        Log.d("KurirFragment", "updateLoadingState: $isLoading")
        binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.recyclerViewTransaksiLaundry.visibility = View.GONE
        }
    }

    private fun isLoadingState(): Boolean {
        return binding.progresBar.visibility == View.VISIBLE
    }

    private fun updateCounter(count: Int, label: String) {
        Log.d("KurirFragment", "updateCounter: $count $label")
        binding.countData.text = count.toString()
        binding.countKeterangan.text = label
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun isLaundryTabSelected() = binding.tabLayout.selectedTabPosition == 0
    private fun isRentalTabSelected() = binding.tabLayout.selectedTabPosition == 1

    override fun onResume() {
        super.onResume()
        Log.d("KurirFragment", "onResume - refreshing current tab")
        // Refresh data saat fragment kembali aktif
        fetchDataForTab(binding.tabLayout.selectedTabPosition)
    }
}