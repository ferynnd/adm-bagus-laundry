package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentCreateItemLaundryBinding
import dev.ferynnd.baguslaundry.model.IsActiveLaundryItem
import dev.ferynnd.baguslaundry.model.ProductLaundry
import kotlinx.coroutines.launch
import dev.ferynnd.baguslaundry.R

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
                binding.textHeaderBold.text = "Update Item Laundry"
                binding.btnSubmit.text = "Update"
            }
        } else {
            binding.textHeaderBold.text = "Create Item Laundry"
            binding.btnSubmit.text = "Create"
        }

        binding.btnSubmit.setOnClickListener {
            if (productLaundryId != null) {
                updateProductLaundry()
            } else {
                createProductLaundry()
            }
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, KurirProductFragment())
                .commit()
        }

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
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat data pengguna",
                    Toast.LENGTH_LONG
                ).show()
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
            Toast.makeText(requireContext(), "Lengkapi semua inputan", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Cabang berhasil dibuat", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.host_fragment_user, KurirProductFragment())
                    .commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal membuat cabang: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateProductLaundry() {

        val name = binding.inputNamaItem.text.toString()
        val price = binding.inputHargaItem.text.toString()
        val time = binding.inputTimeItem.text.toString()

        if (name.isBlank() || price.isBlank() || time.isBlank()) {
            Toast.makeText(requireContext(), "Lengkapi semua inputan", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Cabang berhasil diupdate", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.host_fragment_user, KurirProductFragment())
                    .commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal mengupdate cabang: ${e.message}", Toast.LENGTH_LONG).show()

            }
        }
    }
}