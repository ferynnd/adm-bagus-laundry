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
import dev.ferynnd.baguslaundry.controller.KurirProductAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListProductLaundryBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.ui.showAlert
import dev.ferynnd.baguslaundry.ui.showConfirmationAlert
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

        kurirProductAdapter = KurirProductAdapter(
            onDeleteLaundry = { productLaundry ->
                deleteLaundryItem(productLaundry)
            },
            onDeleteRental = { productRental ->
                deleteRentalItem(productRental)
            }
        )

        binding.recyclerViewProductLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kurirProductAdapter
        }

        binding.LayoutTabSelected.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = fetchDataForTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                laundryProductViewModel.setSearchQuery(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                laundryProductViewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })


        laundryProductViewModel.filteredProductLaundry.observe(viewLifecycleOwner) { products ->
            if (isLaundryTabSelected()) {
                updateProductList(products)
                updateCounter(products?.size ?: 0, "Laundry")
            }
        }

        rentalProductViewModel.filteredRentalProducts.observe(viewLifecycleOwner) { products ->
            if (isRentalTabSelected()) {
                updateProductList(products)
                updateCounter(products?.size ?: 0, "Persewaan")
            }
        }

        laundryProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLaundryTabSelected()) updateLoadingState(isLoading)
        }
        rentalProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isRentalTabSelected()) updateLoadingState(isLoading)
        }

        laundryProductViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isLaundryTabSelected()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                laundryProductViewModel.resetErrorMessage()
            }
        }
        rentalProductViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank() && isRentalTabSelected()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                rentalProductViewModel.resetErrorMessage()
            }
        }

        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            branchList = branches
            kurirProductAdapter.setBranches(branches)
        }

        fetchDataForTab(binding.LayoutTabSelected.selectedTabPosition)

        return binding.root
    }

    private fun deleteLaundryItem(productLaundry: ProductLaundry) {
            showConfirmationAlert(
                title = "Konfirmasi Hapus!",
                message = "Apakah kamu yakin ingin menghapus layanan ${productLaundry.name_laundry_item}?",
                confirmText = "YA",
                cancelText = "BATAL",
                backgroundColorRes = R.color.primary
            ) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        binding.progresBar.visibility = View.VISIBLE
                        laundryProductViewModel.deleteProductLaundry(productLaundry)
                        laundryProductViewModel.getProductLaundry(forceRefresh = true)
                        showAlert(
                            title = "Berhasil!",
                            message = "Layanan laundry '${productLaundry.name_laundry_item}' berhasil dihapus",
                            backgroundColorRes = R.color.primary,
                            iconRes = R.drawable.success
                        )
                    } catch (e: Exception) {
                        showAlert(
                            title = "Gagal!",
                            message = "Gagal menghapus layanan: ${e.message}",
                            backgroundColorRes = R.color.red600,
                            iconRes = R.drawable.failed
                        )
                    } finally {
                        binding.progresBar.visibility = View.GONE
                    }
                }
            }
    }

    private fun deleteRentalItem(productRental: ProductRental) {
          showConfirmationAlert(
                title = "Konfirmasi Hapus!",
                message = "Apakah kamu yakin ingin menghapus produk ${productRental.name_rental_item}?",
                confirmText = "YA",
                cancelText = "BATAL",
                backgroundColorRes = R.color.primary
            ) {
                viewLifecycleOwner.lifecycleScope.launch {
                     try {
                        binding.progresBar.visibility = View.VISIBLE
                        rentalProductViewModel.deleteProductRental(productRental)
                        rentalProductViewModel.getProductRental(forceRefresh = true)
                        showAlert(
                            title = "Berhasil!",
                            message = "Produk sewa '${productRental.name_rental_item}' berhasil dihapus",
                            backgroundColorRes = R.color.primary,
                            iconRes = R.drawable.success
                        )
                    } catch (e: Exception) {
                        showAlert(
                            title = "Gagal!",
                            message = "Gagal menghapus produk sewa: ${e.message}",
                            backgroundColorRes = R.color.red600,
                            iconRes = R.drawable.failed
                        )
                    } finally {
                        binding.progresBar.visibility = View.GONE
                    }
                }
            }
    }

    private fun fetchDataForTab(position: Int) {
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


    private fun isLaundryTabSelected() = binding.LayoutTabSelected.selectedTabPosition == 0
    private fun isRentalTabSelected() = binding.LayoutTabSelected.selectedTabPosition == 1
}