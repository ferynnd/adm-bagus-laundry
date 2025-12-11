package dev.ferynnd.baguslaundry.ui.user

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminCreateItemRentalBinding
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.IsActiveRental
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.ui.openUserFragment
import dev.ferynnd.baguslaundry.ui.showAlert
import kotlinx.coroutines.launch

class AdminCreateItemRentalFragment : Fragment() {
    private var _binding: FragmentAdminCreateItemRentalBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private lateinit var userViewModel: UserViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var branchViewModel: BranchViewModel

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var productRentalId: Int? = null
    private var branchList: List<Branch> = emptyList()
    private var selectedBranchId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())
        rentalProductViewModel = ViewModelProvider(this).get(RentalProductViewModel::class.java)
        rentalProductViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdminCreateItemRentalBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        viewLifecycleOwner.lifecycleScope.launch {
            loadUserData()
            loadBranchData()
        }

        productRentalId = arguments?.getInt("productRentalID")

        if (productRentalId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val client = rentalProductViewModel.getProductRentalById(productRentalId!!)
                binding.inputNamaItem.setText(client.data.name_rental_item)
                binding.inputHargaItem.setText(client.data.price_rental_item.toString())
                selectedBranchId = client.data.id_branch_rental_item!!
                binding.textHeaderBold.text = "Perbarui Produk Sewa"
                binding.btnSubmit.text = "Simpan Pembaruan"

                // Set spinner selection after branches loaded
                setupBranchSpinner()
            }
        } else {
            binding.textHeaderBold.text = "Produk Sewa"
            binding.btnSubmit.text = "Simpan Produk"
        }

        binding.btnSubmit.setOnClickListener {
            if (productRentalId != null) {
                updateProductRental()
            } else {
                createProductRental()
            }
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        hideBottomNavigationView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun loadUserData() {
        try {
            val userResponse = userViewModel.getUserById(userId)
            if (userResponse.success) {
                userIdBranch = userResponse.data.id_branch_user!!.toInt()
                selectedBranchId = userIdBranch // Set default selected branch
            } else {
                showAlert(
                    title = "Gagal!",
                    message = "Gagal memuat data pengguna",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Gagal memuat data pengguna: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private suspend fun loadBranchData() {
        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            branchList = branches
            setupBranchSpinner()
        }
    }

    private fun setupBranchSpinner() {
        if (branchList.isEmpty()) return

        val branchNames = branchList.map { it.name_branch }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            branchNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBranch.adapter = adapter

        // Set selection based on selectedBranchId
        val selectedIndex = branchList.indexOfFirst { it.id_branch == selectedBranchId }
        if (selectedIndex != -1) {
            binding.spinnerBranch.setSelection(selectedIndex)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createProductRental() {
        val name = binding.inputNamaItem.text.toString()
        val price = binding.inputHargaItem.text.toString()
        val selectedBranchPosition = binding.spinnerBranch.selectedItemPosition

        if (name.isBlank() || price.isBlank()) {
            showAlert(
                title = "Peringatan!",
                message = "Lengkapi semua inputan",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val selectedBranch = branchList[selectedBranchPosition]

        val dataProductRental = ProductRental(
            id_branch_rental_item = selectedBranch.id_branch,
            price_rental_item = price.toIntOrNull(),
            name_rental_item = name,
            is_active_rental_item = IsActiveRental.active,
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                rentalProductViewModel.createProductRental(dataProductRental)
                showAlert(
                    title = "Berhasil!",
                    message = "Produk berhasil dibuat",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.success
                )
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                showAlert(
                    title = "Gagal!",
                    message = "Gagal membuat produk ${e.message}",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                    duration = 4000
                )
            }
        }
    }

    private fun updateProductRental() {
        val name = binding.inputNamaItem.text.toString()
        val price = binding.inputHargaItem.text.toString()
        val selectedBranchPosition = binding.spinnerBranch.selectedItemPosition
        val productRentalId = arguments?.getInt("productRentalID") ?: 0

        if (name.isBlank() || price.isBlank()) {
            showAlert(
                title = "Peringatan!",
                message = "Lengkapi semua inputan",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val selectedBranch = branchList[selectedBranchPosition]

        val dataProductRental = ProductRental(
            id_rental_item = productRentalId,
            id_branch_rental_item = selectedBranch.id_branch,
            price_rental_item = price.toIntOrNull(),
            name_rental_item = name,
            is_active_rental_item = IsActiveRental.active,
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                rentalProductViewModel.updateProductRental(dataProductRental)
                showAlert(
                    title = "Berhasil!",
                    message = "Data produk berhasil diperbarui",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.success
                )
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                showAlert(
                    title = "Gagal!",
                    message = "Gagal memperbarui data produk ${e.message}",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                    duration = 4000
                )
            }
        }
    }

    private fun hideBottomNavigationView() {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
    }
}