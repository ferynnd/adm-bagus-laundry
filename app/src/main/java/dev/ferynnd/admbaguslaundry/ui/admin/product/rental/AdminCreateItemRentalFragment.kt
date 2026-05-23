package dev.ferynnd.admbaguslaundry.ui.user

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminCreateItemRentalBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.IsActiveRental
import dev.ferynnd.admbaguslaundry.model.ProductRental
import dev.ferynnd.admbaguslaundry.ui.showAlert
import kotlinx.coroutines.launch

class AdminCreateItemRentalFragment : Fragment() {

    private var _binding: FragmentAdminCreateItemRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var branchViewModel: BranchViewModel

    private var productRentalId: Int? = null
    private var branchList: List<Branch> = emptyList()
    private var selectedBranchId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
        rentalProductViewModel.init(requireContext())

        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        branchViewModel.init(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminCreateItemRentalBinding.inflate(inflater, container, false)

        productRentalId = arguments?.getInt("productRentalID")

        loadBranchData()

        if (productRentalId != null && productRentalId != 0) {
            loadProductData()
        } else {
            binding.textHeaderBold.text = "Produk Sewa"
            binding.btnSubmit.text = "Simpan Produk"
        }

        binding.btnSubmit.setOnClickListener {
            if (productRentalId != null && productRentalId != 0) {
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

    private fun loadBranchData() {
        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            branchList = branches
            setupBranchSpinner()

            if (selectedBranchId != null) {
                setBranchSpinnerSelection(selectedBranchId)
            }
        }
    }

    private fun loadProductData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = rentalProductViewModel.getProductRentalById(productRentalId!!)

                if (response.success && response.data != null) {
                    binding.inputNamaItem.setText(response.data.name_rental_item)
                    binding.inputHargaItem.setText(response.data.price_rental_item.toString())

                    selectedBranchId = response.data.id_branch_rental_item
                    setBranchSpinnerSelection(selectedBranchId)

                    binding.textHeaderBold.text = "Perbarui Produk Sewa"
                    binding.btnSubmit.text = "Simpan Pembaruan"
                } else {
                    showAlert(
                        title = "Gagal!",
                        message = "Data produk tidak ditemukan",
                        backgroundColorRes = R.color.red600,
                        iconRes = R.drawable.failed,
                    )
                }
            } catch (e: Exception) {
                showAlert(
                    title = "Gagal!",
                    message = "Gagal memuat data produk: ${e.message}",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                )
            }
        }
    }

    private fun setupBranchSpinner() {
        if (branchList.isEmpty()) return

        val branchNames = branchList.map {
            "${it.name_branch} - ${it.city_branch}"
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            branchNames
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBranch.adapter = adapter

        binding.spinnerBranch.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedBranchId = branchList[position].id_branch
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    selectedBranchId = null
                }
            }
        )
    }

    private fun setBranchSpinnerSelection(branchId: Int?) {
        if (branchId == null || branchList.isEmpty()) return

        val selectedIndex = branchList.indexOfFirst { it.id_branch == branchId }

        if (selectedIndex != -1) {
            binding.spinnerBranch.setSelection(selectedIndex)
            selectedBranchId = branchId
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createProductRental() {
        val name = binding.inputNamaItem.text.toString()
        val price = binding.inputHargaItem.text.toString()

        if (name.isBlank() || price.isBlank()) {
            showAlert(
                title = "Peringatan!",
                message = "Lengkapi semua inputan",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        if (selectedBranchId == null) {
            showAlert(
                title = "Peringatan!",
                message = "Pilih cabang terlebih dahulu",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val dataProductRental = ProductRental(
            id_branch_rental_item = selectedBranchId,
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

        if (name.isBlank() || price.isBlank()) {
            showAlert(
                title = "Peringatan!",
                message = "Lengkapi semua inputan",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        if (selectedBranchId == null) {
            showAlert(
                title = "Peringatan!",
                message = "Pilih cabang terlebih dahulu",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val dataProductRental = ProductRental(
            id_rental_item = productRentalId,
            id_branch_rental_item = selectedBranchId,
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
    }
}