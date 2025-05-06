package dev.ferynnd.baguslaundry.ui.user.product_rental

import android.os.Bundle
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
import dev.ferynnd.baguslaundry.controller.user.RentalProductAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListProductRentalBinding
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView

class ListProductRentalFragment : Fragment() {
    private var _binding: KurirFragmentListProductRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var rentalProductAdapter: RentalProductAdapter
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullRentalList: List<ProductRental> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countProductLaundry: Int = 0

    private var selectedStatus: String? = null
    private var selectedCondition: String? = null

    private val selectedConditions = mutableSetOf<String>()
    private val selectedStatuses = mutableSetOf<String>()

    // Untuk menyimpan referensi kartu yang aktif, supaya bisa reset style
    private val selectedConditionCards = mutableListOf<Pair<MaterialCardView, TextView>>()
    private val selectedStatusCards = mutableListOf<Pair<MaterialCardView, TextView>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
        rentalProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentListProductRentalBinding.inflate(inflater, container, false)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        rentalProductAdapter = RentalProductAdapter()

        binding.recyclerViewProductLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalProductAdapter
        }

        // Dapatkan user dan baru lanjut observe
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = userViewModel.getUserById(userId)
                    userIdBranch = user.data.id_branch_user!!.toInt()

                    // Setelah userIdBranch tersedia, baru observe
                    rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { productLaundry ->
                        productLaundry?.let {
                            val filteredList = productLaundry.filter { item ->
                                item.id_branch_rental_item == userIdBranch
                            }

                            // Simpan list untuk pencarian
                            fullRentalList = filteredList

                            countProductLaundry = filteredList.size
                            binding.countData.text = countProductLaundry.toString()

                            applyFilters()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(context, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        binding.kondisiBersih.setOnClickListener {
            toggleFilterCard(binding.kondisiBersih, binding.titleKondisiBersih, "clean", selectedConditions, selectedConditionCards)
        }
        binding.kondisiKotor.setOnClickListener {
            toggleFilterCard(binding.kondisiKotor, binding.titleKondisiKotor, "dirty", selectedConditions, selectedConditionCards)
        }
        binding.kondisiRusak.setOnClickListener {
            toggleFilterCard(binding.kondisiRusak, binding.titleKondisiRusak, "damaged", selectedConditions, selectedConditionCards)
        }

        binding.statusTersedia.setOnClickListener {
            toggleFilterCard(binding.statusTersedia, binding.titleStatusTersedia, "available", selectedStatuses, selectedStatusCards)
        }
        binding.statusDisewa.setOnClickListener {
            toggleFilterCard(binding.statusDisewa, binding.titleStatusDisewa, "rented", selectedStatuses, selectedStatusCards)
        }
        binding.statusPemeliharaan.setOnClickListener {
            toggleFilterCard(binding.statusPemeliharaan, binding.titleStatusPemeliharaan, "maintenance", selectedStatuses, selectedStatusCards)
        }

        binding.terapkanFilter.setOnClickListener {
            val filtered = fullRentalList.filter { item ->
                (selectedConditions.isEmpty() || selectedConditions.contains(item.condition_rental_item.name.lowercase())) &&
                        (selectedStatuses.isEmpty() || selectedStatuses.contains(item.status_rental_item.name.lowercase()))
            }
            rentalProductAdapter.submitList(filtered)
            binding.countData.text = filtered.size.toString()

            binding.wadahFilter.visibility = View.GONE
        }

        binding.resetFilter.setOnClickListener {
            selectedConditions.clear()
            selectedStatuses.clear()

            // Reset semua style kartu
            selectedConditionCards.forEach { (card, text) ->
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                text.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            }
            selectedStatusCards.forEach { (card, text) ->
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                text.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            }

            selectedConditionCards.clear()
            selectedStatusCards.clear()

            // Tampilkan ulang semua data
            rentalProductAdapter.submitList(fullRentalList)
            binding.countData.text = fullRentalList.size.toString()

            binding.wadahFilter.visibility = View.GONE
        }

        // Fungsi pencarian
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // proses real-time
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty().lowercase()
                val filtered = fullRentalList.filter {
                    it.name_rental_item!!.lowercase().contains(query) || it.number_rental_item!!.lowercase().contains(query)
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

        binding.imageFilter.setOnClickListener {
            if (binding.wadahFilter.isVisible) {
                binding.wadahFilter.visibility = View.GONE
            } else {
                binding.wadahFilter.visibility = View.VISIBLE
            }
        }

        return binding.root
    }

    // Menetapkan binding ke null saat tampilan dihancurkan untuk menghindari memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyFilters() {
        val filteredList = fullRentalList.filter { item ->
            val statusMatches =
                selectedStatus?.let { item.status_rental_item.name.equals(it, ignoreCase = true) }
                    ?: true
            val conditionMatches = selectedCondition?.let {
                item.condition_rental_item.name.equals(
                    it,
                    ignoreCase = true
                )
            } ?: true
            statusMatches && conditionMatches
        }

        rentalProductAdapter.submitList(filteredList)
        binding.countData.text = filteredList.size.toString()
    }

    private fun toggleFilterCard(
        card: MaterialCardView,
        textView: TextView,
        value: String,
        targetSet: MutableSet<String>,
        trackingList: MutableList<Pair<MaterialCardView, TextView>>
    ) {
        if (targetSet.contains(value)) {
            // Unselect
            targetSet.remove(value)
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            trackingList.removeIf { it.first == card }
        } else {
            // Select
            targetSet.add(value)
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            trackingList.add(card to textView)
        }
    }
}