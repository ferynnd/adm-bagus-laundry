package dev.ferynnd.baguslaundry.ui.user.product_laundry

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.LaundryProductAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListProductLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import kotlin.collections.isNotEmpty

class ListProductLaundryFragment : Fragment() {
    private var _binding: KurirFragmentListProductLaundryBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var laundryProductAdapter: LaundryProductAdapter
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullLaundryList: List<ProductLaundry> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countProductLaundry: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        laundryProductViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java]
        laundryProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentListProductLaundryBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        laundryProductAdapter = LaundryProductAdapter()

        binding.recyclerViewProductLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryProductAdapter
        }

        // Dapatkan user dan baru lanjut observe
//        if (userId != 0) {
//            viewLifecycleOwner.lifecycleScope.launch {
//                try {
//                    val user = userViewModel.getUserById(userId)
//                    userIdBranch = user.data.id_branch_user!!.toInt()
//
//
//                    laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { productLaundry ->
//                        val filteredList = productLaundry.filter { item ->
//                            item.id_branch_laundry_item == userIdBranch
//                        }
//
//                        fullLaundryList = filteredList
////
//                        countProductLaundry = filteredList.size
//                        binding.countData.text = countProductLaundry.toString()
//
//                        if (filteredList.isNotEmpty()) {
//                            binding.recyclerViewProductLaundry.visibility = View.VISIBLE
//                            binding.containerDataNotFound.visibility = View.GONE
//
//                            laundryProductAdapter.submitList(filteredList)
//                        } else {
//                            binding.recyclerViewProductLaundry.visibility = View.GONE
//                            binding.containerDataNotFound.visibility = View.VISIBLE
//                        }
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    Toast.makeText(
//                        requireContext(),
//                        "Gagal memuat data: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        } else {
//            Toast.makeText(context, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
//        }

        // Fungsi pencarian
//        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                val query = newText.orEmpty().lowercase()
//                val filtered = fullLaundryList.filter {
//                    it.name_laundry_item!!.lowercase().contains(query)
//                }
//
//                if (filtered.isNotEmpty()) {
//                    binding.recyclerViewProductLaundry.visibility = View.VISIBLE
//                    binding.containerDataNotFound.visibility = View.GONE
//
//                    laundryProductAdapter.submitList(filtered)
//                } else {
//                    binding.recyclerViewProductLaundry.visibility = View.GONE
//                    binding.containerDataNotFound.visibility = View.VISIBLE
//                }
//
//                binding.searchView.setIconifiedByDefault(false)
//                binding.countData.text = filtered.size.toString()
//                return true
//            }
//        })
//
//        binding.arrowBack.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.host_fragment_user, UserDashboardFragment())
//                .addToBackStack(null)
//                .commit()
//        }

        return binding.root
    }

    // Menetapkan binding ke null saat tampilan dihancurkan untuk menghindari memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}