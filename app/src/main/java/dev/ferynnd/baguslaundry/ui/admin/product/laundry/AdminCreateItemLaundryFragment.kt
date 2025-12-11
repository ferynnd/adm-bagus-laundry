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
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.model.Branch
import dev.ferynnd.baguslaundry.model.IsActiveLaundryItem
import dev.ferynnd.baguslaundry.model.ProductLaundry
import kotlinx.coroutines.launch
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.FragmentAdminCreateItemLaundryBinding
import dev.ferynnd.baguslaundry.ui.openUserFragment
import dev.ferynnd.baguslaundry.ui.showAlert
import android.util.Log

class AdminCreateItemLaundryFragment : Fragment() {
    private var _binding: FragmentAdminCreateItemLaundryBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var branchViewModel: BranchViewModel

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var productLaundryId : Int? = null
    private var isAdmin: Boolean = false
    private var selectedBranchId: Int? = null
    private var branchList: List<Branch> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())
        laundryProductViewModel = ViewModelProvider(this).get(LaundryProductViewModel::class.java)
        laundryProductViewModel.init(requireContext())
        branchViewModel = ViewModelProvider(this).get(BranchViewModel::class.java)
        branchViewModel.init(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdminCreateItemLaundryBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()
        productLaundryId = arguments?.getInt("productLaundryID")
        isAdmin = arguments?.getBoolean("isAdmin", false) ?: false

        Log.d("AdminCreateLaundry", "User ID: $userId, isAdmin: $isAdmin, productLaundryId: $productLaundryId")

        // Tampilkan/sembunyikan dropdown cabang berdasarkan role
        if (isAdmin) {
            binding.inputLayoutBranch.visibility = View.VISIBLE
        } else {
            binding.inputLayoutBranch.visibility = View.GONE
        }

        // Load data user dan branch
        loadUserData()
        loadBranchData()

        // Load product data jika edit mode
        if (productLaundryId != null && productLaundryId != 0) {
            loadProductData()
        } else {
            binding.textHeaderBold.text = "Layanan Laundry"
            binding.btnSubmit.text = "Simpan Layanan"
        }

        binding.btnSubmit.setOnClickListener {
            if (productLaundryId != null && productLaundryId != 0) {
                updateProductLaundry()
            } else {
                createProductLaundry()
            }
        }

        binding.arrowBack.setOnClickListener {
            if (isAdmin) {
                parentFragmentManager.popBackStack()
            } else {
                openUserFragment(KurirProductFragment(), "KurirProduct")
            }
        }

        hideBottomNavigationView()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("AdminCreateLaundry", "Memuat data user dengan ID: $userId")
                val userResponse = userViewModel.getUserById(userId)

                Log.d("AdminCreateLaundry", "User Response - Success: ${userResponse.success}, Data: ${userResponse.data}")

                if (userResponse.success) {
                    userIdBranch = userResponse.data.id_branch_user?.toInt() ?: 0

                    // Jika bukan admin, set selectedBranchId dari user branch
                    if (!isAdmin) {
                        selectedBranchId = userIdBranch
                    }

                    Log.d("AdminCreateLaundry", "User branch ID: $userIdBranch, Selected branch: $selectedBranchId")
                } else {
                    Log.e("AdminCreateLaundry", "Response tidak sukses: ${userResponse.message}")
                    showAlert(
                        title = "Gagal!",
                        message = "Gagal memuat data pengguna: ${userResponse.message}",
                        backgroundColorRes = R.color.red600,
                        iconRes = R.drawable.failed,
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminCreateLaundry", "Exception saat load user: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat data pengguna: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadBranchData() {
        branchViewModel.branches.observe(viewLifecycleOwner) { branches ->
            Log.d("AdminCreateLaundry", "Branch list loaded: ${branches.size} branches")
            branchList = branches
            setupBranchDropdown(branches)
        }
    }

    private fun loadProductData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("AdminCreateLaundry", "Loading product dengan ID: $productLaundryId")
                val product = laundryProductViewModel.getProductLaundryById(productLaundryId!!)

                binding.inputNamaItem.setText(product.data.name_laundry_item)
                binding.inputHargaItem.setText(product.data.price_laundry_item.toString())
                binding.inputTimeItem.setText(product.data.time_laundry_item)
                selectedBranchId = product.data.id_branch_laundry_item

                // Set selected branch di dropdown
                if (isAdmin) {
                    setBranchDropdownSelection(selectedBranchId)
                }

                binding.textHeaderBold.text = "Perbarui Layanan Laundry"
                binding.btnSubmit.text = "Simpan Pembaruan"

                Log.d("AdminCreateLaundry", "Product loaded - Branch: $selectedBranchId")
            } catch (e: Exception) {
                Log.e("AdminCreateLaundry", "Error loading product: ${e.message}", e)
                showAlert(
                    title = "Gagal!",
                    message = "Gagal memuat data produk: ${e.message}",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                )
            }
        }
    }

    private fun setupBranchDropdown(branches: List<Branch>) {
        if (!isAdmin) return

        val branchNames = branches.map { "${it.name_branch} - ${it.city_branch}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, branchNames)
        binding.autoCompleteBranch.setAdapter(adapter)

        binding.autoCompleteBranch.setOnItemClickListener { _, _, position, _ ->
            selectedBranchId = branches[position].id_branch
            Log.d("AdminCreateLaundry", "Branch selected: ${branches[position].name_branch}, ID: $selectedBranchId")
        }

        // Set default selection jika edit mode
        if (productLaundryId != null && selectedBranchId != null) {
            setBranchDropdownSelection(selectedBranchId)
        }
    }

    private fun setBranchDropdownSelection(branchId: Int?) {
        branchId?.let { id ->
            val branch = branchList.find { it.id_branch == id }
            branch?.let {
                binding.autoCompleteBranch.setText("${it.name_branch} - ${it.city_branch}", false)
                selectedBranchId = id
                Log.d("AdminCreateLaundry", "Branch dropdown set to: ${it.name_branch}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createProductLaundry() {
        val name = binding.inputNamaItem.text.toString()
        val price = binding.inputHargaItem.text.toString()
        val time = binding.inputTimeItem.text.toString()

        if (name.isBlank() || price.isBlank()) {
            showAlert(
                title = "Peringatan!",
                message = "Lengkapi semua inputan",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        // Validasi pemilihan cabang untuk admin
        if (isAdmin && selectedBranchId == null) {
            showAlert(
                title = "Peringatan!",
                message = "Pilih cabang terlebih dahulu",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val branchId = if (isAdmin) selectedBranchId!! else userIdBranch

        Log.d("AdminCreateLaundry", "Creating product for branch: $branchId")

        val dataProductLaundry = ProductLaundry(
            id_branch_laundry_item = branchId,
            price_laundry_item = price.toBigDecimalOrNull(),
            name_laundry_item = name,
            time_laundry_item = time,
            is_active_laundry_item = IsActiveLaundryItem.active,
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                laundryProductViewModel.createProductLaundry(dataProductLaundry)
                showAlert(
                    title = "Berhasil!",
                    message = "Layanan berhasil dibuat",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.success
                )

                if (isAdmin) {
                    parentFragmentManager.popBackStack()
                } else {
                    openUserFragment(KurirProductFragment(), "KurirProduct")
                }
            } catch (e: Exception) {
                Log.e("AdminCreateLaundry", "Error creating product: ${e.message}", e)
                showAlert(
                    title = "Gagal!",
                    message = "Gagal membuat layanan ${e.message}",
                    backgroundColorRes = R.color.red600,
                    iconRes = R.drawable.failed,
                    duration = 4000
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateProductLaundry() {
        val name = binding.inputNamaItem.text.toString()
        val price = binding.inputHargaItem.text.toString()
        val time = binding.inputTimeItem.text.toString()

        if (name.isBlank() || price.isBlank() || time.isBlank()) {
            showAlert(
                title = "Peringatan!",
                message = "Lengkapi semua inputan",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        // Validasi pemilihan cabang untuk admin
        if (isAdmin && selectedBranchId == null) {
            showAlert(
                title = "Peringatan!",
                message = "Pilih cabang terlebih dahulu",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val branchId = if (isAdmin) selectedBranchId!! else userIdBranch

        Log.d("AdminCreateLaundry", "Updating product ID: $productLaundryId for branch: $branchId")

        val dataProductLaundry = ProductLaundry(
            id_laundry_item = productLaundryId,
            id_branch_laundry_item = branchId,
            price_laundry_item = price.toBigDecimalOrNull(),
            name_laundry_item = name,
            time_laundry_item = time,
            is_active_laundry_item = IsActiveLaundryItem.active,
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                laundryProductViewModel.updateProductLaundry(dataProductLaundry)
                showAlert(
                    title = "Berhasil!",
                    message = "Data layanan berhasil diperbarui",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.success
                )

                if (isAdmin) {
                    parentFragmentManager.popBackStack()
                } else {
                    openUserFragment(KurirProductFragment(), "KurirProduct")
                }
            } catch (e: Exception) {
                Log.e("AdminCreateLaundry", "Error updating product: ${e.message}", e)
                showAlert(
                    title = "Gagal!",
                    message = "Gagal memperbarui data layanan ${e.message}",
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