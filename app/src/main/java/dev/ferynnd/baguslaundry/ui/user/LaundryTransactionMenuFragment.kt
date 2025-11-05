package dev.ferynnd.baguslaundry.ui.user

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.LaundryTransactionMenuAdapter
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentLaundryTransactionMenuBinding
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.TransactionData
import dev.ferynnd.baguslaundry.ui.openUserFragment
import dev.ferynnd.baguslaundry.ui.showAlert
import kotlinx.coroutines.launch
import java.math.BigDecimal // Import BigDecimal
import java.text.NumberFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class LaundryTransactionMenuFragment : Fragment() {

    private lateinit var binding: FragmentLaundryTransactionMenuBinding
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharePrefrences: SharePrefrenceHelper

    // Map untuk menyimpan reference ke EditText berat untuk setiap item
    private val weightInputMap = mutableMapOf<Int, TextInputEditText>()

    private val numberFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true // Untuk pemisah ribuan (titik)
            maximumFractionDigits = 0 // Ini yang menghilangkan ",00"
            minimumFractionDigits = 0 // Pastikan tidak ada desimal minimal
        }

    // Fungsi helper untuk konversi String ke BigDecimal dengan aman
    private fun getBigDecimalFromCurrencyInput(editText: TextInputEditText): BigDecimal {
        val cleanString = editText.text.toString()
            .replace("[Rp,.\\s]".toRegex(), "")
            .trim()
        return try {
            if (cleanString.isEmpty()) BigDecimal.ZERO
            else BigDecimal(cleanString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    private fun getBigDecimalFromWeightInput(editText: TextInputEditText): BigDecimal {
        val cleanString = editText.text.toString().trim()
        return try {
            if (cleanString.isEmpty()) BigDecimal.ZERO
            else BigDecimal(cleanString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    private fun formatBigDecimalToRupiahWithoutDecimal(value: BigDecimal): String {
        return numberFormatter.format(value.setScale(0, BigDecimal.ROUND_HALF_UP).toDouble())
    }

    private var userId: Int = 0
    private var userIdBranch: Int = 0

    // Menggunakan var untuk memungkinkan perubahan nilai
    private var currentNotes: String = ""
    // Ubah tipe data ini menjadi BigDecimal
    private var currentAdditionalCost: BigDecimal = BigDecimal.ZERO
    private var currentPromoAmount: BigDecimal = BigDecimal.ZERO

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

        setupSpinner()

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = userViewModel.getUserById(userId)
                userIdBranch = user.data.id_branch_user!!.toInt()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal mendapatkan data pengguna",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        laundryProductViewModel.selectedItems.observe(viewLifecycleOwner) { selected ->
            populateLinearLayout(selected)
            updateTotalPrices() // Panggil untuk memperbarui total saat item berubah
        }

        setupCurrencyInput(binding.textCash)

        binding.textCash.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalPrices() // Perbarui total harga dan kembalian
            }
        })

        laundryReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.success) {
                    showAlert(
                        title = "Berhasil!",
                        message = "Data berhasil disimpan.",
                        backgroundColorRes = R.color.primary,
                        iconRes = R.drawable.success
                    )
                    val bundle = Bundle()
                    bundle.putInt("transactionId", response.data.id_transaction_laundry!!)

                    val fragment = PrintPreviewFragment()
                    fragment.arguments = bundle

                    openUserFragment(fragment, "PrintPreviewLaundry")


                } else {
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Gagal membuat transaksi.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                laundryReportViewModel.clearCreateTransactionResponse()
            }
        }

        laundryReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                laundryReportViewModel.clearError()
            }
        }

        binding.btnSubmit.setOnClickListener {
            createLaundryTransaction()
        }

        binding.arrowBack.setOnClickListener {
            openUserFragment(UserDashboardFragment(), "UserDashboard")
            activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
        }

        binding.buttonOptional.setOnClickListener {
            showOptionalDialog(
                requireContext(),
                currentNotes,
                currentAdditionalCost,
                currentPromoAmount
            ) { notes, additionalCost, promoAmount ->
                currentNotes = notes
                currentAdditionalCost = additionalCost
                currentPromoAmount = promoAmount
                updateTotalPrices() // Panggil untuk memperbarui total setelah opsional diubah
            }
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

    // Fungsi baru untuk populate LinearLayout
    private fun populateLinearLayout(selectedItems: List<ProductLaundry>) {
        binding.linearLayoutContainer.removeAllViews()
        weightInputMap.clear()

        selectedItems.forEach { item ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.card_item_detail_transaction_laundry, binding.linearLayoutContainer, false)

            val textName = itemView.findViewById<TextView>(R.id.textName)
            val textPrice = itemView.findViewById<TextView>(R.id.textPrice)
            val weightInput = itemView.findViewById<TextInputEditText>(R.id.textWeightItem)

            textName.text = item.name_laundry_item
            textPrice.text = formatBigDecimalToRupiahWithoutDecimal(item.price_laundry_item ?: BigDecimal.ZERO)

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (weightInput.hasFocus()) {
                        val newWeight = getBigDecimalFromWeightInput(weightInput)
                        // Validasi: berat harus > 0 dan tidak kosong
                        if (weightInput.text.isNullOrEmpty() || newWeight <= BigDecimal.ZERO) {
                            weightInput.error = "Berat harus diisi dan lebih dari 0"
                            Toast.makeText(requireContext(), "Berat harus diisi dan lebih dari 0", Toast.LENGTH_SHORT).show()
                        } else {
                            weightInput.error = null
                        }
                        if (item.weight != newWeight) {
                            laundryProductViewModel.updateItemWeight(item.id_laundry_item ?: 0, newWeight)
                            updateTotalPrices()
                        }
                    }
                }
            }
            weightInput.addTextChangedListener(watcher)

            val expectedWeight = if (item.weight != null && item.weight != BigDecimal.ZERO) item.weight.toString() else ""
            if (!weightInput.hasFocus() && weightInput.text?.toString() != expectedWeight) {
                weightInput.removeTextChangedListener(watcher)
                weightInput.setText(expectedWeight)
                weightInput.addTextChangedListener(watcher)
            }

            weightInputMap[item.id_laundry_item!!] = weightInput

            binding.linearLayoutContainer.addView(itemView)
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item,
            StatusReportLaundry.entries.filter { it != StatusReportLaundry.completed }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusText.adapter = adapter
    }

    private fun updateTotalPrices() {
        var subtotal: BigDecimal = BigDecimal.ZERO
        laundryProductViewModel.selectedItems.value?.forEach { product ->
            val price = product.price_laundry_item ?: BigDecimal.ZERO
            val weight = product.weight ?: BigDecimal.ZERO
            subtotal = subtotal.add(price.multiply(weight))
        }

        val totalBeforeDiscount = subtotal.add(currentAdditionalCost)
        val finalTotal = totalBeforeDiscount.subtract(currentPromoAmount)

        binding.textTotalPrice.text = formatBigDecimalToRupiahWithoutDecimal(finalTotal)
        val cashInput = getBigDecimalFromCurrencyInput(binding.textCash)
        val returnAmount = cashInput.subtract(finalTotal)
        binding.textChangeMoney.text = formatBigDecimalToRupiahWithoutDecimal(returnAmount)
    }


    private fun createLaundryTransaction() {
        val nameClient = binding.textClient.text.toString().trim()
        if (nameClient.isEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Nama pelanggan harus diisi",
                backgroundColorRes = R.color.primary
            )
            binding.textClient.error = "Nama pelanggan harus diisi"
            return
        } else {
            binding.textClient.error = null
        }

        val priceCash = getBigDecimalFromCurrencyInput(binding.textCash)
        if (binding.textCash.text.toString().trim().isEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Uang tunai harus diisi",
                backgroundColorRes = R.color.primary
            )
            binding.textCash.error = "Uang tunai harus diisi"
            return
        }
        if (priceCash <= BigDecimal.ZERO) {
            showAlert(
                title = "Peringatan!",
                message = "Uang tunai harus lebih dari 0",
                backgroundColorRes = R.color.primary
            )
            binding.textCash.error = "Uang tunai harus lebih dari 0"
            return
        } else {
            binding.textCash.error = null
        }

        val selectedItems = laundryProductViewModel.selectedItems.value ?: emptyList()
        if (selectedItems.isEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Pilih setidaknya satu item laundry",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        // Validasi bahwa semua item memiliki berat
        val itemsWithoutWeight = selectedItems.filter { it.weight == null || it.weight == BigDecimal.ZERO }
        if (itemsWithoutWeight.isNotEmpty()) {
            showAlert(
                title = "Peringatan!",
                message = "Harap isi berat untuk semua item",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val statusTransaction = binding.statusText.selectedItem as? StatusReportLaundry
        if (statusTransaction == null) {
            showAlert(
                title = "Peringatan!",
                message = "Status transaksi harus dipilih",
                backgroundColorRes = R.color.primary,
                iconRes = R.drawable.info
            )
            return
        }

        val laundryTransactionItems = selectedItems.map { selectedItem ->
            ListTransactionLaundry(
                id_item_laundry = selectedItem.id_laundry_item,
                weight_list_transaction_laundry = selectedItem.weight,
                deleted_at = null
            )
        }

        // Hitung total harga dari item yang dipilih (subtotal)
        var totalItemPrice: BigDecimal = BigDecimal.ZERO
        selectedItems.forEach { product ->
            val price = product.price_laundry_item ?: BigDecimal.ZERO
            val weight = product.weight ?: BigDecimal.ZERO
            totalItemPrice = totalItemPrice.add(price.multiply(weight))
        }
        val finalTotalTransaction = totalItemPrice.add(currentAdditionalCost).subtract(currentPromoAmount)

        val returnAmount = priceCash.subtract(finalTotalTransaction)

        // Validasi uang tunai cukup
        if (returnAmount < BigDecimal.ZERO) {
            showAlert(
                title = "Peringatan!",
                message = "Uang tunai kurang dari total pembayaran",
                backgroundColorRes = R.color.primary,
            )
            binding.textCash.error = "Uang tunai kurang dari total pembayaran"
            return
        } else {
            binding.textCash.error = null
        }

        val laundryTransactionRequest = TransactionData(
            id_kurir_transaction_laundry = userId,
            id_branch_transaction_laundry = userIdBranch,
            name_client_transaction_laundry = nameClient,
            status_transaction_laundry = statusTransaction,
            count_item_laundry_transaction_laundry = selectedItems.size,
            promo_transaction_laundry = currentPromoAmount,
            additional_cost_transaction_laundry = currentAdditionalCost,
            cash_transaction_laundry = priceCash,
            notes_transaction_laundry = currentNotes,
            list_transaction_laundry = laundryTransactionItems,
            total_price_transaction_laundry = finalTotalTransaction,
            change_money_transaction_laundry = returnAmount,
            total_weight_transaction_laundry = selectedItems.sumOf { it.weight ?: BigDecimal.ZERO }.takeIf { it != BigDecimal.ZERO },
            total_transaction_laundry = finalTotalTransaction
        )

        laundryReportViewModel.createReportLaundry(laundryTransactionRequest)
    }

    fun showOptionalDialog(
        context: Context,
        initialNotes: String,
        initialAdditional: BigDecimal,
        initialPromo: BigDecimal,
        onSave: (notes: String, additionalCost: BigDecimal, promoAmount: BigDecimal) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_additional, null)

        val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotes)
        val editTextAdditionalCost = dialogView.findViewById<TextInputEditText>(R.id.editTextAdditionalCost)
        val editTextPromoAmount = dialogView.findViewById<TextInputEditText>(R.id.editTextPromoAmount)
        val buttonCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCancelDialog)
        val buttonSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonSaveDialog)

        editTextNotes.setText(initialNotes)
        editTextAdditionalCost.setText(formatBigDecimalToRupiahWithoutDecimal(initialAdditional))
        editTextPromoAmount.setText(formatBigDecimalToRupiahWithoutDecimal(initialPromo))

        setupCurrencyInput(editTextAdditionalCost)
        setupCurrencyInput(editTextPromoAmount)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonSave.setOnClickListener {
            val notes = editTextNotes.text.toString()
            val additionalCost = getBigDecimalFromCurrencyInput(editTextAdditionalCost)
            val promoAmount = getBigDecimalFromCurrencyInput(editTextPromoAmount)

            // Validasi biaya tambahan dan promo tidak negatif
            if (additionalCost < BigDecimal.ZERO) {
                editTextAdditionalCost.error = "Biaya tambahan tidak boleh negatif"
                return@setOnClickListener
            } else {
                editTextAdditionalCost.error = null
            }
            if (promoAmount < BigDecimal.ZERO) {
                editTextPromoAmount.error = "Promo tidak boleh negatif"
                return@setOnClickListener
            } else {
                editTextPromoAmount.error = null
            }

            onSave.invoke(notes, additionalCost, promoAmount)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun setupCurrencyInput(editText: TextInputEditText) {
        var current = ""
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editText.removeTextChangedListener(this)

                    val cleanString = s.toString()
                        .replace("[Rp,.\\s]".toRegex(), "")

                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = BigDecimal(cleanString)
                            val formatted = numberFormatter.format(parsed.toDouble())

                            current = formatted
                            editText.setText(formatted)
                            editText.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                        }
                    } else {
                        current = ""
                        editText.setText("") // Hapus teks jika input kosong
                    }
                    editText.addTextChangedListener(this)
                }
            }
        })
    }
}