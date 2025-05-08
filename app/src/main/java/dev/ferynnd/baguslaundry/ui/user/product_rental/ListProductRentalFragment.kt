package dev.ferynnd.baguslaundry.ui.user.product_rental

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
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
    private var countProductRental: Int = 0

    // Maps to track filter selections
    private val selectedFilters = mutableMapOf<String, MutableSet<String>>().apply {
        this["condition"] = mutableSetOf()
        this["status"] = mutableSetOf()
    }

    // Map to track filter buttons (for styling)
    private val filterButtons = mutableMapOf<String, MutableList<Pair<MaterialCardView, TextView>>>()

    // Filter data definitions
    private val filterOptions = mapOf(
        "condition" to listOf(
            FilterOption("clean", "Bersih"),
            FilterOption("dirty", "Kotor"),
            FilterOption("damaged", "Rusak")
        ),
        "status" to listOf(
            FilterOption("available", "Tersedia"),
            FilterOption("rented", "Disewa"),
            FilterOption("maintenance", "Pemeliharaan")
        )
    )

    data class FilterOption(val value: String, val displayName: String)

    // BottomSheet for filters
    private lateinit var filterBottomSheetDialog: BottomSheetDialog
    private lateinit var filterBottomSheetView: View

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

        binding.recyclerViewProductRental.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rentalProductAdapter
        }

        // Initialize filter category containers
        filterButtons["condition"] = mutableListOf()
        filterButtons["status"] = mutableListOf()

        // Initialize bottom sheet filter
        initializeFilterBottomSheet()

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

                            countProductRental = filteredList.size
                            binding.countData.text = countProductRental.toString()

                            if (filteredList.isNotEmpty()) {
                                binding.recyclerViewProductRental.visibility = View.VISIBLE
                                binding.containerDataNotFound.visibility = View.GONE

                                applyFilters()
                            }else {
                                binding.recyclerViewProductRental.visibility = View.GONE
                                binding.containerDataNotFound.visibility = View.VISIBLE
                            }
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

                if (filtered.isNotEmpty()) {
                    binding.recyclerViewProductRental.visibility = View.VISIBLE
                    binding.containerDataNotFound.visibility = View.GONE

                    rentalProductAdapter.submitList(filtered)
                }else {
                    binding.recyclerViewProductRental.visibility = View.GONE
                    binding.containerDataNotFound.visibility = View.VISIBLE
                }

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
            showFilterBottomSheet()
        }

        return binding.root
    }

    // Menetapkan binding ke null saat tampilan dihancurkan untuk menghindari memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Initialize bottom sheet filter
    private fun initializeFilterBottomSheet() {
        // Inflate layout for bottom sheet
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        filterBottomSheetView = inflater.inflate(R.layout.kurir_layout_filter_product_rental, null)

        // Create BottomSheetDialog
        filterBottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        filterBottomSheetDialog.setContentView(filterBottomSheetView)

        // Get filter container views
        val conditionContainer = filterBottomSheetView.findViewById<FlexboxLayout>(R.id.wadah_button_filter_kondisi)
        val statusContainer = filterBottomSheetView.findViewById<FlexboxLayout>(R.id.wadah_button_filter_status)

        // Create filter buttons dynamically
        createFilterButtons("condition", conditionContainer)
        createFilterButtons("status", statusContainer)

        // Setup apply and reset buttons
        val terapkanFilter = filterBottomSheetView.findViewById<MaterialButton>(R.id.terapkan_filter)
        val resetFilter = filterBottomSheetView.findViewById<MaterialButton>(R.id.reset_filter)

        terapkanFilter.setOnClickListener {
            applyFilters()
            filterBottomSheetDialog.dismiss()
        }

        resetFilter.setOnClickListener {
            resetAllFilters()
            filterBottomSheetDialog.dismiss()
        }

        // Optional: Add behavior to make dialog more responsive
        val behavior = filterBottomSheetDialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = true
        behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
    }

    // Create filter buttons dynamically
    private fun createFilterButtons(filterType: String, container: FlexboxLayout) {
        filterOptions[filterType]?.forEach { option ->
            // Create card and text view
            val card = MaterialCardView(requireContext()).apply {
                id = View.generateViewId()
                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(6, 6, 6, 6)
                }
                cardElevation = 0f
                radius = resources.getDimension(R.dimen.card_corner_radius)
                strokeWidth = 2
                strokeColor = ContextCompat.getColor(requireContext(), R.color.blue500)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            val textView = TextView(requireContext()).apply {
                id = View.generateViewId()
                text = option.displayName
                setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
                textSize = 14f
                setPadding(
                    18.dpToPx(requireContext()),
                    8.dpToPx(requireContext()),
                    18.dpToPx(requireContext()),
                    8.dpToPx(requireContext())
                )
                setTypeface(ResourcesCompat.getFont(requireContext(), R.font.inter_semibold))
            }

            // Add text view to card
            card.addView(textView)

            // Set click listener
            card.setOnClickListener {
                toggleFilterOption(filterType, option.value, card, textView)
            }

            // Add card to container
            container.addView(card)
        }
    }

    // Helper function to convert dp to pixels
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    // Toggle filter option selection
    private fun toggleFilterOption(filterType: String, value: String, card: MaterialCardView, textView: TextView) {
        val currentSelections = selectedFilters[filterType] ?: return
        val buttonPair = Pair(card, textView)

        if (currentSelections.contains(value)) {
            // Unselect
            currentSelections.remove(value)
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            filterButtons[filterType]?.remove(buttonPair)
        } else {
            // Select
            currentSelections.add(value)
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            filterButtons[filterType]?.add(buttonPair)
        }
    }

    // Reset all filters
    private fun resetAllFilters() {
        // Clear all selections
        selectedFilters.forEach { (_, selections) -> selections.clear() }

        // Reset UI for all buttons
        filterButtons.forEach { (_, buttons) ->
            buttons.forEach { (card, textView) ->
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
            }
            buttons.clear()
        }

        if (fullRentalList.isNotEmpty()) {
            binding.recyclerViewProductRental.visibility = View.VISIBLE
            binding.containerDataNotFound.visibility = View.GONE

            rentalProductAdapter.submitList(fullRentalList)
        }else {
            binding.recyclerViewProductRental.visibility = View.GONE
            binding.containerDataNotFound.visibility = View.VISIBLE
        }

        binding.countData.text = fullRentalList.size.toString()
    }

    // Show bottom sheet filter
    private fun showFilterBottomSheet() {
        filterBottomSheetDialog.show()
    }

    // Apply current filters to the list
    private fun applyFilters() {
        val conditionFilters = selectedFilters["condition"] ?: emptySet()
        val statusFilters = selectedFilters["status"] ?: emptySet()

        val filteredList = fullRentalList.filter { item ->
            (conditionFilters.isEmpty() || conditionFilters.contains(item.condition_rental_item.name.lowercase())) &&
                    (statusFilters.isEmpty() || statusFilters.contains(item.status_rental_item.name.lowercase()))
        }

        if (filteredList.isNotEmpty()) {
            binding.recyclerViewProductRental.visibility = View.VISIBLE
            binding.containerDataNotFound.visibility = View.GONE

            rentalProductAdapter.submitList(filteredList)
        }else {
            binding.recyclerViewProductRental.visibility = View.GONE
            binding.containerDataNotFound.visibility = View.VISIBLE
        }

        binding.countData.text = filteredList.size.toString()
    }
}