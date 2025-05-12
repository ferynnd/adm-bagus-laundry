package dev.ferynnd.baguslaundry.ui.admin.product.rental

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.baguslaundry.controller.RentalProductAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListProductRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.Status
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import kotlinx.coroutines.launch


class AdminListProductRentalFragment : Fragment() {


    private lateinit var binding: FragmentAdminListProductRentalBinding
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var rentalProductAdapter: RentalProductAdapter

    private var productRentalList: List<ProductRental>? = null
    private var branchList: List<Branch>? = null

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
        // Inflate the layout for this fragment
        rentalProductAdapter = RentalProductAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalProductAdapter
        }


        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                    if (selectedBranch.id_branch == -1) {
                        rentalProductViewModel.filterClient(null) // Semua Cabang
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


        }



        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminDashboardFragment())
                .commit()
        }

        return binding.root
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
                    address_branch = "",
                    city_branch = "",
                    is_active_branch = Status.active,
                    deleted_at = ""
                )

                tempGroupedData.add(unknownBranch)
                tempGroupedData.addAll(products)
            }
        }

        rentalProductAdapter.submitList(tempGroupedData)
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
        // Menentukan tinggi bottom sheet menjadi sepertiga dari tinggi layar perangkat
        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams


        bottomSheetDialog.show()
    }

}