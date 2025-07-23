package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
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
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.KurirProductAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListProductLaundryBinding
import dev.ferynnd.baguslaundry.model.Branch
import kotlinx.coroutines.launch

class KurirProductFragment : Fragment() {

    private lateinit var binding: KurirFragmentListProductLaundryBinding

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var kurirProductAdapter: KurirProductAdapter
    private lateinit var sharePrefrences: SharePrefrenceHelper
    private lateinit var branchViewModel: BranchViewModel

    private var userId: Int = 0
    private var branchList: List<Branch>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        laundryProductViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java].apply { init(requireContext()) }
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java].apply { init(requireContext()) }
        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java].apply { init(requireContext()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = KurirFragmentListProductLaundryBinding.inflate(inflater, container, false)
        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)?.toIntOrNull() ?: 0

        kurirProductAdapter = KurirProductAdapter()
        binding.recyclerViewProductLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kurirProductAdapter
        }

        // Tab event
        binding.LayoutTabSelected.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = fetchDataForTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Ganti listener pencarian ke SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                kurirProductAdapter.filter(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                kurirProductAdapter.filter(newText.orEmpty())
                return true
            }
        })

        // Observer produk laundry
        laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { products ->
            if (isLaundryTabSelected()) {
                updateProductList(products)
                updateCounter(products?.size ?: 0, "Laundry")
                binding.layoutButtonAdd.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, CreateItemLaundryFragment())
                        .addToBackStack("laundry")
                        .commit()
                }
            }
        }

        // Observer produk rental
        rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { products ->
            if (isRentalTabSelected()) {
                updateProductList(products)
                updateCounter(products?.size ?: 0, "Persewaan")
                binding.layoutButtonAdd.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, CreateItemRentalFragment())
                        .addToBackStack("laundry")
                        .commit()
                }
            }
        }

        // Loading observer
        laundryProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLaundryTabSelected()) updateLoadingState(isLoading)
        }
        rentalProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isRentalTabSelected()) updateLoadingState(isLoading)
        }

        // Error observer
        laundryProductViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isLaundryTabSelected()) {
                showToast(error)
                laundryProductViewModel.resetErrorMessage()
            }
        }
        rentalProductViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isRentalTabSelected()) {
                showToast(error)
                rentalProductViewModel.resetErrorMessage()
            }
        }

        // Observer cabang (sekali saja)
        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            branchList = branches
            kurirProductAdapter.setBranches(branches)
        }

        // Load data tab pertama saat fragment tampil
        fetchDataForTab(binding.LayoutTabSelected.selectedTabPosition)

        return binding.root
    }

    private fun fetchDataForTab(position: Int) {
        // Reset pencarian setiap kali tab berpindah
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()
        when (position) {
            0 -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    laundryProductViewModel.getProductLaundry()
                }
            }
            1 -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    rentalProductViewModel.getProductRental()
                }
            }
        }
    }

    private fun updateProductList(data: List<Any>?) {
        kurirProductAdapter.submitList(data)
        val isEmpty = data.isNullOrEmpty()
        binding.recyclerViewProductLaundry.visibility = if (!isEmpty) View.VISIBLE else View.GONE
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.recyclerViewProductLaundry.visibility = View.GONE
        }
    }

    private fun updateCounter(count: Int, label: String) {
        binding.countData.text = count.toString()
        binding.countKeterangan.text = label
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun isLaundryTabSelected() = binding.LayoutTabSelected.selectedTabPosition == 0
    private fun isRentalTabSelected() = binding.LayoutTabSelected.selectedTabPosition == 1
}
