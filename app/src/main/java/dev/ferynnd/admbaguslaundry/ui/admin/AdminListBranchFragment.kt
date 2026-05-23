package dev.ferynnd.admbaguslaundry.ui.admin

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast // Import Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.controller.BranchAdapter
import dev.ferynnd.admbaguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminListBranchBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import kotlinx.coroutines.launch


class AdminListBranchFragment : Fragment() {

    private lateinit var binding: FragmentAdminListBranchBinding
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var branchAdapter: BranchAdapter

    private var currentPage = 1
    private var lastPage = 1

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
        branchAdapter = BranchAdapter()


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = branchAdapter
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selected ->
                    val id = if (selected.id_branch == -1) null else selected.id_branch
                    branchViewModel.onBranchFilterSelected(id)
                }
            }
        }

       binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                branchViewModel.onSearchQueryChanged(query.orEmpty())
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                branchViewModel.onSearchQueryChanged(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            branchViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }

            branchViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                if (errorMessage.isNotBlank()) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    branchViewModel.resetErrorMessage()
                }
            }
            branchViewModel.filteredBranches.observe(viewLifecycleOwner) { filteredBranches ->
                branchAdapter.submitList(filteredBranches)
                Log.d("LIST_BRANCH", filteredBranches.toString())
            }
        }

        branchViewModel.pagination.observe(viewLifecycleOwner) { pagination ->
            currentPage = pagination.current_page
            lastPage = pagination.last_page

            binding.textPageInfo.text = "$currentPage / $lastPage"

            binding.btnPrevPage.isEnabled = currentPage > 1
            binding.btnNextPage.isEnabled = currentPage < lastPage

            binding.btnPrevPage.alpha = if (currentPage > 1) 1f else 0.4f
            binding.btnNextPage.alpha = if (currentPage < lastPage) 1f else 0.4f
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                branchViewModel.getBranch(currentPage)
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < lastPage) {
                currentPage++
                branchViewModel.getBranch(currentPage)
            }
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
        val layoutParams = bottomSheetDialog.window?.attributes
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        bottomSheetDialog.window?.attributes = layoutParams


        bottomSheetDialog.show()
    }
}