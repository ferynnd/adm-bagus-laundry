package dev.ferynnd.baguslaundry.ui.user

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.api.DefaultRequest
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BottomNavViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.TransactionViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentLaundryTransactionMenuBinding
import dev.ferynnd.baguslaundry.model.LaundryTransactionState
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.TransactionData
import dev.ferynnd.baguslaundry.ui.openUserFragment
import dev.ferynnd.baguslaundry.ui.showAlert
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class LaundryTransactionMenuFragment : Fragment() {

    private var _binding: FragmentLaundryTransactionMenuBinding? = null
    private val binding get() = _binding!!

    private val laundryProductViewModel: LaundryProductViewModel by activityViewModels()
    private val laundryReportViewModel: LaundryReportViewModel by activityViewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by activityViewModels()
    private val bottomNavViewModel: BottomNavViewModel by activityViewModels()

    private val sharedPreferences by lazy { SharePrefrenceHelper(requireContext()) }
    private val weightInputMap = mutableMapOf<Int, TextInputEditText>()

    private val currencyFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    private var isProgrammaticChange = false
    private var userId: Int = 0
    private var userBranchId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        laundryReportViewModel.init(requireContext())
        userViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaundryTransactionMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.postDelayed({ bottomNavViewModel.hide() }, 300)
        transactionViewModel.restoreStateFromPrefs(requireContext())

        setupUI()
        setupObservers()
        setupListeners()
        loadUserData()
    }

    private fun setupUI() {
        setupSpinner()
        setupCurrencyInput(binding.textCash)
        setupClientNameInput()
    }

    private fun setupSpinner() {
        val statusList = StatusReportLaundry.entries.filter { it != StatusReportLaundry.completed }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusText.adapter = adapter
    }

    private fun setupClientNameInput() {
        binding.textClient.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                transactionViewModel.updateClientName(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        // Observe transaction state
        transactionViewModel.state.observe(viewLifecycleOwner) { state ->
            updateUIFromState(state)
        }

        // Observe selected items from product view model
        laundryProductViewModel.selectedItems.observe(viewLifecycleOwner) { selectedItems ->
            transactionViewModel.updateSelectedItems(selectedItems)
            populateItemsList(selectedItems)
        }

        // Observe transaction response
        laundryReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
            response?.let { handleTransactionResponse(it) }
        }

        // Observe errors
        laundryReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                showToast(errorMessage)
                laundryReportViewModel.clearError()
            }
        }
    }

    private fun updateUIFromState(state: LaundryTransactionState) {
        // Update client name if not focused
        if (!binding.textClient.hasFocus()) {
            val currentText = binding.textClient.text.toString()
            if (currentText != state.clientName) {
                binding.textClient.setText(state.clientName)
            }
        }

        // Update cash amount if not focused
        if (!binding.textCash.hasFocus()) {
            val formatted = formatCurrency(state.cashAmount)
            val currentText = binding.textCash.text.toString()
            if (currentText != formatted) {
                isProgrammaticChange = true
                binding.textCash.setText(formatted)
                isProgrammaticChange = false
            }
        }

        // Update totals
        updateTotalPrices(state)
    }

    private fun setupListeners() {
        binding.apply {
            btnSubmit.setOnClickListener { createLaundryTransaction() }
            arrowBack.setOnClickListener { navigateBack() }
            buttonOptional.setOnClickListener { showOptionalDialog() }

            textCash.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!isProgrammaticChange) {
                        transactionViewModel.updateCash(parseCurrencyInput(textCash))
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun loadUserData() {
        userId = sharedPreferences.getString(PREF_USER_ID)?.toIntOrNull() ?: 0

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userResponse = userViewModel.getUserById(userId)
                userBranchId = userResponse.data.id_branch_user ?: 0
            } catch (e: Exception) {
                showToast("Gagal mendapatkan data pengguna")
            }
        }
    }

    private fun populateItemsList(selectedItems: List<ProductLaundry>) {
        binding.linearLayoutContainer.removeAllViews()
        weightInputMap.clear()

        selectedItems.forEach { item ->
            val itemView = createItemView(item)
            binding.linearLayoutContainer.addView(itemView)
        }
    }

    private fun createItemView(item: ProductLaundry): View {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.card_item_detail_transaction_laundry, binding.linearLayoutContainer, false)

        val textName = itemView.findViewById<TextView>(R.id.textName)
        val textPrice = itemView.findViewById<TextView>(R.id.textPrice)
        val weightInput = itemView.findViewById<TextInputEditText>(R.id.textWeightItem)

        textName.text = item.name_laundry_item
        textPrice.text = formatCurrency(item.price_laundry_item ?: BigDecimal.ZERO)

        setupWeightInput(weightInput, item)
        weightInputMap[item.id_laundry_item ?: 0] = weightInput

        return itemView
    }

    private fun setupWeightInput(weightInput: TextInputEditText, item: ProductLaundry) {
        val watcher = createWeightTextWatcher(weightInput, item)
        weightInput.addTextChangedListener(watcher)

        // Set initial value
        val expectedWeight = item.weight?.takeIf { it != BigDecimal.ZERO }?.toString() ?: ""
        if (!weightInput.hasFocus()) {
            isProgrammaticChange = true
            weightInput.setText(expectedWeight)
            isProgrammaticChange = false
        }
    }

    private fun createWeightTextWatcher(
        weightInput: TextInputEditText,
        item: ProductLaundry
    ): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticChange || !weightInput.hasFocus()) return
                handleWeightChange(weightInput, item)
            }
        }
    }

    private fun handleWeightChange(weightInput: TextInputEditText, item: ProductLaundry) {
        val newWeight = parseWeightInput(weightInput)

        if (weightInput.text.isNullOrEmpty() || newWeight <= BigDecimal.ZERO) {
            weightInput.error = "Berat harus diisi dan lebih dari 0"
        } else {
            weightInput.error = null
        }

        if (item.weight != newWeight) {
            laundryProductViewModel.updateItemWeight(item.id_laundry_item ?: 0, newWeight)
        }
    }

    private fun updateTotalPrices(state: LaundryTransactionState) {
        val subtotal = calculateSubtotal(state.selectedItems)
        val totalBeforeDiscount = subtotal.add(state.additionalCost)
        val finalTotal = totalBeforeDiscount.subtract(state.promoAmount)

        binding.textTotalPrice.text = formatCurrency(finalTotal)

        val returnAmount = state.cashAmount.subtract(finalTotal)
        binding.textChangeMoney.text = formatCurrency(returnAmount.max(BigDecimal.ZERO))
    }

    private fun calculateSubtotal(items: List<ProductLaundry>): BigDecimal {
        return items.fold(BigDecimal.ZERO) { acc, product ->
            val price = product.price_laundry_item ?: BigDecimal.ZERO
            val weight = product.weight ?: BigDecimal.ZERO
            acc.add(price.multiply(weight))
        }
    }

    private fun createLaundryTransaction() {
        val state = transactionViewModel.state.value ?: return

        if (!validateTransaction(state)) return

        val transactionData = buildTransactionData(state)
        laundryReportViewModel.createReportLaundry(transactionData)
    }

    private fun validateTransaction(state: LaundryTransactionState): Boolean {
        return validateClientName(state.clientName) &&
                validateCashAmount(state.cashAmount) &&
                validateSelectedItems(state.selectedItems) &&
                validateItemWeights(state.selectedItems) &&
                validateTransactionStatus() &&
                validateSufficientCash(state)
    }

    private fun validateClientName(clientName: String): Boolean {
        return if (clientName.trim().isEmpty()) {
            showValidationError("Nama pelanggan harus diisi", binding.textClient)
            false
        } else {
            binding.textClient.error = null
            true
        }
    }

    private fun validateCashAmount(cashAmount: BigDecimal): Boolean {
        return when {
            cashAmount <= BigDecimal.ZERO -> {
                showValidationError("Uang tunai harus diisi dan lebih dari 0", binding.textCash)
                false
            }
            else -> {
                binding.textCash.error = null
                true
            }
        }
    }

    private fun validateSelectedItems(items: List<ProductLaundry>): Boolean {
        return if (items.isEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Pilih setidaknya satu item laundry",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            false
        } else true
    }

    private fun validateItemWeights(items: List<ProductLaundry>): Boolean {
        val itemsWithoutWeight = items.filter {
            it.weight == null || it.weight == BigDecimal.ZERO
        }

        return if (itemsWithoutWeight.isNotEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Harap isi berat untuk semua item",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            false
        } else true
    }

    private fun validateTransactionStatus(): Boolean {
        val status = binding.statusText.selectedItem as? StatusReportLaundry
        return if (status == null) {
            showAlert(
                title = "Peringatan!",
                message = "Status transaksi harus dipilih",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            false
        } else true
    }

    private fun validateSufficientCash(state: LaundryTransactionState): Boolean {
        val finalTotal = calculateFinalTotal(state)
        val returnAmount = state.cashAmount.subtract(finalTotal)

        return if (returnAmount < BigDecimal.ZERO) {
            showValidationError("Uang tunai kurang dari total pembayaran", binding.textCash)
            false
        } else {
            binding.textCash.error = null
            true
        }
    }

    private fun buildTransactionData(state: LaundryTransactionState): TransactionData {
        val status = binding.statusText.selectedItem as StatusReportLaundry
        val finalTotal = calculateFinalTotal(state)
        val returnAmount = state.cashAmount.subtract(finalTotal)

        val transactionItems = state.selectedItems.map { item ->
            ListTransactionLaundry(
                id_item_laundry = item.id_laundry_item,
                weight_list_transaction_laundry = item.weight,
                deleted_at = null
            )
        }

        return TransactionData(
            id_kurir_transaction_laundry = userId,
            id_branch_transaction_laundry = userBranchId,
            name_client_transaction_laundry = state.clientName.trim(),
            status_transaction_laundry = status,
            count_item_laundry_transaction_laundry = state.selectedItems.size,
            promo_transaction_laundry = state.promoAmount,
            additional_cost_transaction_laundry = state.additionalCost,
            cash_transaction_laundry = state.cashAmount,
            notes_transaction_laundry = state.notes,
            list_transaction_laundry = transactionItems,
            total_price_transaction_laundry = finalTotal,
            change_money_transaction_laundry = returnAmount,
            total_weight_transaction_laundry = state.selectedItems
                .sumOf { it.weight ?: BigDecimal.ZERO }
                .takeIf { it != BigDecimal.ZERO },
            total_transaction_laundry = finalTotal
        )
    }

    private fun calculateFinalTotal(state: LaundryTransactionState): BigDecimal {
        val subtotal = calculateSubtotal(state.selectedItems)
        return subtotal.add(state.additionalCost).subtract(state.promoAmount)
    }

    private fun handleTransactionResponse(response: DefaultRequest<TransactionData>) {
        if (response.success) {
            showAlert(
                title = "Berhasil!",
                message = "Data berhasil disimpan.",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.success
            )
            laundryProductViewModel.clearSelectedItems()
            transactionViewModel.clearSavedState(requireContext())
            navigateToPrintPreview(response.data.id_transaction_laundry ?: 0)
        } else {
            showToast(response.message ?: "Gagal membuat transaksi.")
        }
        laundryReportViewModel.clearCreateTransactionResponse()
    }

    private fun showOptionalDialog() {
        val state = transactionViewModel.state.value ?: return

        showOptionalDialog(
            context = requireContext(),
            initialNotes = state.notes,
            initialAdditional = state.additionalCost,
            initialPromo = state.promoAmount
        ) { notes, additionalCost, promoAmount ->
            transactionViewModel.updateNotes(notes)
            transactionViewModel.updateAdditionalCost(additionalCost)
            transactionViewModel.updatePromoAmount(promoAmount)
        }
    }

    private fun showOptionalDialog(
        context: Context,
        initialNotes: String,
        initialAdditional: BigDecimal,
        initialPromo: BigDecimal,
        onSave: (String, BigDecimal, BigDecimal) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_additional, null)

        val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotes)
        val editTextAdditionalCost = dialogView.findViewById<TextInputEditText>(R.id.editTextAdditionalCost)
        val editTextPromoAmount = dialogView.findViewById<TextInputEditText>(R.id.editTextPromoAmount)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancelDialog)
        val buttonSave = dialogView.findViewById<MaterialButton>(R.id.buttonSaveDialog)

        editTextNotes.setText(initialNotes)
        editTextAdditionalCost.setText(formatCurrency(initialAdditional))
        editTextPromoAmount.setText(formatCurrency(initialPromo))

        setupCurrencyInput(editTextAdditionalCost)
        setupCurrencyInput(editTextPromoAmount)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        buttonCancel.setOnClickListener { dialog.dismiss() }
        buttonSave.setOnClickListener {
            handleOptionalDialogSave(
                dialog,
                editTextNotes,
                editTextAdditionalCost,
                editTextPromoAmount,
                onSave
            )
        }

        dialog.show()
    }

    private fun handleOptionalDialogSave(
        dialog: AlertDialog,
        notesInput: TextInputEditText,
        additionalCostInput: TextInputEditText,
        promoInput: TextInputEditText,
        onSave: (String, BigDecimal, BigDecimal) -> Unit
    ) {
        val notes = notesInput.text.toString()
        val additionalCost = parseCurrencyInput(additionalCostInput)
        val promoAmount = parseCurrencyInput(promoInput)

        if (!validateOptionalInputs(additionalCostInput, additionalCost, promoInput, promoAmount)) {
            return
        }

        onSave(notes, additionalCost, promoAmount)
        dialog.dismiss()
    }

    private fun validateOptionalInputs(
        additionalCostInput: TextInputEditText,
        additionalCost: BigDecimal,
        promoInput: TextInputEditText,
        promoAmount: BigDecimal
    ): Boolean {
        return when {
            additionalCost < BigDecimal.ZERO -> {
                additionalCostInput.error = "Biaya tambahan tidak boleh negatif"
                false
            }
            promoAmount < BigDecimal.ZERO -> {
                promoInput.error = "Promo tidak boleh negatif"
                false
            }
            else -> {
                additionalCostInput.error = null
                promoInput.error = null
                true
            }
        }
    }

    private fun setupCurrencyInput(editText: TextInputEditText) {
        var currentValue = ""

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != currentValue) {
                    editText.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[Rp,.\\s]".toRegex(), "")

                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = BigDecimal(cleanString)
                            val formatted = currencyFormatter.format(parsed.toDouble())
                            currentValue = formatted
                            editText.setText(formatted)
                            editText.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            // Handle silently
                        }
                    } else {
                        currentValue = ""
                        editText.setText("")
                    }

                    editText.addTextChangedListener(this)
                }
            }
        })
    }

    // ==================== Helper Methods ====================

    private fun parseCurrencyInput(editText: TextInputEditText): BigDecimal {
        val cleanString = editText.text.toString()
            .replace("[Rp,.\\s]".toRegex(), "")
            .trim()
        return try {
            if (cleanString.isEmpty()) BigDecimal.ZERO else BigDecimal(cleanString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    private fun parseWeightInput(editText: TextInputEditText): BigDecimal {
        val cleanString = editText.text.toString().trim()
        return try {
            if (cleanString.isEmpty()) BigDecimal.ZERO else BigDecimal(cleanString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    private fun formatCurrency(value: BigDecimal): String {
        return currencyFormatter.format(value.setScale(0, BigDecimal.ROUND_HALF_UP).toDouble())
    }

    private fun showValidationError(message: String, editText: TextInputEditText) {
        showAlert(
            title = "Peringatan!",
            message = message,
            backgroundColorRes = R.color.primary,
            iconRes = R.drawable.info
        )
        editText.error = message
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    private fun navigateBack() {
        openUserFragment(UserDashboardFragment(), "UserDashboard")
        bottomNavViewModel.show()
    }

    private fun navigateToPrintPreview(transactionId: Int) {
        val bundle = Bundle().apply {
            putInt("transactionId", transactionId)
        }
        val fragment = PrintPreviewFragment().apply {
            arguments = bundle
        }
        openUserFragment(fragment, "PrintPreviewLaundry")
    }

    override fun onPause() {
        super.onPause()
        // 💾 Simpan otomatis ketika user keluar ke Home atau ganti fragment
        transactionViewModel.saveStateToPrefs(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNavViewModel.show()
        _binding = null
    }
}