package dev.ferynnd.baguslaundry.ui.user.transaksi_laundry

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentCreateListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.LaundryTransactionItem
import dev.ferynnd.baguslaundry.model.LaundryTransactionRequest
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CreateListTransaksiLaundryFragment : Fragment() {

    private var _binding: KurirFragmentCreateListTransaksiLaundryBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var laundryReportViewModel: LaundryReportViewModel
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var userId: Int = 0
    private var userIdBranch: Int = 0

    private var fullLaundryList: List<ProductLaundry> = listOf()
    private var laundryTransactionItems: MutableList<LaundryTransactionItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        laundryProductViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java]
        laundryReportViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java]
        laundryProductViewModel.init(requireContext())
        laundryReportViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            KurirFragmentCreateListTransaksiLaundryBinding.inflate(inflater, container, false)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        setupViews()
        setupObservers()
        loadUserDataAndProducts()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTextWatchers()

        if (binding.containerLaundry.childCount > 0) {
            updateTotalAmountToPay()
        }
    }

    private fun loadUserDataAndProducts() {
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val userResponse = userViewModel.getUserById(userId)
                    if (userResponse.success && userResponse.data != null) {
                        userIdBranch = userResponse.data.id_branch_user!!.toInt()

                        binding.yangMenangani.text = userResponse.data.fullname_user

                        laundryProductViewModel.getProductLaundry()
                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat data pengguna", Toast.LENGTH_LONG).show()
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
            Toast.makeText(context, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViews() {
        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, UserDashboardFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnAddItem.setOnClickListener {
            if (fullLaundryList.isNotEmpty()) {
                addLaundryItemInput()
                updateTotalPrice()
            } else {
                Toast.makeText(requireContext(), "Data laundry kosong untuk cabang ini", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (validateInputs() && validateTransactionInputs()) {
                processTransaction()
            }
        }
    }

    private fun setupObservers() {
        laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { result ->
            result?.let { allProducts ->
                Log.d("LaundryFragment", "All products: ${allProducts.size}, filtering for branch: $userIdBranch")

                fullLaundryList = allProducts.filter { item ->
                    val branchMatch = item.id_branch_laundry_item == userIdBranch
                    Log.d("LaundryFragment", "Item branch: ${item.id_branch_laundry_item}, match: $branchMatch")
                    branchMatch
                }

                if (binding.containerLaundry.childCount == 0) {
                    addLaundryItemInput()
                } else {
                    refreshAllLaundryInputs()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Gagal memuat data laundry", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        laundryReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.success) {
                    Toast.makeText(
                        requireContext(),
                        "Transaksi berhasil dibuat",
                        Toast.LENGTH_LONG
                    ).show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, ListTransaksiLaundryFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Gagal membuat transaksi: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                laundryReportViewModel.resetCreateTransactionResponse()
            }
        }
    }

    private fun refreshAllLaundryInputs() {
        binding.containerLaundry.removeAllViews()

        if (fullLaundryList.isNotEmpty()) {
            addLaundryItemInput()
        }
        updateTotalPrice()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addLaundryItemInput(index: Int = 0) {

        val itemView = layoutInflater.inflate(
            R.layout.kurir_list_item_transaksi_laundry_input,
            binding.containerLaundry,
            false
        )

        val spinner = itemView.findViewById<Spinner>(R.id.spinnerLaundry)
        val beratEditText = itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_laundry)
        val hargaText = itemView.findViewById<TextView>(R.id.harga_list_transaksi_laundry)
        val totalText = itemView.findViewById<TextView>(R.id.total_harga_list_transaksi_laundry)
        val noteText = itemView.findViewById<EditText>(R.id.input_note_list_transaksi_laundry)
        val trashIcon = itemView.findViewById<ImageView>(R.id.trash_create_list_transaksi)

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.kurir_spinner_item_laundry,
            fullLaundryList.map { "${it.name_laundry_item} - ${it.time_laundry_item}" }
        )
        adapter.setDropDownViewResource(R.layout.kurir_spinner_dropdown_item_laundry)
        spinner.adapter = adapter

        val validIndex = if (index in 0 until fullLaundryList.size) index else 0
        spinner.setSelection(validIndex)

        val initialSelection = fullLaundryList.getOrNull(validIndex)
        val initialPrice = initialSelection?.price_laundry_item?.toDouble() ?: 0.0

        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        hargaText.text = numberFormat.format(initialPrice)

        trashIcon.setOnClickListener {
            binding.containerLaundry.removeView(itemView)
            updateTotalPrice()
            if (binding.containerLaundry.childCount == 0) {
                Toast.makeText(requireContext(), "Semua item dihapus", Toast.LENGTH_SHORT).show()
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position in 0 until fullLaundryList.size) {
                    val selected = fullLaundryList[position]
                    val hargaItem = selected.price_laundry_item?.toDouble() ?: 0.0
                    hargaText.text = numberFormat.format(hargaItem)

                    val berat = beratEditText.text.toString().toDoubleOrNull() ?: 0.0
                    val total = berat * hargaItem
                    totalText.text = numberFormat.format(total)

                    updateTotalPrice()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        beratEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable?) {
                val berat = editable?.toString()?.toDoubleOrNull() ?: 0.0
                val position = spinner.selectedItemPosition
                if (position in 0 until fullLaundryList.size) {
                    val selected = fullLaundryList[position]
                    val hargaItem = selected.price_laundry_item?.toDouble() ?: 0.0
                    val total = berat * hargaItem
                    totalText.text = numberFormat.format(total)
                    updateTotalPrice()
                }
            }
        })

        binding.containerLaundry.addView(itemView)
    }

    private fun updateTotalPrice() {
        var totalBerat = 0.0
        var totalPrice = 0.0

        for (i in 0 until binding.containerLaundry.childCount) {
            val itemView = binding.containerLaundry.getChildAt(i)
            val spinner = itemView.findViewById<Spinner>(R.id.spinnerLaundry)
            val beratEditText =
                itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_laundry)

            val position = spinner.selectedItemPosition
            val selected = fullLaundryList.getOrNull(position)
            val hargaItem = selected?.price_laundry_item?.toDouble() ?: 0.0
            val berat = beratEditText.text.toString().toDoubleOrNull() ?: 0.0

            totalBerat += berat
            totalPrice += berat * hargaItem
        }

        binding.totalItem.text = binding.containerLaundry.childCount.toString()
        binding.totalBerat.text = String.format("%.2f Kg", totalBerat)
        binding.totalPrice.text = formatToRupiah(totalPrice)

        updateTotalAmountToPay()
    }

    private fun validateInputs(): Boolean {
        if (binding.containerLaundry.childCount == 0) {
            Toast.makeText(
                requireContext(),
                "Tambahkan minimal satu item laundry",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        for (i in 0 until binding.containerLaundry.childCount) {
            val itemView = binding.containerLaundry.getChildAt(i)
            val beratEditText =
                itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_laundry)

            val berat = beratEditText.text.toString().toDoubleOrNull() ?: 0.0
            if (berat <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Berat laundry harus lebih dari 0 kg",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        }
        return true
    }

    private fun validateTransactionInputs(): Boolean {
        if (binding.inputNamaClient.text.isNullOrEmpty()) {
            binding.inputNamaClient.error = "Nama klien tidak boleh kosong"
            return false
        }
        return true
    }

    private fun setupTextWatchers() {
        binding.inputPromo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalAmountToPay()
            }
        })

        binding.inputBiayaTambahan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalAmountToPay()
            }
        })

        binding.inputUangTunai.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateChange()
            }
        })
    }

    private fun updateTotalAmountToPay() {

        val basePrice = extractNumericValue(binding.totalPrice.text.toString())
        val promo = binding.inputPromo.text.toString().toDoubleOrNull() ?: 0.0
        val additionalCost = binding.inputBiayaTambahan.text.toString().toDoubleOrNull() ?: 0.0
        val totalAmountToPay = (basePrice - promo) + additionalCost
        binding.totalHargaKeseluruhan.text = formatToRupiah(totalAmountToPay)

        updateChange()
    }

    private fun updateChange() {

        val totalAmountToPay = extractNumericValue(binding.totalHargaKeseluruhan.text.toString())
        val cash = binding.inputUangTunai.text.toString().toDoubleOrNull() ?: 0.0
        val change = cash - totalAmountToPay

        binding.uangKembalian.text = formatToRupiah(change)
    }

    private fun extractNumericValue(text: String): Double {
        val cleanText = text.replace("Rp", "")
            .replace(".", "")
            .replace(",00", "")
            .replace("\\s".toRegex(), "") // menghapus spasi
        return cleanText.toIntOrNull()?.toDouble() ?: 0.0
    }

    private fun formatToRupiah(amount: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(amount).replace(",00", "")
    }

    private fun processTransaction() {
        laundryTransactionItems.clear()

        if (binding.containerLaundry.childCount == 0) {
            Toast.makeText(requireContext(), "Silahkan tambahkan item laundry terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        for (i in 0 until binding.containerLaundry.childCount) {
            val itemView = binding.containerLaundry.getChildAt(i)
            val spinner = itemView.findViewById<Spinner>(R.id.spinnerLaundry)
            val beratEditText = itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_laundry)
            val noteEditText = itemView.findViewById<EditText>(R.id.input_note_list_transaksi_laundry)

            val position = spinner.selectedItemPosition
            if (position < 0 || position >= fullLaundryList.size) {
                Toast.makeText(requireContext(), "Ada kesalahan pada pemilihan item laundry", Toast.LENGTH_SHORT).show()
                return
            }

            val berat = beratEditText.text.toString().toDoubleOrNull()
            if (berat == null || berat <= 0) {
                Toast.makeText(requireContext(), "Berat item laundry harus diisi dengan benar", Toast.LENGTH_SHORT).show()
                return
            }

            val selected = fullLaundryList[position]
            val transactionItem = LaundryTransactionItem(
                id_item_laundry = selected.id_laundry_item,
                weight_list_transaction_laundry = berat,
                note_list_transaction_laundry = noteEditText.text.toString()
            )
            laundryTransactionItems.add(transactionItem)
        }

        val promo = binding.inputPromo.text.toString().toDoubleOrNull() ?: 0.0
        val additionalCost = binding.inputBiayaTambahan.text.toString().toDoubleOrNull() ?: 0.0
        val cash = binding.inputUangTunai.text.toString().toDoubleOrNull() ?: 0.0

        val totalAmountToPay = extractNumericValue(binding.totalHargaKeseluruhan.text.toString())

        if (cash < totalAmountToPay) {
            Toast.makeText(requireContext(), "Uang tunai tidak mencukupi", Toast.LENGTH_SHORT).show()
            return
        }

        val laundryTransactionRequest = LaundryTransactionRequest(
            id_user_transaction_laundry = userId,
            id_branch_transaction_laundry = userIdBranch,
            name_client_transaction_laundry = binding.inputNamaClient.text.toString(),
            notes_transaction_laundry = binding.inputCatatan.text.toString(),
            promo_transaction_laundry = promo,
            additional_cost_transaction_laundry = additionalCost,
            cash_transaction_laundry = cash,
            list_transaction_laundry = laundryTransactionItems
        )

        // Send the transaction request
        laundryReportViewModel.createReportLaundry(laundryTransactionRequest)
    }
}