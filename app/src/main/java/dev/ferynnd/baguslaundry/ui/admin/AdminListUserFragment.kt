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
import dev.ferynnd.baguslaundry.controller.UserAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListUserBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminListUserFragment : Fragment() {

    private lateinit var binding: FragmentAdminListUserBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var userAdapter: UserAdapter

    private lateinit var branchViewModel: BranchViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAdminListUserBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        userAdapter = UserAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            branchViewModel.branches.observe(viewLifecycleOwner) { branchList ->
                userAdapter.setBranches(branchList)
            }
            userViewModel.users.observe(viewLifecycleOwner) { user ->
                user?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (user.isNotEmpty()) {
                            userAdapter.submitList(user)
//                              setProduct(products)
                        } else {
                            userAdapter.submitList(emptyList())
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



