package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.helper.TransactionDraftHelper
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentCreateListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.RentalTransactionData
import dev.ferynnd.baguslaundry.model.RentalTransactionItem
import dev.ferynnd.baguslaundry.model.RentalTransactionRequest
import dev.ferynnd.baguslaundry.ui.openUserFragment
import dev.ferynnd.baguslaundry.ui.showAlert
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class CreateListTransaksiRentalFragment : Fragment() {
    private var _binding: KurirFragmentCreateListTransaksiRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var rentalReportViewModel: RentalReportViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var draftHelper: TransactionDraftHelper
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var userId: Int = 0
    private var userIdBranch: Int = 0

    private var fullRentalList: List<ProductRental> = listOf()
    private var clientList: List<Client> = listOf()

    private var isUserDataLoaded = false
    private var isRentalDataLoaded = false
    private var isClientDataLoaded = false
    private var isRestoringState = false

    // Handler untuk debounce save
    private val saveHandler = Handler(Looper.getMainLooper())
    private var saveRunnable: Runnable? = null

    data class FilterOption(val value: String, val displayName: String)

    private val statusOptions = listOf(
        FilterOption("out", "Keluar"),
        FilterOption("in", "Masuk"),
        FilterOption("cancelled", "Dibatalkan")
    )

    private val conditionOptions = listOf(
        FilterOption("clean", "Bersih"),
        FilterOption("dirty", "Kotor"),
        FilterOption("damaged", "Rusak")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java].apply { init(requireContext()) }
        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java].apply { init(requireContext()) }
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java].apply { init(requireContext()) }

        // Initialize draft helper
        draftHelper = TransactionDraftHelper(requireContext())

        Log.d("FragmentLifecycle", "onCreate - Draft exists: ${draftHelper.hasUnsavedData()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = KurirFragmentCreateListTransaksiRentalBinding.inflate(layoutInflater)
        hideBottomNavigationView()

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        if (userId != 0) {
            updateLoadingState(true)
            setupObservers()
            loadInitialData()
        } else {
            Toast.makeText(context, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        setupClickListeners()

        Log.d("FragmentLifecycle", "onCreateView")
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.d("FragmentLifecycle", "onResume - Will restore after data loaded")
    }

    override fun onPause() {
        super.onPause()
        // Save current state when leaving
        saveAllCurrentState()
        Log.d("FragmentLifecycle", "onPause - State saved")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveHandler.removeCallbacksAndMessages(null)
        _binding = null
        Log.d("FragmentLifecycle", "onDestroyView")
    }

    private fun setupObservers() {
        rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isUserDataLoaded && isRentalDataLoaded && isClientDataLoaded) {
                updateLoadingState(isLoading)
            }
        }

        rentalReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
            handleTransactionResponse(response)
        }
    }

    private fun setupClickListeners() {
        binding.arrowBack.setOnClickListener {
            openUserFragment(UserDashboardFragment(), "UserDashboard")
        }

        binding.btnAddItem.setOnClickListener {
            if (fullRentalList.isNotEmpty()) {
                addRentalItemInput()
            } else {
                Toast.makeText(requireContext(), "Data rental kosong untuk cabang ini", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (validateRentalTransaction()) {
                processTransaction()
            }
        }
    }

    private fun loadInitialData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                loadUserData()
            } catch (e: Exception) {
                updateLoadingState(false)
                Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun loadUserData() {
        try {
            val userResponse = userViewModel.getUserById(userId)
            if (userResponse.success) {
                userIdBranch = userResponse.data.id_branch_user!!.toInt()
                binding.namaKurir.text = userResponse.data.fullname_user ?: "-"
                isUserDataLoaded = true

                coroutineScope {
                    launch { loadRentalProductsData() }
                    launch { loadClientData() }
                }
            } else {
                updateLoadingState(false)
                Toast.makeText(requireContext(), "Gagal memuat data pengguna", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            updateLoadingState(false)
            Toast.makeText(requireContext(), "Gagal memuat data pengguna: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun loadRentalProductsData() {
        try {
            val rentalResponse = rentalProductViewModel.getProductRental()
            rentalProductViewModel.filteredRentalProducts.value?.let { rentalList ->
                handleRentalProductsData(rentalList)
            }
            isRentalDataLoaded = true
            checkAllDataLoaded()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal memuat data product item: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadClientData() {
        clientViewModel.getClient()

        clientViewModel.clients.observe(viewLifecycleOwner) { clients ->
            if (clients != null) {
                handleClientData(clients)
                isClientDataLoaded = true
                checkAllDataLoaded()
            }
        }

        clientViewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAllDataLoaded() {
        if (isUserDataLoaded && isRentalDataLoaded && isClientDataLoaded) {
            updateLoadingState(false)

            // Restore draft setelah semua data siap
            restoreAllState()

            // Setup text watchers AFTER restore
            setupTextWatchers()
        }
    }

    private fun handleRentalProductsData(rentalList: List<ProductRental>) {
        val filtered = rentalList.filter { item ->
            item.id_branch_rental_item?.toInt() == userIdBranch
        }
        fullRentalList = filtered

        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada item rental untuk cabang ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleClientData(clients: List<Client>) {
        val filteredClients = clients.filter { client ->
            client.id_branch_client?.toInt() == userIdBranch
        }
        clientList = filteredClients

        val clientNames = clientList.map { it.name_client ?: "" }
        val adapterClient = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            clientNames
        )
        binding.inputNamaClient.setAdapter(adapterClient)

        binding.inputNamaClient.setOnItemClickListener { _, _, position, _ ->
            val selectedClient = clientList.getOrNull(position)
            val clientId = selectedClient?.id_client ?: 0
            val clientName = selectedClient?.name_client ?: ""
            val clientAddress = selectedClient?.full_address_client ?: "-"

            if (!isRestoringState) {
                draftHelper.saveClient(clientName, clientId, clientAddress)
            }

            binding.alamatKlien.text = clientAddress
        }

        if (clientList.isEmpty()) {
            binding.alamatKlien.text = "-"
        }
    }

    private fun restoreAllState() {
        if (!draftHelper.hasUnsavedData()) {
            Log.d("RestoreState", "No draft data to restore")
            return
        }

        isRestoringState = true
        Log.d("RestoreState", "Starting restore...")

        // Restore form fields
        val nomorNota = draftHelper.getNomorNota()
        if (nomorNota.isNotEmpty()) {
            binding.inputNomorNota.setText(nomorNota)
            Log.d("RestoreState", "Restored nomor nota: $nomorNota")
        }

        val clientName = draftHelper.getClientName()
        if (clientName.isNotEmpty()) {
            binding.inputNamaClient.setText(clientName, false)
            binding.alamatKlien.text = draftHelper.getClientAddress()
            Log.d("RestoreState", "Restored client: $clientName")
        }

        val namaPenerima = draftHelper.getNamaPenerima()
        if (namaPenerima.isNotEmpty()) {
            binding.inputNamaPenerima.setText(namaPenerima)
            Log.d("RestoreState", "Restored nama penerima: $namaPenerima")
        }

        val noteTransaksi = draftHelper.getNoteTransaksi()
        if (noteTransaksi.isNotEmpty()) {
            binding.inputNoteTransaksi.setText(noteTransaksi)
            Log.d("RestoreState", "Restored note: $noteTransaksi")
        }

        // Restore rental items
        val savedItems = draftHelper.getRentalItems()
        Log.d("RestoreState", "Restoring ${savedItems.size} rental items")

        savedItems.forEach { savedItem ->
            val itemView = layoutInflater.inflate(
                R.layout.kurir_list_item_transaksi_rental_input,
                binding.containerRental,
                false
            )
            setupRentalItemView(itemView, savedItem)
            binding.containerRental.addView(itemView)
        }

        isRestoringState = false
        Log.d("RestoreState", "Restore completed")
    }

    private fun setupTextWatchers() {
        binding.inputNomorNota.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isRestoringState) {
                    debouncedSave { draftHelper.saveNomorNota(s.toString()) }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.inputNamaPenerima.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isRestoringState) {
                    debouncedSave { draftHelper.saveNamaPenerima(s.toString()) }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.inputNoteTransaksi.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isRestoringState) {
                    debouncedSave { draftHelper.saveNoteTransaksi(s.toString()) }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun debouncedSave(action: () -> Unit) {
        saveRunnable?.let { saveHandler.removeCallbacks(it) }
        saveRunnable = Runnable { action() }
        saveHandler.postDelayed(saveRunnable!!, 500) // Save after 500ms idle
    }

    private fun addRentalItemInput() {
        val itemView = layoutInflater.inflate(
            R.layout.kurir_list_item_transaksi_rental_input,
            binding.containerRental,
            false
        )

        setupRentalItemView(itemView, null)
        binding.containerRental.addView(itemView)

        // Save immediately
        saveAllRentalItems()

        Log.d("AddItem", "Added new rental item, total views: ${binding.containerRental.childCount}")
    }

    private fun setupRentalItemView(itemView: View, savedState: TransactionDraftHelper.RentalItemDraft?) {
        val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
        val statusAutoComplete = itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)
        val kondisiAutoComplete = itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
        val beratEditText = itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
        val pcsEditText = itemView.findViewById<EditText>(R.id.input_pcs_list_transaksi_rental)
        val trashIcon = itemView.findViewById<ImageView>(R.id.trash_create_list_transaksi)

        // Setup spinner
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            fullRentalList.map { it.name_rental_item }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // Setup status dropdown
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statusOptions.map { it.displayName }
        )
        statusAutoComplete.setAdapter(statusAdapter)

        // Setup kondisi dropdown
        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            conditionOptions.map { it.displayName }
        )
        kondisiAutoComplete.setAdapter(conditionAdapter)

        // Restore saved state
        if (savedState != null) {
            Log.d("RestoreItem", "Restoring item: ${savedState.selectedItemName}")

            if (savedState.selectedItemPosition >= 0 && savedState.selectedItemPosition < fullRentalList.size) {
                spinner.setSelection(savedState.selectedItemPosition)
            }
            spinner.setTag(R.id.spinnerRental, savedState.selectedItemId)

            if (savedState.status.isNotEmpty()) {
                statusAutoComplete.setText(savedState.status, false)
            }
            if (savedState.kondisi.isNotEmpty()) {
                kondisiAutoComplete.setText(savedState.kondisi, false)
            }
            if (savedState.berat.isNotEmpty()) {
                beratEditText.setText(savedState.berat)
            }
            if (savedState.pcs.isNotEmpty()) {
                pcsEditText.setText(savedState.pcs)
            }
        }

        // Spinner listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = fullRentalList.getOrNull(position)
                if (selectedItem != null) {
                    spinner.setTag(R.id.spinnerRental, selectedItem.id_rental_item)
                    if (!isRestoringState) {
                        debouncedSave { saveAllRentalItems() }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Text watchers
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isRestoringState) {
                    debouncedSave { saveAllRentalItems() }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        statusAutoComplete.addTextChangedListener(textWatcher)
        kondisiAutoComplete.addTextChangedListener(textWatcher)
        beratEditText.addTextChangedListener(textWatcher)
        pcsEditText.addTextChangedListener(textWatcher)

        // Delete button
        trashIcon.setOnClickListener {
            binding.containerRental.removeView(itemView)
            saveAllRentalItems()

            Log.d("DeleteItem", "Removed item, remaining: ${binding.containerRental.childCount}")

            if (binding.containerRental.childCount == 0) {
                Toast.makeText(requireContext(), "Semua item dihapus", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAllRentalItems() {
        val items = mutableListOf<TransactionDraftHelper.RentalItemDraft>()

        for (i in 0 until binding.containerRental.childCount) {
            val itemView = binding.containerRental.getChildAt(i)
            val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
            val statusAutoComplete = itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)
            val kondisiAutoComplete = itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
            val beratEditText = itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
            val pcsEditText = itemView.findViewById<EditText>(R.id.input_pcs_list_transaksi_rental)

            val selectedPosition = spinner.selectedItemPosition
            val selectedItem = fullRentalList.getOrNull(selectedPosition)

            val draft = TransactionDraftHelper.RentalItemDraft(
                selectedItemId = selectedItem?.id_rental_item ?: 0,
                selectedItemPosition = selectedPosition,
                selectedItemName = selectedItem?.name_rental_item ?: "",
                status = statusAutoComplete.text.toString(),
                kondisi = kondisiAutoComplete.text.toString(),
                berat = beratEditText.text.toString(),
                pcs = pcsEditText.text.toString()
            )

            items.add(draft)
        }

        draftHelper.saveRentalItems(items)
        Log.d("SaveDraft", "Saved ${items.size} rental items")
    }

    private fun saveAllCurrentState() {
        // Save rental items
        saveAllRentalItems()

        // Save form fields (if not already saved by text watchers)
        draftHelper.saveNomorNota(binding.inputNomorNota.text.toString())
        draftHelper.saveNamaPenerima(binding.inputNamaPenerima.text.toString())
        draftHelper.saveNoteTransaksi(binding.inputNoteTransaksi.text.toString())

        Log.d("SaveState", "All state saved")
    }

    private fun handleTransactionResponse(response: DefaultRequest<RentalTransactionData>?) {
        try {
            if (response != null) {
                if (response.success) {
                    // Clear draft setelah sukses
                    draftHelper.clearAllData()

                    showAlert(
                        title = "Berhasil!",
                        message = "Transaksi berhasil disimpan",
                        backgroundColorRes = R.color.primary,
                        iconRes = R.drawable.success
                    )

                    val bundle = Bundle()
                    bundle.putInt("transactionId", response.data.id_transaction_rental)
                    val fragment = PrintPreviewRentalFragment()
                    fragment.arguments = bundle

                    openUserFragment(fragment, "PrintPreviewRental")
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Gagal membuat transaksi.", Toast.LENGTH_SHORT).show()
                }
                rentalReportViewModel.resetCreateTransactionResponse()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal memproses response: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateRentalTransaction(): Boolean {
        if (binding.containerRental.childCount == 0) {
            Toast.makeText(requireContext(), "Tambahkan minimal satu item rental", Toast.LENGTH_SHORT).show()
            return false
        }

        for (i in 0 until binding.containerRental.childCount) {
            val itemView = binding.containerRental.getChildAt(i)
            val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
            val pcsEditText = itemView.findViewById<EditText>(R.id.input_pcs_list_transaksi_rental)
            val kondisiDropdown = itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
            val statusDropdown = itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)

            val position = spinner.selectedItemPosition
            if (position < 0 || position >= fullRentalList.size) {
                showAlert(
                    title = "Peringatan!",
                    message = "Pemilihan item rental tidak valid pada baris ke-${i + 1}",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.info
                )
                return false
            }

            val pcs = pcsEditText.text.toString().toDoubleOrNull()
            if (pcs == null || pcs <= 0) {
                showAlert(
                    title = "Peringatan!",
                    message = "Jumlah item rental harus diisi dengan benar",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.info
                )
                return false
            }

            if (statusDropdown.text.isNullOrEmpty()) {
                showAlert(
                    title = "Peringatan!",
                    message = "Pilih status item rental terlebih dahulu",
                    backgroundColorRes = R.color.primary
                )
                return false
            }

            val selectedStatus = statusDropdown.text.toString()
            val selectedStatusValue = statusOptions.find { it.displayName == selectedStatus }?.value
            if (selectedStatusValue == null) {
                showAlert(
                    title = "Peringatan!",
                    message = "Status item rental tidak sesuai",
                    backgroundColorRes = R.color.primary
                )
                return false
            }

            if (kondisiDropdown.text.isNullOrEmpty()) {
                showAlert(
                    title = "Peringatan!",
                    message = "Pilih kondisi item rental terlebih dahulu",
                    backgroundColorRes = R.color.primary
                )
                return false
            }

            val selectedKondisi = kondisiDropdown.text.toString()
            val selectedKondisiValue = conditionOptions.find { it.displayName == selectedKondisi }?.value
            if (selectedKondisiValue == null) {
                showAlert(
                    title = "Peringatan!",
                    message = "Kondisi item rental tidak sesuai",
                    backgroundColorRes = R.color.primary
                )
                return false
            }
        }

        if (binding.inputNamaClient.text.isNullOrEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Pilih client terlebih dahulu",
                backgroundColorRes = R.color.primary
            )
            return false
        }

        if (binding.inputNomorNota.text.isNullOrEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Masukkan nomor nota",
                backgroundColorRes = R.color.primary
            )
            return false
        }

        if (binding.inputNamaPenerima.text.isNullOrEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Masukkan nama penerima",
                backgroundColorRes = R.color.primary
            )
            return false
        }

        return true
    }

    private fun processTransaction() {
        val rentalTransactionItems = mutableListOf<RentalTransactionItem>()

        if (binding.containerRental.childCount == 0) {
            showAlert(
                title = "Peringatan!",
                message = "Silahkan tambahkan item rental terlebih dahulu",
                backgroundColorRes = R.color.primary
            )
            return
        }

        for (i in 0 until binding.containerRental.childCount) {
            val itemView = binding.containerRental.getChildAt(i)
            val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
            val statusDropdown = itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)
            val selectedStatusCondition = statusDropdown.text.toString()
            val selectedStatusValue = statusOptions.find { it.displayName == selectedStatusCondition }?.value

            val kondisiDropdown = itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
            val selectedKondisiCondition = kondisiDropdown.text.toString()
            val selectedKondisiValue = conditionOptions.find { it.displayName == selectedKondisiCondition }?.value

            val beratEditText = itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
            val beratText = beratEditText.text.toString().trim()
            val berat = if (beratText.isEmpty()) 0.0 else beratText.toDoubleOrNull() ?: 0.0

            val pcsEditText = itemView.findViewById<EditText>(R.id.input_pcs_list_transaksi_rental)
            val pcsText = pcsEditText.text.toString().trim()
            val pcs = pcsText.toIntOrNull() ?: 0

            val selectedRentalItemId = spinner.getTag(R.id.spinnerRental) as? Int
            val selected = fullRentalList.find { it.id_rental_item == selectedRentalItemId }
            if (selected == null) {
                showAlert(
                    title = "Peringatan!",
                    message = "Item rental tidak valid atau tidak ditemukan",
                    backgroundColorRes = R.color.primary
                )
                return
            }

            val transactionItem = RentalTransactionItem(
                id_item_rental = selected.id_rental_item,
                status_list_transaction_rental = selectedStatusValue.toString(),
                condition_list_transaction_rental = selectedKondisiValue.toString(),
                count_list_transaction_rental = pcs,
                weight_list_transaction_rental = berat
            )

            rentalTransactionItems.add(transactionItem)
        }

        val clientId = draftHelper.getClientId()

        val rentalTransactionRequest = RentalTransactionRequest(
            id_kurir_transaction_rental = userId,
            id_branch_transaction_rental = userIdBranch,
            id_client_transaction_rental = clientId,
            recipient_name_transaction_rental = binding.inputNamaPenerima.text.toString(),
            number_transaction_rental = binding.inputNomorNota.text.toString().toInt(),
            notes_transaction_rental = binding.inputNoteTransaksi.text.toString(),
            list_transaction_rentals = rentalTransactionItems
        )

        rentalReportViewModel.createRentalTransaction(rentalTransactionRequest)
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.scrollView2.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun hideBottomNavigationView() {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
    }
}