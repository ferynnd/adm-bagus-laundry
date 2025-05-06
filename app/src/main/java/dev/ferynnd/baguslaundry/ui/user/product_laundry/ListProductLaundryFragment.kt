package dev.ferynnd.baguslaundry.ui.user.product_laundry

import android.os.Bundle
import android.util.Log
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
import dev.ferynnd.baguslaundry.controller.user.LaundryProductAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListProductLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView

class ListProductLaundryFragment : Fragment() {
    private var _binding: KurirFragmentListProductLaundryBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var laundryProductAdapter: LaundryProductAdapter
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullLaundryList: List<ProductLaundry> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countProductLaundry: Int = 0

    private var selectedName: String? = null
    private val selectedNames = mutableSetOf<String>()
    private val selectedNameCards = mutableListOf<Pair<MaterialCardView, TextView>>()

    private var excludedKeywordsForOtherFilter = listOf(
        "cs", "cuci setrika",
        "ck", "cuci kering",
        "s", "setrika",
        "selimut",
        "bed cover", "bc",
        "karpet",
        "korden", "gorden"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        laundryProductViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java]
        laundryProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentListProductLaundryBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        laundryProductAdapter = LaundryProductAdapter()

        binding.recyclerViewProductLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryProductAdapter
        }

        // Dapatkan user dan baru lanjut observe
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = userViewModel.getUserById(userId)
                    userIdBranch = user.data.id_branch_user!!.toInt()


                    laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { productLaundry ->
                        val filteredList = productLaundry.filter { item ->
                            item.id_branch_laundry_item == userIdBranch
                        }

                        fullLaundryList = filteredList
//
                        countProductLaundry = filteredList.size
                        binding.countData.text = countProductLaundry.toString()

                        applyFilters()
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

        binding.typeCs.setOnClickListener {
            toggleFilterCard(
                binding.typeCs,
                binding.titleTypeCs,
                listOf("CS", "Cuci Setrika"),
                selectedNames,
                selectedNameCards
            )
        }
        binding.typeCk.setOnClickListener {
            toggleFilterCard(
                binding.typeCk,
                binding.titleTypeCk,
                listOf("CK", "Cuci Kering"),
                selectedNames,
                selectedNameCards
            )
        }
        binding.typeS.setOnClickListener {
            toggleFilterCard(
                binding.typeS,
                binding.titleTypeS,
                listOf("S", "Setrika"),
                selectedNames,
                selectedNameCards
            )
        }
        binding.typeSelimut.setOnClickListener {
            toggleFilterCard(
                binding.typeSelimut,
                binding.titleTypeSelimut,
                listOf("Selimut"),
                selectedNames,
                selectedNameCards
            )
        }
        binding.typeBc.setOnClickListener {
            toggleFilterCard(
                binding.typeBc,
                binding.titleTypeBc,
                listOf("Bed Cover", "BC"),
                selectedNames,
                selectedNameCards
            )
        }
        binding.typeKarpet.setOnClickListener {
            toggleFilterCard(
                binding.typeKarpet,
                binding.titleTypeKarpet,
                listOf("Karpet"),
                selectedNames,
                selectedNameCards
            )
        }
        binding.typeKorden.setOnClickListener {
            toggleFilterCard(
                binding.typeKorden,
                binding.titleTypeKorden,
                listOf("Korden", "Gorden"),
                selectedNames,
                selectedNameCards
            )
        }
        binding.typeOther.setOnClickListener {
            val allMainKeywords = listOf(
                "CS", "Cuci Setrika",
                "CK", "Cuci Kering",
                "S", "Setrika",
                "Selimut",
                "Bed Cover", "BC",
                "Karpet",
                "Korden", "Gorden"
            )
            toggleFilterCard(
                binding.typeOther,
                binding.titleTypeOther,
                listOf("other"),
                selectedNames,
                selectedNameCards
            )
            excludedKeywordsForOtherFilter = allMainKeywords.map { it.lowercase() }
        }

        binding.terapkanFilter.setOnClickListener {
            val filtered = fullLaundryList.filter { item ->
                val name = item.name_laundry_item?.lowercase().orEmpty()
                selectedNames.isEmpty() || selectedNames.any { keyword -> name.contains(keyword) }
            }

            laundryProductAdapter.submitList(filtered)
            binding.countData.text = filtered.size.toString()
            binding.wadahFilter.visibility = View.GONE
        }

        binding.resetFilter.setOnClickListener {
            selectedNames.clear()
            selectedNameCards.forEach { (card, text) ->
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                text.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            }
            selectedNameCards.clear()
            laundryProductAdapter.submitList(fullLaundryList)
            binding.countData.text = fullLaundryList.size.toString()
            binding.wadahFilter.visibility = View.GONE
        }

        // Fungsi pencarian
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
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

        binding.imageFilter.setOnClickListener {
            if (binding.wadahFilter.isVisible) {
                binding.wadahFilter.visibility = View.GONE
            } else {
                binding.wadahFilter.visibility = View.VISIBLE
            }
        }

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

    private fun applyFilters() {
        val filteredList = fullLaundryList.filter { item ->
            val name = item.name_laundry_item?.lowercase().orEmpty()

            if (selectedNames.contains("other")) {
                excludedKeywordsForOtherFilter.none { keyword -> name.contains(keyword) }
            } else {
                selectedNames.isEmpty() || selectedNames.any { keyword -> name.contains(keyword) }
            }
        }

        laundryProductAdapter.submitList(filteredList)
        binding.countData.text = filteredList.size.toString()
    }

    private fun toggleFilterCard(
        card: MaterialCardView,
        textView: TextView,
        values: List<String>,
        targetSet: MutableSet<String>,
        trackingList: MutableList<Pair<MaterialCardView, TextView>>
    ) {
        val lowerValues = values.map { it.lowercase() }

        val alreadySelected = lowerValues.all { targetSet.contains(it) }

        if (alreadySelected) {
            targetSet.removeAll(lowerValues)
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            trackingList.removeIf { it.first == card }
        } else {
            targetSet.addAll(lowerValues)
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            trackingList.add(card to textView)
        }
    }
}