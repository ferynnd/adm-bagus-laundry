package dev.ferynnd.baguslaundry.ui.admin

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
import dev.ferynnd.baguslaundry.controller.BranchAdapter
import dev.ferynnd.baguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListBranchBinding
import dev.ferynnd.baguslaundry.model.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AdminListBranchFragment : Fragment() {

    private lateinit var binding: FragmentAdminListBranchBinding
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var branchAdapter: BranchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListBranchBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        branchAdapter = BranchAdapter()


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = branchAdapter
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                    if (selectedBranch.id_branch == -1) {
                        branchViewModel.filterClient(null) // Semua Cabang
                    } else {
                        branchViewModel.filterClient(selectedBranch.id_branch)
                    }
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { branchViewModel.searchBranches(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                branchViewModel.searchBranches(newText.orEmpty())
                return true
            }
        })


        viewLifecycleOwner.lifecycleScope.launch {
            branchViewModel.filteredBranches.observe(viewLifecycleOwner) { filteredBranches ->
                branchAdapter.submitList(filteredBranches)
            }
            branchViewModel.branches.observe(viewLifecycleOwner) { branch ->
                branch?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (branch.isNotEmpty()) {
                            branchAdapter.submitList(branch)
//                              setProduct(products)
                        } else {
                            branchAdapter.submitList(emptyList())
//                              productAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }

        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_admin, AdminDashboardFragment())
                .commit()
        }

        return binding.root
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