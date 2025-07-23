package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import dev.ferynnd.baguslaundry.R
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentCreateItemRentalBinding
import dev.ferynnd.baguslaundry.model.IsActiveRental
import dev.ferynnd.baguslaundry.model.ProductRental
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class CreateItemRentalFragment : Fragment() {
    private var _binding: KurirFragmentCreateItemRentalBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private lateinit var userViewModel: UserViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel

    private var userId: Int = 0
    private var userIdBranch: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        rentalProductViewModel = ViewModelProvider(this).get(RentalProductViewModel::class.java)
        rentalProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentCreateItemRentalBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        viewLifecycleOwner.lifecycleScope.launch {
            loadUserData()
        }

        binding.btnSubmit.setOnClickListener {
                createProductRental()
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

    private fun createProductRental() {
        val name = binding.inputNamaItem.text.toString()
        val price = binding.inputHargaItem.text.toString()

        if (name.isBlank() || price.isBlank()) {
            Toast.makeText(requireContext(), "Lengkapi semua inputan", Toast.LENGTH_SHORT).show()
            return
        }

        val dataProductRental = ProductRental(
            id_branch_rental_item = userIdBranch,
            price_rental_item = price.toIntOrNull(),
            name_rental_item = name,
            is_active_rental_item = IsActiveRental.active,
        )

        Log.d("CreateEditProductRentalFragment", "Data Product Rental: $dataProductRental")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                rentalProductViewModel.createProductRental(dataProductRental)
                Toast.makeText(requireContext(), "Berhasil membuat item Rental", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.host_fragment_user, KurirProductFragment())
                    .commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal membuat cabang: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}