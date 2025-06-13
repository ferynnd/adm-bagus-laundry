//package dev.ferynnd.baguslaundry.ui.user
//
//import android.content.Context
//import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ArrayAdapter
//import android.widget.Toast
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import com.google.android.material.textfield.TextInputEditText
//import dev.ferynnd.baguslaundry.R
//import dev.ferynnd.baguslaundry.controller.user.LaundryTransactionMenuAdapter
//import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
//import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
//import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
//import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
//import dev.ferynnd.baguslaundry.databinding.FragmentLaundryTransactionMenuBinding
//import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
//import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
//import dev.ferynnd.baguslaundry.model.ProductLaundry
//import dev.ferynnd.baguslaundry.model.StatusReportLaundry
//import dev.ferynnd.baguslaundry.model.TransactionData
//import kotlinx.coroutines.launch
//import java.text.NumberFormat
//import java.util.Locale
//
//class LaundryTransactionMenuFragment : Fragment() {
//
//
//    private lateinit var binding: FragmentLaundryTransactionMenuBinding
//    private lateinit var laundryProductViewModel: LaundryProductViewModel
//    private lateinit var laundryReportViewModel: LaundryReportViewModel
//    private lateinit var laundryTransactionMenuAdapter: LaundryTransactionMenuAdapter
//    private lateinit var userViewModel: UserViewModel
//    private lateinit var sharePrefrences: SharePrefrenceHelper
//
//    private var userId: Int = 0
//    private var userIdBranch: Int = 0
//
//    private var currentNotes: String = ""
//    private var currentAdditionalCost: Double = 0.0
//    private var currentPromoAmount: Double = 0.0
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        laundryProductViewModel =
//            ViewModelProvider(requireActivity())[LaundryProductViewModel::class.java]
//        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
//        laundryReportViewModel =
//            ViewModelProvider(requireActivity())[LaundryReportViewModel::class.java]
//        laundryReportViewModel.init(requireContext())
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding = FragmentLaundryTransactionMenuBinding.inflate(inflater, container, false)
//        hideBottomNavigationView()
//
//        setupSpinner()
//
//        sharePrefrences = SharePrefrenceHelper(requireContext())
//        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                val user = userViewModel.getUserById(userId)
//                userIdBranch = user.data.id_branch_user!!.toInt()
//            } catch (e: Exception) {
//                throw e
//            }
//        }
//
//        laundryTransactionMenuAdapter = LaundryTransactionMenuAdapter()
//        binding.recyclerView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = laundryTransactionMenuAdapter
//        }
//
//        laundryProductViewModel.selectedItems.observe(viewLifecycleOwner) { selected ->
//            laundryTransactionMenuAdapter.submitList(selected)
//        }
//
//        laundryReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
//            if (response != null) { // Pastikan response tidak null
//                if (response.success) {
//                    Toast.makeText(requireContext(), "Data Berhasil Disimpan", Toast.LENGTH_SHORT)
//                        .show()
//                    // Navigasi setelah sukses
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.host_fragment_user, UserDashboardFragment())
//                        .commit()
//                } else {
//                    // Tampilkan pesan error dari backend
//                    Log.d("CreateTransaction", "Error: ${response.message}")
//                    Toast.makeText(
//                        requireContext(),
//                        response.message ?: "Gagal membuat transaksi.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//                laundryReportViewModel.clearCreateTransactionResponse()
//            }
//        }
//
//        laundryReportViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
//            if (errorMessage.isNotEmpty()) {
//                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
//                Log.d("CreateTransaction", "Error: $errorMessage")
//                laundryReportViewModel.clearError()
//            }
//        }
//
//        binding.btnSubmit.setOnClickListener {
//            createLaundryTransaction(currentNotes, currentAdditionalCost, currentPromoAmount)
//        }
//
//        binding.buttonOptional.setOnClickListener {
//            showOptionalDialog(
//                requireContext(),
//                currentNotes,
//                currentAdditionalCost,
//                currentPromoAmount
//            ) { notes, additionalCost, promoAmount ->
//                currentNotes = notes
//                currentAdditionalCost = additionalCost
//                currentPromoAmount = promoAmount
//
//                createLaundryTransaction(notes, additionalCost, promoAmount)
//            }
//        }
//
//
//        return binding.root
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        laundryProductViewModel.clearSelectedItems()
//        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
//    }
//
//
//    private fun hideBottomNavigationView() {
//        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.GONE
//    }
//
//    private fun setupSpinner() {
//        val adapter = ArrayAdapter(
//            requireContext(), android.R.layout.simple_spinner_item,
//            StatusReportLaundry.entries.filter { it != StatusReportLaundry.completed }
//        )
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.statusText.adapter = adapter
//    }
//
//
//    private fun createLaundryTransaction(
//        notes: String,
//        additionalCost: Double,
//        promoAmount: Double
//    ) {
//
//        val nameClient = binding.textClient.text.toString().trim()
//
//
//        if (nameClient.isEmpty()) {
//            Toast.makeText(
//                requireContext(), // Menggunakan 'this' karena ini di Activity, jika di Fragment gunakan requireContext()
//                "Nama pelanggan dan uang tunai harus diisi",
//                Toast.LENGTH_SHORT
//            ).show()
//            return // Keluar dari fungsi jika validasi gagal
//        }
//
//        val priceCash = setupCurrencyFormatter(binding.textPrice as TextInputEditText)
//        if (priceCash == null) {
//            Toast.makeText(requireContext(), "Format uang tunai tidak valid", Toast.LENGTH_SHORT)
//                .show()
//            return // Keluar dari fungsi jika validasi gagal
//        }
//
//        val selectedItems = laundryProductViewModel.selectedItems.value ?: emptyList()
//        val statusTransaction = binding.statusText.selectedItem as? StatusReportLaundry
//
//        val laundryTransactionItems = selectedItems.map { selectedItem ->
//            ListTransactionLaundry(
//                id_item_laundry = selectedItem.id_laundry_item,
//                weight_list_transaction_laundry = selectedItem.weight?.toDouble(),
//                deleted_at = null
//            )
//        }
//
//        // Hitung total harga terlebih dahulu
//        val totalPrice = selectedItems.sumOf {
//            it.price_laundry_item?.times(it.weight?.toDouble() ?: 0.0) ?: 0.0
//        }
//        val allCost = (totalPrice + additionalCost) - promoAmount
//        binding.textTotalPrice.text = allCost.toString()
//
//        // Sekarang hitung kembalian dengan aman
//        val changeMoney = priceCash - allCost
//        binding.textChangeMoney.text = changeMoney.toString()
//
//
//        val laundryTransactionRequest = TransactionData(
//            id_kurir_transaction_laundry = userId,
//            id_branch_transaction_laundry = userIdBranch,
//            name_client_transaction_laundry = nameClient,
//            status_transaction_laundry = statusTransaction,
//            count_item_laundry_transaction_laundry = selectedItems.size,
//            promo_transaction_laundry = promoAmount,
//            additional_cost_transaction_laundry = additionalCost,
//            cash_transaction_laundry = priceCash,
//            notes_transaction_laundry = notes,
//            list_transaction_laundry = laundryTransactionItems
//        )
//
//        laundryReportViewModel.createReportLaundry(laundryTransactionRequest)
//
//    }
//
//
//    fun showOptionalDialog(
//        context: Context,
//        initialNotes: String,
//        initialAdditional: Double,
//        initialPromo: Double,
//        onSave: (notes: String, additionalCost: Double, promoAmount: Double) -> Unit
//    ) {
//        // Inflate layout custom dialog dari XML
//        val dialogView =
//            LayoutInflater.from(context).inflate(R.layout.dialog_additional, null)
//
//        // Inisialisasi komponen UI dari layout dialog
//        val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotes)
//        val editTextAdditionalCost = setupCurrencyFormatter(dialogView.findViewById<TextInputEditText>(R.id.editTextAdditionalCost)  as TextInputEditText)
//        val editTextPromoAmount =setupCurrencyFormatter(dialogView.findViewById<TextInputEditText>(R.id.editTextPromoAmount) as TextInputEditText)
//        val buttonCancel =
//            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCancelDialog)
//        val buttonSave =
//            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonSaveDialog)
//
//        // Isi data awal ke dalam input field
//        editTextNotes.setText(initialNotes)
//        // Pastikan nilai double diubah menjadi string sebelum diset ke EditText
//        editTextAdditionalCost.setText(initialAdditional.toString())
//        editTextPromoAmount.setText(initialPromo.toString())
//
//        // Buat dialog menggunakan MaterialAlertDialogBuilder untuk tampilan yang modern
//        val dialog = MaterialAlertDialogBuilder(context)
//            .setView(dialogView)
//            .setCancelable(false) // Mencegah dialog tertutup saat klik di luar area dialog
//            .create()
//
//        // Atur listener untuk tombol "Batal"
//        buttonCancel.setOnClickListener {
//            dialog.dismiss() // Tutup dialog
//        }
//
//        // Atur listener untuk tombol "Simpan"
//        buttonSave.setOnClickListener {
//            // Ambil nilai dari input field
//            val notes = editTextNotes.text.toString()
//            val additionalCost = editTextAdditionalCost.text.toString().toDoubleOrNull() ?: 0.0
//            val promoAmount = editTextPromoAmount.text.toString().toDoubleOrNull() ?: 0.0
//
//            // Panggil callback onSave dengan data yang sudah diambil
//            onSave.invoke(notes, additionalCost, promoAmount)
//
//            dialog.dismiss() // Tutup dialog setelah data disimpan
//        }
//
//        // Tampilkan dialog
//        dialog.show()
//    }
//
//    fun setupCurrencyFormatter(editText: TextInputEditText) {
//        editText.addTextChangedListener(object : TextWatcher {
//            private var current = ""
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//            override fun afterTextChanged(s: Editable?) {
//                if (s.toString() != current) {
//                    editText.removeTextChangedListener(this)
//
//                    val cleanString = s.toString().replace("[Rp,.\\s]".toRegex(), "")
//                    if (cleanString.isNotEmpty()) {
//                        try {
//                            val parsed = cleanString.toDouble()
//                            val formatted =
//                                NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(parsed)
//                            current = formatted
//                            editText.setText(formatted)
//                            editText.setSelection(formatted.length)
//                        } catch (e: NumberFormatException) {
//                            e.printStackTrace()
//                        }
//                    }
//
//                    editText.addTextChangedListener(this)
//                }
//            }
//        })
//    }
//
//
//}
//

package dev.ferynnd.baguslaundry.ui.user

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.ListTransactionLaundry
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import dev.ferynnd.baguslaundry.model.TransactionData
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class LaundryTransactionMenuFragment : Fragment() {


    private lateinit var binding: FragmentLaundryTransactionMenuBinding
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var laundryTransactionMenuAdapter: LaundryTransactionMenuAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private val numberFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true // Untuk pemisah ribuan (titik)
            maximumFractionDigits = 0 // Ini yang menghilangkan ",00"
            minimumFractionDigits = 0 // Pastikan tidak ada desimal minimal
        }

    private var userId: Int = 0
    private var userIdBranch: Int = 0

    // Menggunakan var untuk memungkinkan perubahan nilai
    private var currentNotes: String = ""
    private var currentAdditionalCost: Double = 0.0
    private var currentPromoAmount: Double = 0.0


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
                Log.e("LaundryTransactionMenuFragment", "Error fetching user data: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Gagal mendapatkan data pengguna",
                    Toast.LENGTH_SHORT
                ).show()
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

        // Apply currency formatter to the cash input field
        setupCurrencyInput(binding.textCash)

        binding.textCash.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                null
            }
        })


        laundryReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.success) {
                    Toast.makeText(requireContext(), "Data Berhasil Disimpan", Toast.LENGTH_SHORT)
                        .show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, UserDashboardFragment())
                        .commit()
                } else {
                    Log.d("CreateTransaction", "Error: ${response.message}")
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
                Log.d("CreateTransaction", "Error: $errorMessage")
                laundryReportViewModel.clearError()
            }
        }

        binding.btnSubmit.setOnClickListener {
            createLaundryTransaction()
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
            }
        }

        // Initial update for total price and change money
//        updateTransactionSummary()

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

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item,
            StatusReportLaundry.entries.filter { it != StatusReportLaundry.completed }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusText.adapter = adapter
    }

    // Fungsi untuk mendapatkan nilai Double dari TextInputEditText yang diformat mata uang
//    private fun getDoubleFromCurrencyInput(editText: TextInputEditText): Double {
//        val cleanString = editText.text.toString().replace("[Rp,.\\s]".toRegex(), "")
//        return cleanString.toDoubleOrNull() ?: 0.0
//    }
    private fun getDoubleFromCurrencyInput(editText: TextInputEditText): Double {
        // Hapus semua karakter non-digit dan non-koma dari string
        // Kita anggap koma desimal adalah ',' sesuai Locale ID, tapi karena kita tidak pakai desimal
        // kita hanya fokus menghapus simbol mata uang dan pemisah ribuan.
        val cleanString = editText.text.toString()
            .replace("Rp", "") // Hapus simbol Rupiah
            .replace(".", "") // Hapus pemisah ribuan (titik)
            .trim()

        return cleanString.toDoubleOrNull() ?: 0.0
    }

    private fun formatToRupiahWithoutDecimal(value: Double): String {
        // Gunakan numberFormatter untuk formatting angka tanpa desimal
        val formattedNumber = numberFormatter.format(value)
        // Tambahkan simbol Rupiah di depannya
        return formattedNumber
    }

    private fun createLaundryTransaction() {
        val nameClient = binding.textClient.text.toString().trim()

        if (nameClient.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Nama pelanggan harus diisi",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val priceCash = getDoubleFromCurrencyInput(binding.textCash)
        if (priceCash == 0.0 && binding.textCash.text.toString().trim()
                .isEmpty()
        ) { // Cek juga jika input kosong
            Toast.makeText(
                requireContext(),
                "Uang tunai harus diisi",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val selectedItems = laundryProductViewModel.selectedItems.value ?: emptyList()
        if (selectedItems.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Pilih setidaknya satu item laundry",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val statusTransaction = binding.statusText.selectedItem as? StatusReportLaundry

        val laundryTransactionItems = selectedItems.map { selectedItem ->
            ListTransactionLaundry(
                id_item_laundry = selectedItem.id_laundry_item,
                weight_list_transaction_laundry = selectedItem.weight?.toDouble(),
                deleted_at = null
            )
        }

        // Dapatkan total harga dari tampilan yang sudah diupdate
        val totalCostFromView = binding.textTotalPrice.toString()

        val laundryTransactionRequest = TransactionData(
            id_kurir_transaction_laundry = userId,
            id_branch_transaction_laundry = userIdBranch,
            name_client_transaction_laundry = nameClient,
            status_transaction_laundry = statusTransaction,
            count_item_laundry_transaction_laundry = selectedItems.size,
            promo_transaction_laundry = currentPromoAmount, // Gunakan currentPromoAmount
            additional_cost_transaction_laundry = currentAdditionalCost, // Gunakan currentAdditionalCost
            cash_transaction_laundry = priceCash,
            notes_transaction_laundry = currentNotes, // Gunakan currentNotes
            list_transaction_laundry = laundryTransactionItems
        )

        laundryReportViewModel.createReportLaundry(laundryTransactionRequest)
    }


    fun showOptionalDialog(
        context: Context,
        initialNotes: String,
        initialAdditional: Double,
        initialPromo: Double,
        onSave: (notes: String, additionalCost: Double, promoAmount: Double) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_additional, null)

        val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotes)
        val editTextAdditionalCost =
            dialogView.findViewById<TextInputEditText>(R.id.editTextAdditionalCost)
        val editTextPromoAmount =
            dialogView.findViewById<TextInputEditText>(R.id.editTextPromoAmount)
        val buttonCancel =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCancelDialog)
        val buttonSave =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonSaveDialog)

        editTextNotes.setText(initialNotes)
        editTextAdditionalCost.setText(numberFormatter.format(initialAdditional))
        editTextPromoAmount.setText(numberFormatter.format(initialPromo))

        // Terapkan formatter mata uang ke input di dialog
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
            val additionalCost = getDoubleFromCurrencyInput(editTextAdditionalCost)
            val promoAmount = getDoubleFromCurrencyInput(editTextPromoAmount)

            onSave.invoke(notes, additionalCost, promoAmount)
            dialog.dismiss()
        }

        dialog.show()
    }

    // Fungsi yang lebih baik untuk mengatur input mata uang
    private fun setupCurrencyInput(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var currentFormattedValue = ""
            private var isFormatting = false
            private var isDeleting = false // Flag untuk mendeteksi penghapusan

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Jika count > 0 dan after == 0, berarti terjadi penghapusan
                isDeleting = count > 0 && after == 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) {
                    return
                }

                isFormatting = true

                try {
                    val rawInput = s.toString()
                    // Hapus semua karakter non-digit kecuali jika Anda ingin mendukung input koma untuk desimal
                    // Karena Anda tidak ingin ",00", kita hanya fokus pada digit
                    val cleanInput = rawInput.replace("[^\\d]".toRegex(), "")

                    if (cleanInput.isNotEmpty()) {
                        val parsedValue = cleanInput.toDouble()

                        // Format ulang nilai
                        val newFormattedValue = formatToRupiahWithoutDecimal(parsedValue)

                        if (newFormattedValue != currentFormattedValue) {
                            currentFormattedValue = newFormattedValue
                            editText.setText(newFormattedValue)

                            // Set posisi kursor di akhir
                            editText.setSelection(newFormattedValue.length)

                        } else if (rawInput.isEmpty() && newFormattedValue.isNotEmpty()) {
                            // Kasus khusus: jika input kosong tapi formattedValue tidak kosong (misal awalnya 0, terus dihapus)
                            editText.setText("")
                        }
                    } else {
                        currentFormattedValue = ""
                        editText.setText("") // Kosongkan jika tidak ada input
                    }
                } catch (e: NumberFormatException) {
                    Log.e("CurrencyFormatter", "Invalid number format: ${s.toString()}", e)
                } finally {
                    isFormatting = false
                }
            }
        })
    }
}