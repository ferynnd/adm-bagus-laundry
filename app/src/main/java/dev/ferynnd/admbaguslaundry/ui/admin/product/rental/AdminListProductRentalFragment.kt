package dev.ferynnd.admbaguslaundry.ui.admin.product.rental

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.admbaguslaundry.controller.RentalProductAdapter
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminListProductRentalBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.ProductRental
import dev.ferynnd.admbaguslaundry.model.Status
import dev.ferynnd.admbaguslaundry.ui.showAlert
import dev.ferynnd.admbaguslaundry.ui.user.AdminCreateItemRentalFragment
import kotlinx.coroutines.launch

class AdminListProductRentalFragment : Fragment() {

    private lateinit var binding: FragmentAdminListProductRentalBinding
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var rentalProductAdapter: RentalProductAdapter

    private var productRentalList: List<ProductRental>? = null
    private var branchList: List<Branch>? = null

      private var currentPage = 1
    private var lastPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rentalProductViewModel = ViewModelProvider(this).get(RentalProductViewModel::class.java)
        rentalProductViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListProductRentalBinding.inflate(layoutInflater)

        rentalProductAdapter = RentalProductAdapter(
            onEditClick = { productRental ->
                navigateToEditItem(productRental)
            },
            onDeleteClick = { productRental ->
                showDeleteConfirmationDialog(productRental)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalProductAdapter
        }

        // Tombol Create Item
        binding.btnCreateItem.setOnClickListener {
            navigateToCreateItem()
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                    if (selectedBranch.id_branch == -1) {
                        rentalProductViewModel.filterClient(null)
                    } else {
                        rentalProductViewModel.filterClient(selectedBranch.id_branch)
                    }
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { rentalProductViewModel.searchRentalProducts(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                rentalProductViewModel.searchRentalProducts(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            rentalProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }

            rentalProductViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                if (errorMessage.isNotBlank()) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    rentalProductViewModel.resetErrorMessage()
                }
            }

            rentalProductViewModel.filteredRentalProducts.observe(viewLifecycleOwner) { filteredProducts ->
                rentalProductAdapter.submitList(filteredProducts)
            }

            branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                branchList = branches
                updateUIIfReady()
                rentalProductAdapter.setBranches(branches)
            }

            rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { products ->
                productRentalList = products
                updateUIIfReady()
            }

             rentalProductViewModel.pagination.observe(viewLifecycleOwner) { pagination ->
                currentPage = pagination.current_page
                lastPage = pagination.last_page

                binding.textPageInfo.text = "$currentPage / $lastPage"

                binding.btnPrevPage.isEnabled = currentPage > 1
                binding.btnNextPage.isEnabled = currentPage < lastPage

                binding.btnPrevPage.alpha = if (currentPage > 1) 1f else 0.4f
                binding.btnNextPage.alpha = if (currentPage < lastPage) 1f else 0.4f
            }
        }

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                rentalProductViewModel.getProductRentalPage(currentPage - 1)
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < lastPage) {
                rentalProductViewModel.getProductRentalPage(currentPage + 1)
            }
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

    private fun navigateToCreateItem() {
        val fragment = AdminCreateItemRentalFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_admin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToEditItem(productRental: ProductRental) {
        val fragment = AdminCreateItemRentalFragment()
        val bundle = Bundle().apply {
            putInt("productRentalID", productRental.id_rental_item ?: 0)
        }
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_admin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateUIIfReady() {
        val products = productRentalList
        val branches = branchList

        if (!products.isNullOrEmpty() && !branches.isNullOrEmpty()) {
            setProductRental(products)
        }
    }

    private fun setProductRental(newProductRentals: List<ProductRental>) {
        val tempGroupedData = mutableListOf<Any>()
        val branchList = branchViewModel.branches.value ?: emptyList()
        val groupedMap = newProductRentals.groupBy { it.id_branch_rental_item }

        for ((branchId, products) in groupedMap) {
            val branch = branchList.find { it.id_branch == branchId }
            if (branch != null) {
                tempGroupedData.add(branch)
                tempGroupedData.addAll(products)
            } else {
                val unknownBranch = Branch(
                    id_branch = branchId,
                    name_branch = "UNKNOWN",
                    city_branch = "",
                    is_active_branch = Status.active,
                )
                tempGroupedData.add(unknownBranch)
                tempGroupedData.addAll(products)
            }
        }
        rentalProductAdapter.submitList(tempGroupedData)
    }


    private fun showDeleteConfirmationDialog(productRental: ProductRental) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus Produk Sewa")
            .setMessage("Apakah Anda yakin ingin menghapus produk \"${productRental.name_rental_item}\"?")
            .setPositiveButton("Hapus") { dialog, _ ->
                deleteProductRental(productRental)
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteProductRental(productRental: ProductRental) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                rentalProductViewModel.deleteProductRental(productRental)

                showAlert(
                    title = "Berhasil!",
                    message = "Produk sewa berhasil dihapus",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.success
                )

                rentalProductViewModel.getProductRentalPage(currentPage)
            } catch (e: Exception) {
                showAlert(
                    title = "Gagal!",
                    message = "Gagal menghapus produk sewa: ${e.message}",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                    duration = 4000
                )
            }
        }
    }
    private fun showFilterBottomSheet(
        context: Context,
        items: List<Branch>,
        onBranchSelected: (Branch) -> Unit
    ) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_filter_branch, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFilterBranch)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = FilterBranchAdapter { selectedBranch ->
            onBranchSelected(selectedBranch)
            bottomSheetDialog.dismiss()
        }

        recyclerView.adapter = adapter
        adapter.submitList(items)

        bottomSheetDialog.setContentView(view)
        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams

        bottomSheetDialog.show()
    }
}