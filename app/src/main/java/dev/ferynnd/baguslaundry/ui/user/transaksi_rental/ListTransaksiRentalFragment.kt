package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

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
import dev.ferynnd.baguslaundry.controller.user.RentalTransaksiAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.model.StatusTransactionRental
import dev.ferynnd.baguslaundry.model.TypeTransactionRental
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch

class ListTransaksiRentalFragment : Fragment() {
    private var _binding: KurirFragmentListTransaksiRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var transaksiRentalViewModel: RentalReportViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var transaksiRentalAdapter: RentalTransaksiAdapter

    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullTransaksiRentalList: List<ReportRental> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countListTransaksiRental: Int = 0

    // Maps to track filter selections
    private val selectedFilters = mutableMapOf<String, MutableSet<String>>().apply {
        this["type"] = mutableSetOf()
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
        "type" to listOf(
            FilterOption("bath towel", "Bath Towel"),
            FilterOption("hand towel", "Hand Towel"),
            FilterOption("gorden", "Gorden"),
            FilterOption("keset", "Keset")
        ),
        "status" to listOf(
            FilterOption("waiting for approval", "Menunggu Persetujuan"),
            FilterOption("approved", "Disetujui"),
            FilterOption("out", "keluar"),
            FilterOption("in", "Masuk"),
            FilterOption("cancelled", "Dibatalkan")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())
        transaksiRentalViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        transaksiRentalViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentListTransaksiRentalBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        transaksiRentalAdapter = RentalTransaksiAdapter { transaksiRental: ReportRental ->
            onDetailClick(transaksiRental)
        }

        binding.recyclerViewTransaksiRental.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transaksiRentalAdapter
        }

        // Initialize filter category containers
        filterButtons["type"] = mutableListOf()
        filterButtons["status"] = mutableListOf()

        // Initialize bottom sheet filter
        initializeFilterBottomSheet()

        // Dapatkan user dan baru lanjut observe
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = userViewModel.getUserById(userId)
                    userIdBranch = user.data.id_branch_user!!.toInt()

                    userViewModel.users.observe(viewLifecycleOwner) { kurirList ->
                        transaksiRentalAdapter.setKurir(kurirList)
                    }

                    clientViewModel.clients.observe(viewLifecycleOwner) { ClientList ->
                        transaksiRentalAdapter.setClient(ClientList)
                    }

                    // Setelah userIdBranch tersedia, baru observe
                    transaksiRentalViewModel.rentalReports.observe(viewLifecycleOwner) { productLaundry ->
                        productLaundry?.let {
                            val filteredList = productLaundry.filter { item ->
                                item.id_branch_transaction_rental == userIdBranch
                            }

                            // Simpan list untuk pencarian
                            fullTransaksiRentalList = filteredList

                            countListTransaksiRental = filteredList.size
                            binding.countData.text = countListTransaksiRental.toString()

                            if (fullTransaksiRentalList.isNotEmpty()) {
                                binding.recyclerViewTransaksiRental.visibility = View.VISIBLE
                                binding.containerDataNotFound.visibility = View.GONE

                                transaksiRentalAdapter.submitList(filteredList)
                            } else {
                                binding.recyclerViewTransaksiRental.visibility = View.GONE
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
                val filtered = fullTransaksiRentalList.filter {
                    it.recipient_name_transaction_rental!!.lowercase().contains(query)
                }
                if (fullTransaksiRentalList.isNotEmpty()) {
                    binding.recyclerViewTransaksiRental.visibility = View.VISIBLE
                    binding.containerDataNotFound.visibility = View.GONE

                    transaksiRentalAdapter.submitList(filtered)
                } else {
                    binding.recyclerViewTransaksiRental.visibility = View.GONE
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

    // Menetapkan binding ke null saat tampilan dihancurkan untuk menghindari memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onDetailClick(transaksiRental: ReportRental) {
//        Toast.makeText(context, "Detail ${transaksiRental.id_transaction_rental} akan ditampilkan", Toast.LENGTH_SHORT).show()
        val bundle = Bundle().apply {
            putInt(
                "TRANSAKSI_RENTAL_ID",
                transaksiRental.id_transaction_rental
            )  // Mengirimkan ID supplier ke fragment berikutnya
        }
        val detailFragment = DetailListTransaksiRentalFragment()
        detailFragment.arguments = bundle  // Menetapkan argumen untuk fragment detail

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.host_fragment_user,
                detailFragment
            )  // Mengganti fragment saat ini dengan DetailSupplierFragment
            .addToBackStack(null)  // Menambahkan transaksi ke back stack agar pengguna bisa kembali
            .commit()  // Menyelesaikan transaksi
    }

    private fun initializeFilterBottomSheet() {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        filterBottomSheetView =
            inflater.inflate(R.layout.kurir_layout_filter_transaksi_rental, null)

        filterBottomSheetDialog =
            BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        filterBottomSheetDialog.setContentView(filterBottomSheetView)

        val typeContainer =
            filterBottomSheetView.findViewById<FlexboxLayout>(R.id.wadah_button_filter_type_transaksi_rental)
        val statusContainer =
            filterBottomSheetView.findViewById<FlexboxLayout>(R.id.wadah_button_filter_status_transaksi_rental)

        createFilterButtons("type", typeContainer)
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

        if (fullTransaksiRentalList.isNotEmpty()) {
            binding.recyclerViewTransaksiRental.visibility = View.VISIBLE
            binding.containerDataNotFound.visibility = View.GONE

            transaksiRentalAdapter.submitList(fullTransaksiRentalList)
        } else {
            binding.recyclerViewTransaksiRental.visibility = View.GONE
            binding.containerDataNotFound.visibility = View.VISIBLE
        }

        binding.countData.text = fullTransaksiRentalList.size.toString()
    }

    // Show bottom sheet filter
    private fun showFilterBottomSheet() {
        filterBottomSheetDialog.show()
    }

    // Apply current filters to the list
    private fun applyFilters() {
        val typeFilters = selectedFilters["type"] ?: emptySet()
        val statusFilters = selectedFilters["status"] ?: emptySet()

        val filteredList = fullTransaksiRentalList.filter { item ->

            val typeMatches = if (typeFilters.isEmpty()) {
                true
            } else {
                val typeName = when (item.type_rental_transaction) {
                    TypeTransactionRental.BATH_TOWEL -> "bath towel"
                    TypeTransactionRental.HAND_TOWEL -> "hand towel"
                    TypeTransactionRental.GORDEN -> "gorden"
                    TypeTransactionRental.KESET -> "keset"
                }
                typeFilters.contains(typeName)
            }

            val statusMatches = if (statusFilters.isEmpty()) {
                true
            } else {
                val statusName = when (item.status_transaction_rental) {
                    StatusTransactionRental.WAITING_FOR_APPROVAL -> "waiting for approval"
                    StatusTransactionRental.APPROVED -> "approved"
                    StatusTransactionRental.OUT -> "out"
                    StatusTransactionRental.IN -> "in"
                    StatusTransactionRental.CANCELLED -> "cancelled"
                }
                statusFilters.contains(statusName)
            }

            typeMatches && statusMatches
        }

        if (filteredList.isNotEmpty()) {
            binding.recyclerViewTransaksiRental.visibility = View.VISIBLE
            binding.containerDataNotFound.visibility = View.GONE

            transaksiRentalAdapter.submitList(filteredList)
        } else {
            binding.recyclerViewTransaksiRental.visibility = View.GONE
            binding.containerDataNotFound.visibility = View.VISIBLE
        }

        binding.countData.text = filteredList.size.toString()
    }
}