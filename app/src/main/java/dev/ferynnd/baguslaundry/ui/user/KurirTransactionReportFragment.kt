package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import dev.ferynnd.baguslaundry.controller.user.KurirTransactionListAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.User
import kotlinx.coroutines.launch


class KurirTransactionReportFragment : Fragment() {

    private lateinit var binding: KurirFragmentListTransaksiLaundryBinding

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryReportViewModel : LaundryReportViewModel
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var kurirTransactionListAdapter: KurirTransactionListAdapter
    private lateinit var sharePrefrences: SharePrefrenceHelper
    private lateinit var clientViewModel: ClientViewModel


    private var userId: Int = 0
    private var clientList : List<Client>? = null
    private var userList : List<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())
        laundryReportViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java].apply { init(requireContext()) }
        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java].apply { init(requireContext()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = KurirFragmentListTransaksiLaundryBinding.inflate(inflater, container, false)
        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)?.toIntOrNull() ?: 0

        kurirTransactionListAdapter = KurirTransactionListAdapter()
        binding.recyclerViewTransaksiLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kurirTransactionListAdapter
        }

        // Tab event
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = fetchDataForTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
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

        // Observer produk laundry
        laundryReportViewModel.laundryReports.observe(viewLifecycleOwner) { laundry ->
            if (isLaundryTabSelected()) {
                val filterStatus = laundry?.filter { it.status_transaction_laundry == StatusReportLaundry.completed }
                updateTransactionReportList(filterStatus)
                updateCounter(laundry?.size ?: 0, "Laundry")
            }
        }

        // Observer produk rental
        rentalReportViewModel.rentalReports.observe(viewLifecycleOwner) { rental ->
            if (isRentalTabSelected()) {
                updateTransactionReportList(rental)
                updateCounter(rental?.size ?: 0, "Persewaan")
            }
        }

        // Loading observer
        laundryReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLaundryTabSelected()) updateLoadingState(isLoading)
        }
        rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isRentalTabSelected()) updateLoadingState(isLoading)
        }

        // Error observer
        laundryReportViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isLaundryTabSelected()) {
                showToast(error)
                laundryReportViewModel.resetErrorMessage()
            }
        }
        rentalReportViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isRentalTabSelected()) {
                showToast(error)
                rentalReportViewModel.resetErrorMessage()
            }
        }

        userViewModel.users.observe(viewLifecycleOwner) { users ->
            userList = users
            kurirTransactionListAdapter.setUsers(users)
        }

        clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
            clientList = clients
            kurirTransactionListAdapter.setClients(clients)
        }

        // Load data tab pertama saat fragment tampil
        fetchDataForTab(binding.tabLayout.selectedTabPosition)

        return binding.root
    }

    private fun fetchDataForTab(position: Int) {
        // Reset pencarian setiap kali tab berpindah
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()
        when (position) {
            0 -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    laundryReportViewModel.getReportLaundry()
                }
            }
            1 -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    rentalReportViewModel.getReportRental()
                }
            }
        }
    }

    private fun updateTransactionReportList(data: List<Any>?) {
        kurirTransactionListAdapter.submitList(data)
        val isEmpty = data.isNullOrEmpty()
        binding.recyclerViewTransaksiLaundry.visibility = if (!isEmpty) View.VISIBLE else View.GONE
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.recyclerViewTransaksiLaundry.visibility = View.GONE
        }
    }

    private fun updateCounter(count: Int, label: String) {
        binding.countData.text = count.toString()
        binding.countKeterangan.text = label
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun isLaundryTabSelected() = binding.tabLayout.selectedTabPosition == 0
    private fun isRentalTabSelected() = binding.tabLayout.selectedTabPosition == 1
}
