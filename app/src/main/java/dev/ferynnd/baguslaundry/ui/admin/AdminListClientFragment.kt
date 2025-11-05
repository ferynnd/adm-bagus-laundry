package dev.ferynnd.baguslaundry.ui.admin

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
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.ClientAdapter
import dev.ferynnd.baguslaundry.controller.FilterBranchAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListClientBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.ui.openAdminFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminListClientFragment : Fragment() {

    private lateinit var binding: FragmentAdminListClientBinding
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var clientAdapter: ClientAdapter

    private lateinit var branchViewModel: BranchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java).apply {
            init(requireContext())
        }
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java).apply {
            init(requireContext())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAdminListClientBinding.inflate(layoutInflater)
        clientAdapter = ClientAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clientAdapter
        }

        binding.btnRoutes.setOnClickListener {
            branchViewModel.branches.value?.let { branches ->
                showFilterBottomSheet(requireContext(), branches) { selectedBranch ->
                   val id = if (selectedBranch.id_branch == -1) null else selectedBranch.id_branch
                        clientViewModel.onBranchFilterSelected(id)
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
               clientViewModel.onSearchQueryChanged(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                 branchViewModel.onSearchQueryChanged(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            clientViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }

            clientViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                if (errorMessage.isNotBlank()) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    clientViewModel.resetErrorMessage()
                }
            }

            branchViewModel.branches.observe(viewLifecycleOwner) { branchList ->
                clientAdapter.setBranches(branchList)
            }
            clientViewModel.filteredClients.observe(viewLifecycleOwner) { filteredClients ->
                clientAdapter.submitList(filteredClients)
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