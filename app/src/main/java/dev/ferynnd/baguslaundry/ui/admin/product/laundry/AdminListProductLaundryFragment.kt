package dev.ferynnd.baguslaundry.ui.admin.product.laundry

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.baguslaundry.controller.LaundryProductAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListProductLaundryBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.Status
import dev.ferynnd.baguslaundry.ui.admin.AdminDashboardFragment
import kotlinx.coroutines.launch

class AdminListProductLaundryFragment : Fragment()  {

    private lateinit var binding: FragmentAdminListProductLaundryBinding
    private lateinit var laundryProductViewModel : LaundryProductViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var laundryProductAdapter: LaundryProductAdapter

    private var productLaundryList: List<ProductLaundry>? = null
    private var branchList: List<Branch>? = null

    private val groupedData = mutableListOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        laundryProductViewModel = ViewModelProvider(this).get(LaundryProductViewModel::class.java)
        laundryProductViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListProductLaundryBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        laundryProductAdapter = LaundryProductAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryProductAdapter
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                    if (selectedBranch.id_branch == -1) {
                        laundryProductViewModel.filterClient(null) // Semua Cabang
                    } else {
                        laundryProductViewModel.filterClient(selectedBranch.id_branch)
                    }
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { laundryProductViewModel.searchLaundryProducts(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                laundryProductViewModel.searchLaundryProducts(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe loading state
            laundryProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }

            // Observe error messages
            laundryProductViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                // Periksa apakah pesan error tidak kosong
                if (errorMessage.isNotBlank()) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    // Panggil fungsi reset di ViewModel
                    laundryProductViewModel.resetErrorMessage()
                }
            }

            laundryProductViewModel.filteredProductLaundry.observe(viewLifecycleOwner) { filteredProducts ->
                laundryProductAdapter.submitList(filteredProducts)
            }

            branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
                branchList = branches
                updateUIIfReady()
                laundryProductAdapter.setBranches(branches)
            }

            laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { products ->
                productLaundryList = products
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
        val products = productLaundryList
        val branches = branchList

        if (!products.isNullOrEmpty() && !branches.isNullOrEmpty()) {
            Log.d("LaundryFragment", "Kedua data tersedia, memproses display")
            setProductLaundry(products)
        }
    }

    private fun setProductLaundry(newProductLaundrys: List<ProductLaundry>) {
        groupedData.clear()

        val branchList = branchViewModel.branches.value ?: emptyList()
        Log.d("LaundryFragment", "Jumlah branch: ${branchList.size}")

        val groupedMap = newProductLaundrys.groupBy { it.id_branch_laundry_item }

        for ((branchId, products) in groupedMap) {
            val branch = branchList.find { it.id_branch == branchId }

            if (branch != null) {
                groupedData.add(branch)
                groupedData.addAll(products)
            } else {
                // 💡 Branch dummy dengan nama UNKNOWN
                val unknownBranch = Branch(
                    id_branch = branchId,
                    name_branch = "UNKNOWN",
                    city_branch = "",
                    is_active_branch = Status.active,
                    deleted_at = ""
                )

                groupedData.add(unknownBranch)
                groupedData.addAll(products)
            }
        }
        laundryProductAdapter.submitList(groupedData)
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