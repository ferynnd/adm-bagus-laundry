package dev.ferynnd.baguslaundry.ui.user.product_laundry

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.LaundryProductAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_NAME
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentListProductLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListProductLaundryFragment : Fragment() {
    private var _binding: FragmentListProductLaundryBinding? = null
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
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        laundryProductViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListProductLaundryBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        laundryProductAdapter = LaundryProductAdapter { productLaundry: ProductLaundry ->
            onDetailClick(productLaundry)
        }

        binding.recyclerViewProductLaundry.adapter = laundryProductAdapter
        binding.recyclerViewProductLaundry.layoutManager = LinearLayoutManager(requireContext())

        // Dapatkan user dan baru lanjut observe
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val user = userViewModel.getUserById(userId)
                userIdBranch = user.data.id_branch_user!!.toInt()

                // Setelah userIdBranch tersedia, baru observe
                laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { productLaundry ->
                    productLaundry?.let {
                        val filteredList = productLaundry.filter { item ->
                            item.id_branch_laundry_item == userIdBranch
                        }

                        // Simpan list untuk pencarian
                        fullLaundryList = filteredList

                        countProductLaundry = filteredList.size
                        binding.countData.text = countProductLaundry.toString()

                        laundryProductAdapter.submitList(filteredList)
                    }
                }
            }
        }

        // Fungsi pencarian
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // kita proses real-time, jadi tidak perlu submit
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty().lowercase()
                val filtered = fullLaundryList.filter {
                    it.name_laundry_item!!.lowercase().contains(query)
                }
                laundryProductAdapter.submitList(filtered)
                binding.searchView.setIconifiedByDefault(false)
                binding.countData.text = filtered.size.toString()
                return true
            }
        })

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, UserDashboardFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    // Menetapkan binding ke null saat tampilan dihancurkan untuk menghindari memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onDetailClick(itemLaundry: ProductLaundry) {
        Toast.makeText(context, "Detail ${itemLaundry.id_laundry_item} akan ditampilkan", Toast.LENGTH_SHORT).show()
//        val bundle = Bundle().apply {
//            putLong("supplierId", supplier.id_supplier)  // Mengirimkan ID supplier ke fragment berikutnya
//        }
//        val detailFragment = DetailSupplierFragment()
//        detailFragment.arguments = bundle  // Menetapkan argumen untuk fragment detail
//
//        parentFragmentManager.beginTransaction()
//            .replace(R.id.FragmentMenu, detailFragment)  // Mengganti fragment saat ini dengan DetailSupplierFragment
//            .addToBackStack(null)  // Menambahkan transaksi ke back stack agar pengguna bisa kembali
//            .commit()  // Menyelesaikan transaksi
    }
}