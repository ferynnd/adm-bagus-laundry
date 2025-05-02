package dev.ferynnd.baguslaundry.ui.admin.product.rental

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
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
    private lateinit var rentalProductViewModel : RentalProductViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var rentalProductAdapter: RentalProductAdapter

    private var productRentalList: List<ProductRental>? = null
    private var branchList: List<Branch>? = null


   private val groupedData = mutableListOf<Any>()


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

        viewLifecycleOwner.lifecycleScope.launch {

            // 🔁 Observe branches
            branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                branchList = branches
                updateUIIfReady()
                rentalProductAdapter.setBranches(branches)
            }

            // 🔁 Observe products
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
}