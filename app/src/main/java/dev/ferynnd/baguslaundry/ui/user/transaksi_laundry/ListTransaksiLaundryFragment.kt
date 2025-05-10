package dev.ferynnd.baguslaundry.ui.user.transaksi_laundry

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
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.LaundryTransaksiAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.StatusTransactionRental
import dev.ferynnd.baguslaundry.model.TypeTransactionRental
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch

class ListTransaksiLaundryFragment : Fragment() {
    private var _binding: KurirFragmentListTransaksiLaundryBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var transaksiLaundryViewModel: LaundryReportViewModel
    private lateinit var transaksiLaundryAdapter: LaundryTransaksiAdapter

    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullTransaksiLaundryList: List<ReportLaundry> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countListTransaksiLaundry: Int = 0

    // Maps to track filter selections
    private val selectedFilters = mutableMapOf<String, MutableSet<String>>().apply {
        this["status"] = mutableSetOf()
    }

    // Map to track filter buttons (for styling)
    private val filterButtons =
        mutableMapOf<String, MutableList<Pair<MaterialCardView, TextView>>>()

    // BottomSheet for filters
    private lateinit var filterBottomSheetDialog: BottomSheetDialog
    private lateinit var filterBottomSheetView: View

    data class FilterOption(val value: String, val displayName: String)

    // Filter data definitions
    private val filterOptions = mapOf(
        "status" to listOf(
            FilterOption("pending", "Menunggu"),
            FilterOption("in_progress", "Sedang Diproses"),
            FilterOption("completed", "Selesai"),
            FilterOption("cancelled", "Dibatalkan")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        transaksiLaundryViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java]
        transaksiLaundryViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentListTransaksiLaundryBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        transaksiLaundryAdapter = LaundryTransaksiAdapter { transaksiLaundry: ReportLaundry ->
            onDetailClick(transaksiLaundry)
        }

        binding.recyclerViewTransaksiLaundry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transaksiLaundryAdapter
        }

        // Initialize filter category containers
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
                    transaksiLaundryViewModel.laundryReports.observe(viewLifecycleOwner) { productLaundry ->
                        productLaundry?.let {
                            val filteredList = productLaundry.filter { item ->
                                item.id_branch_transaction_laundry == userIdBranch
                            }

                            // Simpan list untuk pencarian
                            fullTransaksiLaundryList = filteredList

                            countListTransaksiLaundry = filteredList.size
                            binding.countData.text = countListTransaksiLaundry.toString()

                            if (fullTransaksiLaundryList.isNotEmpty()) {
                                binding.recyclerViewTransaksiLaundry.visibility = View.VISIBLE
                                binding.containerDataNotFound.visibility = View.GONE

                                transaksiLaundryAdapter.submitList(filteredList)
                            } else {
                                binding.recyclerViewTransaksiLaundry.visibility = View.GONE
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
                return false // kita proses real-time, jadi tidak perlu submit
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty().lowercase()
                val filtered = fullTransaksiLaundryList.filter {
                    it.name_client_transaction_laundry!!.lowercase().contains(query)
                }

                if (fullTransaksiLaundryList.isNotEmpty()) {
                    binding.recyclerViewTransaksiLaundry.visibility = View.VISIBLE
                    binding.containerDataNotFound.visibility = View.GONE

                    transaksiLaundryAdapter.submitList(filtered)
                } else {
                    binding.recyclerViewTransaksiLaundry.visibility = View.GONE
                    binding.containerDataNotFound.visibility = View.VISIBLE
                }

                binding.searchView.setIconifiedByDefault(false)
                binding.countData.text = filtered.size.toString()
                return true
            }
        })

        binding.imageFilter.setOnClickListener {
            showFilterBottomSheet()
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, UserDashboardFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onDetailClick(transaksiLaundry: ReportLaundry) {
//        Toast.makeText(context, "Detail ${transaksiLaundry.id_transaction_laundry} akan ditampilkan", Toast.LENGTH_SHORT).show()
        val bundle = Bundle().apply {
            putInt("TRANSAKSI_LAUNDRY_ID", transaksiLaundry.id_transaction_laundry ?: 0)
        }
        val detailFragment = DetailListTransaksiLaundryFragment()
        detailFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_user, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun initializeFilterBottomSheet() {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        filterBottomSheetView =
            inflater.inflate(R.layout.kurir_layout_filter_transaksi_laundry, null)

        filterBottomSheetDialog =
            BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        filterBottomSheetDialog.setContentView(filterBottomSheetView)

        val statusContainer =
            filterBottomSheetView.findViewById<FlexboxLayout>(R.id.wadah_button_filter_status_transaksi_laundry)

        createFilterButtons("status", statusContainer)

        val terapkanFilter =
            filterBottomSheetView.findViewById<MaterialButton>(R.id.terapkan_filter)
        val resetFilter = filterBottomSheetView.findViewById<MaterialButton>(R.id.reset_filter)

        terapkanFilter.setOnClickListener {
            applyFilters()
            filterBottomSheetDialog.dismiss()
        }

        resetFilter.setOnClickListener {
            resetAllFilters()
            filterBottomSheetDialog.dismiss()
        }

        val behavior = filterBottomSheetDialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = true
        behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
    }

    private fun createFilterButtons(filterType: String, container: FlexboxLayout) {
        filterOptions[filterType]?.forEach { option ->
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

            card.addView(textView)

            card.setOnClickListener {
                toggleFilterOption(filterType, option.value, card, textView)
            }

            container.addView(card)
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun toggleFilterOption(
        filterType: String,
        value: String,
        card: MaterialCardView,
        textView: TextView
    ) {
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

        if (fullTransaksiLaundryList.isNotEmpty()) {
            binding.recyclerViewTransaksiLaundry.visibility = View.VISIBLE
            binding.containerDataNotFound.visibility = View.GONE

            transaksiLaundryAdapter.submitList(fullTransaksiLaundryList)
        } else {
            binding.recyclerViewTransaksiLaundry.visibility = View.GONE
            binding.containerDataNotFound.visibility = View.VISIBLE
        }

        binding.countData.text = fullTransaksiLaundryList.size.toString()
    }

    // Show bottom sheet filter
    private fun showFilterBottomSheet() {
        filterBottomSheetDialog.show()
    }

    // Apply current filters to the list
    private fun applyFilters() {
        val statusFilters = selectedFilters["status"] ?: emptySet()

        val filteredList = fullTransaksiLaundryList.filter { item ->

            val statusMatches = if (statusFilters.isEmpty()) {
                true
            } else {
                val statusName = when (item.status_transaction_laundry) {
                    StatusReportLaundry.pending -> "pending"
                    StatusReportLaundry.in_progress -> "in_progress"
                    StatusReportLaundry.completed -> "completed"
                    StatusReportLaundry.cancelled -> "cancelled"
                }
                statusFilters.contains(statusName)
            }

            statusMatches
        }

        if (filteredList.isNotEmpty()) {
            binding.recyclerViewTransaksiLaundry.visibility = View.VISIBLE
            binding.containerDataNotFound.visibility = View.GONE

            transaksiLaundryAdapter.submitList(filteredList)
        } else {
            binding.recyclerViewTransaksiLaundry.visibility = View.GONE
            binding.containerDataNotFound.visibility = View.VISIBLE
        }

        binding.countData.text = filteredList.size.toString()
    }
}