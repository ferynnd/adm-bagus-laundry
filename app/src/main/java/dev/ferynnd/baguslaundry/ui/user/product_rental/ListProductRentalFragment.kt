package dev.ferynnd.baguslaundry.ui.user.product_rental

import android.os.Bundle
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
import dev.ferynnd.baguslaundry.controller.user.RentalProductAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentListProductRentalBinding
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch

class ListProductRentalFragment : Fragment() {
    private var _binding: FragmentListProductRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var rentalProductAdapter: RentalProductAdapter
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullLaundryList: List<ProductRental> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countProductLaundry: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListProductRentalBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        rentalProductAdapter = RentalProductAdapter { productRental: ProductRental ->
            onDetailClick(productRental)
        }

        binding.recyclerViewProductLaundry.adapter = rentalProductAdapter
        binding.recyclerViewProductLaundry.layoutManager = LinearLayoutManager(requireContext())

        // Dapatkan user dan baru lanjut observe
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val user = userViewModel.getUserById(userId)
                userIdBranch = user.data.id_branch_user!!.toInt()

                // Setelah userIdBranch tersedia, baru observe
                rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { productLaundry ->
                    productLaundry?.let {
                        val filteredList = productLaundry.filter { item ->
                            item.id_branch_rental_item == userIdBranch
                        }

                        // Simpan list untuk pencarian
                        fullLaundryList = filteredList

                        countProductLaundry = filteredList.size
                        binding.countData.text = countProductLaundry.toString()

                        rentalProductAdapter.submitList(filteredList)
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
                    it.name_rental_item!!.lowercase().contains(query)
                }
                rentalProductAdapter.submitList(filtered)
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

    private fun onDetailClick(rentalItem: ProductRental) {
        Toast.makeText(context, "Detail ${rentalItem.id_rental_item} akan ditampilkan", Toast.LENGTH_SHORT).show()
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