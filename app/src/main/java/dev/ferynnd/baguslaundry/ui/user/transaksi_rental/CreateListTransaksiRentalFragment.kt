package dev.ferynnd.baguslaundry.ui.user.transaksi_rental
//
import android.content.Context
import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
//import android.widget.AdapterView
//import android.widget.ArrayAdapter
//import android.widget.AutoCompleteTextView
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.Spinner
//import android.widget.TextView
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import androidx.core.content.res.ResourcesCompat
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import com.google.android.flexbox.FlexboxLayout
//import com.google.android.material.bottomsheet.BottomSheetBehavior
//import com.google.android.material.bottomsheet.BottomSheetDialog
//import com.google.android.material.button.MaterialButton
//import com.google.android.material.card.MaterialCardView
//import dev.ferynnd.baguslaundry.R
//import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
//import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
//import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
//import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
//import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
//import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentCreateListTransaksiRentalBinding
//import dev.ferynnd.baguslaundry.model.Client
//import dev.ferynnd.baguslaundry.model.ConditionRental
//import dev.ferynnd.baguslaundry.model.ProductRental
//import dev.ferynnd.baguslaundry.model.StatusRental
//import dev.ferynnd.baguslaundry.model.RentalTransactionItem
//import dev.ferynnd.baguslaundry.model.RentalTransactionRequest
//import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
//import dev.ferynnd.baguslaundry.ui.user.transaksi_laundry.ListTransaksiLaundryFragment
//import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.ListTransaksiRentalFragment.FilterOption
//import kotlinx.coroutines.launch
//import java.text.NumberFormat
//import java.util.Locale
//
class CreateListTransaksiRentalFragment : Fragment() {
    private var _binding: KurirFragmentCreateListTransaksiRentalBinding? = null
    private val binding get() = _binding!!
////
////    private lateinit var userViewModel: UserViewModel
////    private lateinit var rentalProductViewModel: RentalProductViewModel
////    private lateinit var rentalReportViewModel: RentalReportViewModel
////    private lateinit var clientViewModel: ClientViewModel
////    private lateinit var sharePrefrences: SharePrefrenceHelper
////
////    private var userId: Int = 0
////    private var userIdBranch: Int = 0
////    private var clientId: Int = 0
////
////    private var fullRentalList: List<ProductRental> = listOf()
////    private var clientList: List<Client> = listOf()
////
////    private var selectedTransactionType: String = ""
////    private var selectedTransactionStatus: String = ""
////
////    private val selectedFilters = mutableMapOf<String, MutableSet<String>>().apply {
////        this["condition"] = mutableSetOf()
////        this["status"] = mutableSetOf()
////    }
////    private val filterButtons =
////        mutableMapOf<String, MutableList<Pair<MaterialCardView, TextView>>>()
////
////    private val filterOptions = mapOf(
////        "condition" to listOf(
////            FilterOption("clean", "Bersih"),
////            FilterOption("dirty", "Kotor"),
////            FilterOption("damaged", "Rusak")
////        ), "status" to listOf(
////            FilterOption("available", "Tersedia"),
////            FilterOption("rented", "Disewa"),
////            FilterOption("maintenance", "Pemeliharaan")
////        )
////    )
////
////    private val typeTransaksi = listOf(
////        FilterOption("bath towel", "Handuk Besar"),
////        FilterOption("hand towel", "Handuk Kecil"),
////        FilterOption("gorden", "Gorden"),
////        FilterOption("keset", "Keset")
////    )
////
////    private val statusOptions = listOf(
////        FilterOption("waiting for approval", "Menunggu Persetujuan"),
////        FilterOption("approved", "Disetujui"),
////        FilterOption("out", "Keluar"),
////        FilterOption("in", "Masuk"),
////        FilterOption("cancelled", "Dibatalkan")
////    )
////
////    private val conditionOptions = listOf(
////        FilterOption("clean", "Bersih"),
////        FilterOption("dirty", "Kotor"),
////        FilterOption("damaged", "Rusak")
////    )
////
////    data class FilterOption(val value: String, val displayName: String)
////
////    private var rentalTransactionItems: MutableList<RentalTransactionItem> = mutableListOf()
////
////    private lateinit var filterBottomSheetDialog: BottomSheetDialog
////    private lateinit var filterBottomSheetView: View
//
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
////        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
////
////        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
////        rentalProductViewModel.init(requireContext())
////
////        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
////        rentalReportViewModel.init(requireContext())
////
////        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
////        clientViewModel.init(requireContext())
    }
//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
       _binding = KurirFragmentCreateListTransaksiRentalBinding.inflate(layoutInflater)
////
////        sharePrefrences = SharePrefrenceHelper(requireContext())
////        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()
////
////        // Initialize empty dropdowns while data loads
////        setupViews()
////        setupObservers()
////        filterButtons["condition"] = mutableListOf()
////        filterButtons["status"] = mutableListOf()
////        initFilterBottomSheet()
////        loadUserDataAndProducts()
////
////        binding.arrowBack.setOnClickListener {
////            parentFragmentManager.beginTransaction()
////                .replace(R.id.host_fragment_user, UserDashboardFragment()).addToBackStack(null)
////                .commit()
////        }
////
////        binding.imageFilterItemRental.setOnClickListener {
////            showFilterBottomSheet()
////        }
////
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
////        setupTextWatchers()
////
////        if (binding.containerRental.childCount > 0) {
////            updateTotalAmountToPay()
////        }
//    }
//
//    private fun refreshAllRentalInputs() {
////        binding.containerRental.removeAllViews()
////
////        updateTotalPrice()
//    }
////
////    private fun loadUserDataAndProducts() {
////        if (userId != 0) {
////            viewLifecycleOwner.lifecycleScope.launch {
////                try {
////                    val userResponse = userViewModel.getUserById(userId)
////                    if (userResponse.success && userResponse.data != null) {
////                        userIdBranch = userResponse.data.id_branch_user!!.toInt()
////
////                        // Set kurir information
////                        binding.namaKurir.text = userResponse.data.fullname_user ?: "-"
////
////                        rentalProductViewModel.getProductRental()
////
////                        try {
////                            clientViewModel.getClient()
////
////                            if (clientViewModel.clients.value != null) {
////                                val filteredClients =
////                                    clientViewModel.clients.value!!.filter { client ->
////                                        client.id_branch_client?.toInt() == userIdBranch
////                                    }
////                                clientList = filteredClients
////
////                                val clientNames = clientList.map { it.name_client ?: "" }
////                                val adapterClient = ArrayAdapter(
////                                    requireContext(),
////                                    android.R.layout.simple_dropdown_item_1line,
////                                    clientNames
////                                )
////                                binding.inputNamaClient.setAdapter(adapterClient)
////
////                                binding.inputNamaClient.setOnItemClickListener { _, _, position, _ ->
////                                    val selectedClient = clientList.getOrNull(position)
////                                    clientId = selectedClient?.id_client ?: 0
////                                    Toast.makeText(
////                                        requireContext(),
////                                        "Client id yang terpilih: ${clientId}",
////                                        Toast.LENGTH_SHORT
////                                    ).show()
////                                    binding.alamatKlien.text = selectedClient?.address_client ?: "-"
////                                }
////                            } else {
////                                binding.alamatKlien.text = "-" // Set default jika data klien null
////                            }
////                        } catch (e: Exception) {
////                            Toast.makeText(
////                                requireContext(),
////                                "Gagal memuat data client: ${e.message}",
////                                Toast.LENGTH_SHORT
////                            ).show()
////                        }
////                    } else {
////                        Toast.makeText(
////                            requireContext(), "Gagal memuat data pengguna", Toast.LENGTH_LONG
////                        ).show()
////                    }
////                } catch (e: Exception) {
////                    e.printStackTrace()
////                    Toast.makeText(
////                        requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_LONG
////                    ).show()
////                }
////            }
////        } else {
////            Toast.makeText(context, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
////        }
////    }
////
////    private fun setupObservers() {
////        rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { products ->
////            if (products != null) {
////                fullRentalList = products.filter { product ->
////                    product.id_branch_rental_item?.toInt() == userIdBranch
////                }
////
////                val typeTransaksiAdapter = ArrayAdapter(
////                    requireContext(),
////                    android.R.layout.simple_dropdown_item_1line,
////                    typeTransaksi.map { it.displayName })
////                binding.inputTypeTransaksi.setAdapter(typeTransaksiAdapter)
////
////                binding.inputTypeTransaksi.setOnItemClickListener { _, _, position, _ ->
////                    val selectedOption = typeTransaksi[position]
////                    selectedTransactionType = selectedOption.value
////
////                    binding.containerRental.removeAllViews()
////
////                    if (fullRentalList.isNotEmpty()) {
////                        addRentalItemInput()
////                        updateTotalPrice()
////                    }
////                }
////
////                if (binding.containerRental.childCount > 0) {
////                    refreshAllRentalInputs()
////                }
////            }
////        }
//
//        // Observe clients for dropdown
//    saksi_rental_input,
//            binding.containerRental,
//            false
//        )
//        clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
//            if (clients != null) {
//                if (userIdBranch != 0) {
//                    clientList = clients.filter { client ->
//                        client.id_branch_client?.toInt() == userIdBranch
//                    }
//
//                    val clientNames = clientList.map { it.name_client ?: "" }
//
//                    val adapterClient = ArrayAdapter(
//                        requireContext(), android.R.layout.simple_dropdown_item_1line, clientNames
//                    )
//                    binding.inputNamaClient.setAdapter(adapterClient)
//                }
//            }
//        }
//
//        rentalReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
//            response?.let {
//                if (it.success) {
//                    Toast.makeText(
//                        requireContext(), "Transaksi berhasil dibuat", Toast.LENGTH_LONG
//                    ).show()
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.host_fragment_user, ListTransaksiRentalFragment())
//                        .addToBackStack(null).commit()
//                } else {
//                    Toast.makeText(
//                        requireContext(),
//                        "Gagal membuat transaksi: ${it.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//                rentalReportViewModel.resetCreateTransactionResponse()
//            }
//        }
//    }
//
//private fun setupViews() {
//    binding.arrowBack.setOnClickListener {
//        parentFragmentManager.beginTransaction()
//            .replace(R.id.host_fragment_user, UserDashboardFragment()).addToBackStack(null)
//            .commit()
//    }
//
//    binding.btnAddItem.setOnClickListener {
//        if (fullRentalList.isNotEmpty()) {
//            addRentalItemInput()
//            updateTotalPrice()
//        } else {
//            Toast.makeText(
//                requireContext(), "Data rental kosong untuk cabang ini", Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
//
//    binding.btnSubmit.setOnClickListener {
//        if (validateRentalTransaction()) {
//            processTransaction()
//        }
//    }
//
//    val statusAdapter = ArrayAdapter(
//        requireContext(),
//        android.R.layout.simple_dropdown_item_1line,
//        statusOptions.map { it.displayName })
//    binding.inputStatusTransaksi.setAdapter(statusAdapter)
//
//    binding.inputStatusTransaksi.setOnItemClickListener { parent, _, position, _ ->
//        selectedTransactionStatus = statusOptions[position].value
//    }
//
//    // Add text change listeners for promo and additional cost
//    binding.inputHargaPerKg.addTextChangedListener(object : TextWatcher {
//        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        override fun afterTextChanged(s: Editable?) {
//            updateTotalPrice()
//        }
//    })
//
//    binding.inputPromoTransaksi.addTextChangedListener(object : TextWatcher {
//        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        override fun afterTextChanged(s: Editable?) {
//            updateTotalPrice()
//        }
//    })
//
//    binding.inputTambahanTransaksi.addTextChangedListener(object : TextWatcher {
//        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        override fun afterTextChanged(s: Editable?) {
//            updateTotalPrice()
//        }
//    })
//}
//
//private fun addRentalItemInput(index: Int = 0) {
//    val itemView = layoutInflater.inflate(
//        R.layout.kurir_list_item_tran
//        // Dapatkan spinner baru terlebih dahulu
//        val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
//        val beratEditText = itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
//        val trashIcon = itemView.findViewById<ImageView>(R.id.trash_create_list_transaksi)
//        val kondisiDropdown =
//            itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
//
//        // Ambil semua ID dari spinner yang SUDAH ditambahkan sebelumnya (bukan spinner baru)
//        val selectedRentalItemIds = mutableSetOf<Int>()
//        for (i in 0 until binding.containerRental.childCount) {
//            val existingView = binding.containerRental.getChildAt(i)
//            val existingSpinner = existingView.findViewById<Spinner>(R.id.spinnerRental)
//            val existingTag = existingSpinner.getTag(R.id.spinnerRental) as? Int
//            existingTag?.let { selectedRentalItemIds.add(it) }
//        }
//
//        // Setup adapter untuk kondisi
//        val conditionAdapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_dropdown_item_1line,
//            conditionOptions.map { it.displayName }
//        )
//        kondisiDropdown.setAdapter(conditionAdapter)
//
//        // Filter berdasarkan tipe transaksi
//        val typeFilteredRentalList = if (selectedTransactionType.isNotEmpty()) {
//            fullRentalList.filter { rentalItem ->
//                val rental_nama = when (selectedTransactionType.lowercase()) {
//                    "bath towel" -> Triple("bath towel", "handuk besar", "besar")
//                    "hand towel" -> Triple("hand towel", "handuk kecil", "kecil")
//                    "gorde" -> Triple("gorden", "gorden", "gorden")
//                    "keset" -> Triple("keset", "keset", "gorden")
//                    else -> Triple("-", "-", "-")
//                }
//                rentalItem.name_rental_item?.lowercase()?.let { name ->
//                    name.contains(rental_nama.first) || name.contains(rental_nama.second) || name.contains(
//                        rental_nama.third
//                    )
//                } == true
//            }
//        } else {
//            fullRentalList
//        }
//
//        // Hilangkan item rental yang sudah dipilih sebelumnya
//        val availableRentalList = typeFilteredRentalList.filter { rentalItem ->
//            rentalItem.id_rental_item !in selectedRentalItemIds
//        }
//
//        if (availableRentalList.isEmpty()) {
//            val message = if (selectedTransactionType.isNotEmpty()) {
//                "Tidak ada item rental untuk tipe ${selectedTransactionType} yang tersedia"
//            } else {
//                "Semua item rental sudah ditambahkan"
//            }
//            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Atur spinner baru
//        val adapter = ArrayAdapter(
//            requireContext(),
//            R.layout.kurir_spinner_item_laundry,
//            availableRentalList.map { "${it.number_rental_item} - ${it.name_rental_item}" }
//        )
//        adapter.setDropDownViewResource(R.layout.kurir_spinner_dropdown_item_laundry)
//        spinner.adapter = adapter
//
//        val validIndex = if (index in 0 until availableRentalList.size) index else 0
//        spinner.setSelection(validIndex)
//
//        // Simpan tag ID rental item yang dipilih awal
//        spinner.setTag(R.id.spinnerRental, availableRentalList[validIndex].id_rental_item)
//
//        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                if (position in 0 until availableRentalList.size) {
//                    val selectedItem = availableRentalList[position]
//                    spinner.setTag(R.id.spinnerRental, selectedItem.id_rental_item)
//                    val status = selectedItem.status_rental_item.toString()
//                    val kondisi = selectedItem.condition_rental_item.toString()
//
//                    val textStatus = itemView.findViewById<TextView>(R.id.status_product_rental)
//                    val textKondisi = itemView.findViewById<TextView>(R.id.condition_product_rental)
//
//                    when (status) {
//                        "available" -> {
//                            textStatus.text = "TERSEDIA"
//                        }
//                        "rented" -> {
//                            textStatus.text = "DISEWA"
//                        }
//                        "maintenance" -> {
//                            textStatus.text = "PEMELIHARAAN"
//                        }
//                    }
//
//                    when (kondisi) {
//                        "clean" -> {
//                            textKondisi.text = "BERSIH"
//                        }
//                        "dirty" -> {
//                            textKondisi.text = "KOTOR"
//                        }
//                        "damaged" -> {
//                            textKondisi.text = "RUSAK"
//                        }
//                    }
//
//                    updateTotalPrice()
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }
//
//        beratEditText.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                updateTotalPrice()
//            }
//        })
//
//        trashIcon.setOnClickListener {
//            binding.containerRental.removeView(itemView)
//            updateTotalPrice()
//            if (binding.containerRental.childCount == 0) {
//                Toast.makeText(requireContext(), "Semua item dihapus", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        binding.containerRental.addView(itemView)
//    }
//
//    private fun updateTotalPrice() {
//        var totalBerat = 0.0
//        var totalPrice = 0.0
//
//        for (i in 0 until binding.containerRental.childCount) {
//            val itemView = binding.containerRental.getChildAt(i)
//            val beratEditText =
//                itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
//
//            val berat = beratEditText.text.toString().toDoubleOrNull() ?: 0.0
//
//            totalBerat += berat
//        }
//        val hargaPerKg = binding.inputHargaPerKg.text.toString().toDoubleOrNull() ?: 0.0
//        totalPrice = totalBerat * hargaPerKg
//
//        binding.pcsTransaksiRental.text = binding.containerRental.childCount.toString()
//        binding.totalBerat.text = String.format("%.2f Kg", totalBerat)
//        binding.totalPrice.text = formatToRupiah(totalPrice)
//        binding.totalTransaksiRental.text = formatToRupiah(totalPrice)
//    }
//
//    private fun initFilterBottomSheet() {
//        val inflater =
//            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        filterBottomSheetView = inflater.inflate(R.layout.kurir_layout_filter_product_rental, null)
//
//        filterBottomSheetDialog =
//            BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
//        filterBottomSheetDialog.setContentView(filterBottomSheetView)
//
//        val conditionContainer =
//            filterBottomSheetView.findViewById<FlexboxLayout>(R.id.wadah_button_filter_kondisi)
//        val statusContainer =
//            filterBottomSheetView.findViewById<FlexboxLayout>(R.id.wadah_button_filter_status)
//
//        createFilterButtons("condition", conditionContainer)
//        createFilterButtons("status", statusContainer)
//
//        val terapkanFilter =
//            filterBottomSheetView.findViewById<MaterialButton>(R.id.terapkan_filter)
//        val resetFilter = filterBottomSheetView.findViewById<MaterialButton>(R.id.reset_filter)
//
//        terapkanFilter.setOnClickListener {
//            applyFilters()
//            filterBottomSheetDialog.dismiss()
//        }
//
//        resetFilter.setOnClickListener {
//            resetAllFilters()
//            filterBottomSheetDialog.dismiss()
//        }
//
//        val behavior = filterBottomSheetDialog.behavior
//        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//        behavior.isDraggable = true
//        behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
//    }
//
//    private fun Int.dpToPx(context: Context): Int {
//        return (this * context.resources.displayMetrics.density).toInt()
//    }
//
//    private fun createFilterButtons(filterType: String, container: FlexboxLayout) {
//        filterOptions[filterType]?.forEach { option ->
//            // Create card and text view
//            val card = MaterialCardView(requireContext()).apply {
//                id = View.generateViewId()
//                layoutParams = FlexboxLayout.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
//                ).apply {
//                    setMargins(6, 6, 6, 6)
//                }
//                cardElevation = 0f
//                radius = resources.getDimension(R.dimen.card_corner_radius)
//                strokeWidth = 2
//                strokeColor = ContextCompat.getColor(requireContext(), R.color.blue500)
//                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
//            }
//
//            val textView = TextView(requireContext()).apply {
//                id = View.generateViewId()
//                text = option.displayName
//                setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
//                textSize = 14f
//                setPadding(
//                    18.dpToPx(requireContext()),
//                    8.dpToPx(requireContext()),
//                    18.dpToPx(requireContext()),
//                    8.dpToPx(requireContext())
//                )
////                setTypeface(ResourcesCompat.getFont(requireContext(), R.font.inter_semibold))
//            }
//
//            // Add text view to card
//            card.addView(textView)
//
//            // Set click listener
//            card.setOnClickListener {
//                toggleFilterOption(filterType, option.value, card, textView)
//            }
//
//            // Add card to container
//            container.addView(card)
//        }
//    }
//
//    private fun toggleFilterOption(filterType: String, value: String, card: MaterialCardView, textView: TextView) {
//        val currentSelections = selectedFilters[filterType] ?: return
//        val buttonPair = Pair(card, textView)
//
//        if (currentSelections.contains(value)) {
//            // Unselect
//            currentSelections.remove(value)
//            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
//            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
//            filterButtons[filterType]?.remove(buttonPair)
//        } else {
//            // Select
//            currentSelections.add(value)
//            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue500))
//            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
//            filterButtons[filterType]?.add(buttonPair)
//        }
//    }
//
//    private fun applyFilters() {
//        val currentRentalList = rentalProductViewModel.rentalProducts.value?.filter { product ->
//            product.id_branch_rental_item?.toInt() == userIdBranch
//        } ?: listOf()
//
//        val filteredList = currentRentalList.filter { rental ->
//            val conditionFilters = selectedFilters["condition"] ?: emptySet()
//            val statusFilters = selectedFilters["status"] ?: emptySet()
//
//            // Cek kondisi
//            val conditionMatches = if (conditionFilters.isEmpty()) {
//                true
//            } else {
////                val conditionName = when (rental.co) {
////                    ConditionRental.clean -> "clean"
////                    ConditionRental.dirty -> "dirty"
////                    ConditionRental.damaged -> "damaged"
////                }
////                conditionFilters.contains(conditionName)
//            }
//
//            // Cek status
//            val statusMatches = if (statusFilters.isEmpty()) {
//                true
//            } else {
////                val statusName = when (rental.status_rental_item) {
////                    StatusRental.available -> "available"
////                    StatusRental.rented -> "rented"
////                    StatusRental.maintenance -> "maintenance"
////                }
////                statusFilters.contains(statusName)
////            }
//
//            // Cek berdasarkan selectedTransactionType
////            val keywordMap = mapOf(
////                "bath towel" to listOf("bath towel", "handuk besar", "besar"),
////                "hand towel" to listOf("hand towel", "handuk kecil", "kecil"),
////                "gorden" to listOf("gorden"),
////                "keset" to listOf("keset")
////            )
//
////            val keywords = keywordMap[selectedTransactionType.lowercase()] ?: emptyList()
////            val nameLower = rental.name_rental_item?.lowercase().orEmpty()
////            val typeMatch = selectedTransactionType.isEmpty() || keywords.any { keyword ->
////                nameLower.contains(keyword)
////            }
//
////            conditionMatches && statusMatches && typeMatch
//        }
//
//        if (filteredList.isEmpty() && (selectedFilters["condition"]?.isNotEmpty() == true || selectedFilters["status"]?.isNotEmpty() == true || selectedTransactionType.isNotEmpty())) {
//            Toast.makeText(
//                requireContext(),
//                "Tidak ada item rental dengan filter yang dipilih",
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//
//        fullRentalList = filteredList
//        binding.containerRental.removeAllViews()
//
//        if (filteredList.isNotEmpty()) {
//            addRentalItemInput()
//            updateTotalPrice()
//        } else {
//            binding.pcsTransaksiRental.text = "0"
//            binding.totalBerat.text = "0.00 Kg"
//            binding.totalPrice.text = formatToRupiah(0.0)
//        }
//    }
//
//    private fun resetAllFilters() {
//        // Clear all selections
//        selectedFilters.forEach { (_, selections) -> selections.clear() }
//
//        // Reset UI for all buttons
//        filterButtons.forEach { (_, buttons) ->
//            buttons.forEach { (card, textView) ->
//                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
//                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue500))
//            }
//            buttons.clear()
//        }
//
//        // Also clear the transaction type input
//        selectedTransactionType = ""
//        binding.inputTypeTransaksi.setText("")
//
//        // Reload the original full list
//        if (userIdBranch != 0) {
//            viewLifecycleOwner.lifecycleScope.launch {
//                try {
//                    rentalProductViewModel.getProductRental()
//
//                    // Wait for the data to be loaded
//                    rentalProductViewModel.rentalProducts.value?.let { products ->
//                        fullRentalList = products.filter { product ->
//                            product.id_branch_rental_item?.toInt() == userIdBranch
//                        }
//
//                        // Clear and reinitialize the rental items container
//                        binding.containerRental.removeAllViews()
//                        if (fullRentalList.isNotEmpty()) {
//                            addRentalItemInput()
//                            updateTotalPrice()
//                        }
//                    }
//                } catch (e: Exception) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Gagal memuat ulang data rental: ${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }
//
//    private fun showFilterBottomSheet() {
//        filterBottomSheetDialog.show()
//    }
//
//    private fun setupTextWatchers() {
//        binding.inputHargaPerKg.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                updateTotalAmountToPay()
//            }
//        })
//
//        binding.inputPromoTransaksi.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                updateTotalAmountToPay()
//            }
//        })
//
//        binding.inputTambahanTransaksi.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                updateTotalAmountToPay()
//            }
//        })
//    }
//
//    private fun updateTotalAmountToPay() {
//
//        val basePrice = extractNumericValue(binding.totalPrice.text.toString())
//        val promo = binding.inputPromoTransaksi.text.toString().toDoubleOrNull() ?: 0.0
//        val additionalCost = binding.inputTambahanTransaksi.text.toString().toDoubleOrNull() ?: 0.0
//        val totalAmountToPay = (basePrice - promo) + additionalCost
//        binding.totalTransaksiRental.text = formatToRupiah(totalAmountToPay)
//    }
//
//    private fun extractNumericValue(text: String): Double {
//        val cleanText = text.replace("Rp", "").replace(".", "").replace(",00", "")
//            .replace("\\s".toRegex(), "") // menghapus spasi
//        return cleanText.toIntOrNull()?.toDouble() ?: 0.0
//    }
//
//    private fun formatToRupiah(amount: Double): String {
//        val localeID = Locale("in", "ID")
//        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
//        return numberFormat.format(amount).replace(",00", "")
//    }
//
//    private fun validateRentalTransaction(): Boolean {
//        // validasi type transaksi rental
//        if (binding.inputTypeTransaksi.text.isNullOrEmpty()) {
//            Toast.makeText(
//                requireContext(), "Pilih type transaksi terlebih dahulu", Toast.LENGTH_SHORT
//            ).show()
//            return false
//        }
//
//        val selectedLabel = selectedTransactionType.toString()
//        val selectedValue = typeTransaksi.find { it.value == selectedLabel }?.value
//        if (selectedValue == null) {
//            Toast.makeText(requireContext(), "Status transaksi belum dipilih", Toast.LENGTH_SHORT)
//                .show()
//            return false
//        }
//
//        // Cek apakah ada item rental
//        if (binding.containerRental.childCount == 0) {
//            Toast.makeText(
//                requireContext(), "Tambahkan minimal satu item rental", Toast.LENGTH_SHORT
//            ).show()
//            return false
//        }
//
//        // Validasi item rental satu per satu
//        for (i in 0 until binding.containerRental.childCount) {
//            val itemView = binding.containerRental.getChildAt(i)
//            val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
//            val beratEditText =
//                itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
//            val kondisiDropdown =
//                itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
//
//            val position = spinner.selectedItemPosition
//            if (position < 0 || position >= fullRentalList.size) {
//                Toast.makeText(
//                    requireContext(),
//                    "Pemilihan item rental tidak valid pada baris ke-${i + 1}",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return false
//            }
//
//            val berat = beratEditText.text.toString().toDoubleOrNull()
//            if (berat == null || berat <= 0) {
//                Toast.makeText(
//                    requireContext(),
//                    "Berat item rental harus diisi dan lebih dari 0 pada baris ke-${i + 1}",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return false
//            }
//
//            if (kondisiDropdown.text.isNullOrEmpty()) {
//                Toast.makeText(
//                    requireContext(),
//                    "Pilih kondisi item rental terlebih dahulu",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return false
//            }
//
//            val selectedKondisiCondition = kondisiDropdown.text.toString()
//            val selectedKondisiValue =
//                conditionOptions.find { it.displayName == selectedKondisiCondition }?.value
//            if (selectedKondisiValue == null) {
//                Toast.makeText(
//                    requireContext(),
//                    "Kondisi item rental tidak sesuai",
//                    Toast.LENGTH_SHORT
//                )
//                    .show()
//                return false
//            }
//        }
//
//        // Validasi input lainnya
//        val pricePerKg = binding.inputHargaPerKg.text.toString().toIntOrNull()
//        if (pricePerKg == null || pricePerKg <= 0) {
//            Toast.makeText(
//                requireContext(), "Harga per Kg harus diisi dengan benar", Toast.LENGTH_SHORT
//            ).show()
//            return false
//        }
//
//        if (binding.totalBerat.text.isNullOrEmpty()) {
//            Toast.makeText(
//                requireContext(), "Masukkan Berat Item Rental terlebih dahulu", Toast.LENGTH_SHORT
//            ).show()
//            return false
//        }
//
//        if (binding.inputNamaClient.text.isNullOrEmpty()) {
//            Toast.makeText(requireContext(), "Pilih client terlebih dahulu", Toast.LENGTH_SHORT)
//                .show()
//            return false
//        }
//
//        if (binding.inputNamaPenerima.text.isNullOrEmpty()) {
//            Toast.makeText(requireContext(), "Masukkan nama penerima", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        if (binding.inputTypeTransaksi.text.isNullOrEmpty()) {
//            Toast.makeText(
//                requireContext(), "Pilih type transaksi terlebih dahulu", Toast.LENGTH_SHORT
//            ).show()
//            return false
//        }
//
//        val selectedStatusLabel = selectedTransactionStatus.toString()
//        val selectedStatusValue = statusOptions.find { it.value == selectedStatusLabel }?.value
//        if (selectedStatusValue == null) {
//            Toast.makeText(
//                requireContext(), "Pilih Status Transaksi yang benar", Toast.LENGTH_SHORT
//            ).show()
//            return false
//        }
//
//        if (binding.inputStatusTransaksi.text.isNullOrEmpty()) {
//            Toast.makeText(requireContext(), "Pilih status transaksi", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        return true
//    }
//
//    private fun processTransaction() {
//        rentalTransactionItems.clear()
//
//        if (binding.containerRental.childCount == 0) {
//            Toast.makeText(
//                requireContext(),
//                "Silahkan tambahkan item rental terlebih dahulu",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }
//
//        val selectedStatusLabel = selectedTransactionStatus.toString()
//        val selectedStatusValue = statusOptions.find { it.value == selectedStatusLabel }?.value
//        if (selectedStatusValue == null) {
//            Toast.makeText(
//                requireContext(),
//                "Status transaksi belum dipilih",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }
//
//        for (i in 0 until binding.containerRental.childCount) {
//            val itemView = binding.containerRental.getChildAt(i)
//            val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
//            val beratEditText =
//                itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
//            val noteEditText =
//                itemView.findViewById<EditText>(R.id.input_note_list_transaksi_rental)
//            val kondisiDropdown =
//                itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
//
//            val berat = beratEditText.text.toString().toDoubleOrNull()
//            if (berat == null || berat <= 0) {
//                Toast.makeText(
//                    requireContext(),
//                    "Berat item rental harus diisi dengan benar",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return
//            }
//
//            val selectedKondisiCondition = kondisiDropdown.text.toString()
//            val selectedKondisiValue =
//                conditionOptions.find { it.displayName == selectedKondisiCondition }?.value
//            if (selectedKondisiValue == null) {
//                Toast.makeText(
//                    requireContext(),
//                    "Kondisi item rental tidak sesuai",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return
//            }
//
//            val selectedRentalItemId = spinner.getTag(R.id.spinnerRental) as? Int
//            val selected = fullRentalList.find { it.id_rental_item == selectedRentalItemId }
//            if (selected == null) {
//                Toast.makeText(
//                    requireContext(),
//                    "Item rental tidak valid atau tidak ditemukan",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return
//            }
//
//            val transactionItem = RentalTransactionItem(
//                id_item_rental = selected.id_rental_item,
//                type_list_rental_transaction = selectedTransactionType.toString(),
//                condition_list_transaction_rental = selectedKondisiValue,
//                note_list_transaction_rental = noteEditText.text.toString(),
//                weight_list_transaction_rental = berat
//            )
//
//            rentalTransactionItems.add(transactionItem)
//        }
//
//        val promo = binding.inputPromoTransaksi.text.toString().toDoubleOrNull() ?: 0.0
//        val additionalCost = binding.inputTambahanTransaksi.text.toString().toDoubleOrNull() ?: 0.0
//        val pricePerKg = binding.inputHargaPerKg.text.toString().toInt()
//
//        val rentalTransactionRequest = RentalTransactionRequest(
//            id_kurir_transaction_rental = userId,
//            id_branch_transaction_rental = userIdBranch,
//            id_client_transaction_rental = clientId,
//            recipient_name_transaction_rental = binding.inputNamaPenerima.text.toString(),
//            type_rental_transaction = selectedTransactionType.toString(),
//            status_transaction_rental = selectedStatusValue,
//            price_weight_transaction_rental = pricePerKg,
//            promo_transaction_rental = promo,
//            additional_cost_transaction_rental = additionalCost,
//            notes_transaction_laundry = binding.inputNoteTransaksi.text.toString(),
//            list_transaction_rentals = rentalTransactionItems
//        )
//
//        rentalReportViewModel.createRentalTransaction(rentalTransactionRequest)
//    }
//}