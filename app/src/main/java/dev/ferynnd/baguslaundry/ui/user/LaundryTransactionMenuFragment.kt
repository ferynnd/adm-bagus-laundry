package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.LaundryTransactionMenuAdapter
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentLaundryTransactionMenuBinding
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.TransactionData
import kotlinx.coroutines.launch

class LaundryTransactionMenuFragment : Fragment() {


    private lateinit var binding: FragmentLaundryTransactionMenuBinding
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var laundryTransactionMenuAdapter: LaundryTransactionMenuAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var userId: Int = 0
    private var userIdBranch: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        laundryProductViewModel =
            ViewModelProvider(requireActivity())[LaundryProductViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        laundryReportViewModel =
            ViewModelProvider(requireActivity())[LaundryReportViewModel::class.java]
        laundryReportViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLaundryTransactionMenuBinding.inflate(inflater, container, false)
        hideBottomNavigationView()

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = userViewModel.getUserById(userId)
                userIdBranch = user.data.id_branch_user!!.toInt()
            } catch (e: Exception) {
                throw e
            }
        }

        laundryTransactionMenuAdapter = LaundryTransactionMenuAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryTransactionMenuAdapter
        }

        laundryProductViewModel.selectedItems.observe(viewLifecycleOwner) { selected ->
            laundryTransactionMenuAdapter.submitList(selected)
        }

        laundryReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) { // Pastikan response tidak null
                if (response.success) {
                    Toast.makeText(requireContext(), "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                    // Navigasi setelah sukses
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, UserDashboardFragment())
                        .commit()
                } else {
                    // Tampilkan pesan error dari backend
                    Log.d("CreateTransaction", "Error: ${response.message}")
                    Toast.makeText(requireContext(), response.message ?: "Gagal membuat transaksi.", Toast.LENGTH_SHORT).show()
                }
                laundryReportViewModel.clearCreateTransactionResponse()
            }
        }

        laundryReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                Log.d("CreateTransaction", "Error: $errorMessage")
                 laundryReportViewModel.clearError()
            }
        }

        binding.btnSubmit.setOnClickListener {
            createLaundryTransaction()
        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        laundryProductViewModel.clearSelectedItems()
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
    }


    private fun hideBottomNavigationView() {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.GONE
    }

    private fun createLaundryTransaction() {

        val selectedItems = laundryProductViewModel.selectedItems.value ?: emptyList()

        val laundryTransactionItems = selectedItems.map { selectedItem ->
            ProductLaundry(
                id_laundry_item = selectedItem.id_laundry_item,
                name_laundry_item = selectedItem.name_laundry_item,
                price_laundry_item = selectedItem.price_laundry_item,
                weight = selectedItem.weight
            )
        }


        val nameClient = binding.textClient.text.toString().trim()
        val priceCashStr = binding.textCash.text.toString().trim()

        if (nameClient.isEmpty() || priceCashStr.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Nama pelanggan dan uang tunai harus diisi",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val priceCash = priceCashStr.toDoubleOrNull()
        if (priceCash == null) {
            Toast.makeText(requireContext(), "Format uang tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val laundryTransactionRequest = TransactionData(
            id_kurir_transaction_laundry = userId,
            id_branch_transaction_laundry = userIdBranch,
            name_client_transaction_laundry = nameClient,
            status_transaction_laundry = "unpaid",
            promo_transaction_laundry = 0.0,
            additional_cost_transaction_laundry = 0.0,
            cash_transaction_laundry = priceCash,
            notes_transaction_laundry = "hsjns",
            list_transaction_laundry = laundryTransactionItems
        )

        Log.d("CreateTransaction", laundryTransactionRequest.toString())
        laundryReportViewModel.createReportLaundry(laundryTransactionRequest)

    }

}

