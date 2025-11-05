package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentCreateItemLaundryBinding
import dev.ferynnd.baguslaundry.model.IsActiveLaundryItem
import dev.ferynnd.baguslaundry.model.ProductLaundry
import kotlinx.coroutines.launch
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.ui.openUserFragment
import dev.ferynnd.baguslaundry.ui.showAlert

class CreateItemLaundryFragment : Fragment() {
    private var _binding: KurirFragmentCreateItemLaundryBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryProductViewModel: LaundryProductViewModel

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var productLaundryId : Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        laundryProductViewModel = ViewModelProvider(this).get(LaundryProductViewModel::class.java)
        laundryProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentCreateItemLaundryBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()
        productLaundryId = arguments?.getInt("productLaundryID")

        viewLifecycleOwner.lifecycleScope.launch {
            loadUserData()
        }

        if (productLaundryId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val product = laundryProductViewModel.getProductLaundryById(productLaundryId!!)
                binding.inputNamaItem.setText(product.data.name_laundry_item)
                binding.inputHargaItem.setText(product.data.price_laundry_item.toString())
                binding.inputTimeItem.setText(product.data.time_laundry_item)
                binding.textHeaderBold.text = "Perbarui Layanan Laundry"
                binding.btnSubmit.text = "Simpan Pembaruan"
            }
        } else {
            binding.textHeaderBold.text = "Layanan Laundry"
            binding.btnSubmit.text = "Simpan Layanan"
        }

        binding.btnSubmit.setOnClickListener {
            if (productLaundryId != null) {
                updateProductLaundry()
            } else {
                createProductLaundry()
            }
        }

        binding.arrowBack.setOnClickListener {
             openUserFragment(KurirProductFragment(), "KurirProduct")
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

        val dataProductLaundry = ProductLaundry(
            id_branch_laundry_item = userIdBranch,
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
                openUserFragment(KurirProductFragment(), "KurirProduct")
            } catch (e: Exception) {
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


        val dataProductLaundry = ProductLaundry(
            id_laundry_item = productLaundryId,
            id_branch_laundry_item = userIdBranch,
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
                openUserFragment(KurirProductFragment(), "KurirProduct")
            } catch (e: Exception) {
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