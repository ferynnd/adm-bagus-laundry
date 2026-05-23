package dev.ferynnd.admbaguslaundry.ui.admin

import android.content.Context
import android.os.Bundle
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
import dev.ferynnd.admbaguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.admbaguslaundry.controller.UserAdapter
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminListUserBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import kotlinx.coroutines.launch

class AdminListUserFragment : Fragment() {

    private lateinit var binding: FragmentAdminListUserBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var userAdapter: UserAdapter
    private lateinit var branchViewModel: BranchViewModel

    private var currentPage = 1
    private var lastPage = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
        userViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAdminListUserBinding.inflate(layoutInflater)
        userAdapter = UserAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                    val id = if (selectedBranch.id_branch == -1) null else selectedBranch.id_branch
                        userViewModel.onBranchFilterSelected(id)
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                userViewModel.onSearchQueryChanged(query.orEmpty())
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                userViewModel.onSearchQueryChanged(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }

            userViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                if (errorMessage.isNotBlank()) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    userViewModel.resetErrorMessage()
                }
            }

            branchViewModel.branches.observe(viewLifecycleOwner) { branchList ->
                userAdapter.setBranches(branchList)
            }
            userViewModel.filteredUsers.observe(viewLifecycleOwner) { filteredUsers ->
                userAdapter.submitList(filteredUsers)
            }
        }

        userViewModel.pagination.observe(viewLifecycleOwner) { pagination ->
            currentPage = pagination.current_page
            lastPage = pagination.last_page

            binding.textPageInfo.text = "$currentPage / $lastPage"

            binding.btnPrevPage.isEnabled = currentPage > 1
            binding.btnNextPage.isEnabled = currentPage < lastPage

            binding.btnPrevPage.alpha = if (currentPage > 1) 1f else 0.4f
            binding.btnNextPage.alpha = if (currentPage < lastPage) 1f else 0.4f
        }

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                userViewModel.getUser(currentPage - 1)
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < lastPage) {
                userViewModel.getUser(currentPage + 1)
            }
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.popBackStack()
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