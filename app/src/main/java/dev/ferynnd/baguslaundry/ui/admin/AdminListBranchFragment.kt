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
import dev.ferynnd.baguslaundry.controller.BranchAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminListBranchBinding
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


//        val searchView = binding.searchView
//        val listView = binding.listView
//
////        val searchText = searchView.findViewById<android.widget.EditText>(search_src_text)
//        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
//        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
//
////         Ubah warna teks yang diinput dan warna hint
//        searchText.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue900))      // Warna teks input
//        searchText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.gray900))   // Warna hint
//
//        searchIcon.visibility = View.GONE
//
//        val listName = arrayOf("Arman", "Ansar", "Akash", "Sudish", "nando")
//
//        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listName)
//        listView.adapter = arrayAdapter
//
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//
//                arrayAdapter.filter.filter(newText) { count ->
//                    listView.isVisible = count > 0
//                }
//
//                if (newText.isNullOrEmpty()) {
//                    listView.isGone = true
//                } else {
//                    listView.isVisible = true
//                }
//                return true
//            }
//
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return false
//            }
//        })


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = branchAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            branchViewModel.branches.observe(viewLifecycleOwner) { branch ->
                  branch?.let {
                      lifecycleScope.launch(Dispatchers.Main) {
                          if(branch.isNotEmpty()){
                              branchAdapter.submitList(branch)
//                              setProduct(products)
                          }else{
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



}