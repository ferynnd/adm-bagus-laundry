package dev.ferynnd.baguslaundry.ui.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.ClientAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListClientBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminListClientFragment : Fragment() {

    private lateinit var binding: FragmentAdminListClientBinding
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var clientAdapter: ClientAdapter

    private lateinit var branchViewModel: BranchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAdminListClientBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        clientAdapter = ClientAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clientAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            branchViewModel.branches.observe(viewLifecycleOwner) { branchList ->
                clientAdapter.setBranches(branchList)
            }
            clientViewModel.clients.observe(viewLifecycleOwner) { client ->
                client?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (client.isNotEmpty()) {
                            clientAdapter.submitList(client)
//                              setProduct(products)
                        } else {
                            clientAdapter.submitList(emptyList())
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

}



