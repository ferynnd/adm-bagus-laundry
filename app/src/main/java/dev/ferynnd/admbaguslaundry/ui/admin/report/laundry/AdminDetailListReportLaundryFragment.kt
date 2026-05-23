package dev.ferynnd.admbaguslaundry.ui.admin.report.laundry

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.admbaguslaundry.R
import dev.ferynnd.admbaguslaundry.controller.DetailLaundryReportAdapter
import dev.ferynnd.admbaguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.admbaguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.admbaguslaundry.databinding.FragmentAdminDetailListReportLaundryBinding
import dev.ferynnd.admbaguslaundry.model.Branch
import dev.ferynnd.admbaguslaundry.model.StatusReportLaundry
import dev.ferynnd.admbaguslaundry.model.UpdateLaundryFullRequest
import dev.ferynnd.admbaguslaundry.model.UpdateLaundryItem
import dev.ferynnd.admbaguslaundry.model.UpdateLaundryTransactionData
import dev.ferynnd.admbaguslaundry.model.User
import dev.ferynnd.admbaguslaundry.ui.showAlert
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class AdminDetailListReportLaundryFragment : Fragment() {

    private lateinit var binding: FragmentAdminDetailListReportLaundryBinding
    private lateinit var reportLaundryViewModel: LaundryReportViewModel
    private lateinit var detailLaundryReportAdapter: DetailLaundryReportAdapter
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var branchViewModel: BranchViewModel
    private lateinit var userViewModel: UserViewModel

    private var branches: List<Branch> = emptyList()
    private var users: List<User> = emptyList()
    private var transactionReportID: Int? = null

    companion object {
        private const val TAG = "DETAIL_LAUNDRY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reportLaundryViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java]
        reportLaundryViewModel.init(requireContext())

        laundryProductViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java]
        laundryProductViewModel.init(requireContext())

        branchViewModel = ViewModelProvider(this)[BranchViewModel::class.java]
        branchViewModel.init(requireContext())

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAdminDetailListReportLaundryBinding.inflate(inflater, container, false)

        detailLaundryReportAdapter = DetailLaundryReportAdapter()
        transactionReportID = arguments?.getInt("transactionLaundryID")

        binding.recyclerViewListItem.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detailLaundryReportAdapter
            isNestedScrollingEnabled = false
        }

        setupObservers()
        loadDetail()

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.buttonEdit.setOnClickListener {
            showEditDialog()
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        return binding.root
    }

    private fun setupObservers() {
        branchViewModel.branches.observe(viewLifecycleOwner) { branchList ->
            branches = branchList
        }

        userViewModel.users.observe(viewLifecycleOwner) { userList ->
            users = userList
        }

        laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { products ->
            detailLaundryReportAdapter.setProductLaundry(products)
        }

        reportLaundryViewModel.deleteTransactionResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.success) {
                    showAlert(
                        title = "Berhasil",
                        message = it.message ?: "Transaksi berhasil dihapus",
                    )
                    parentFragmentManager.popBackStack()
                } else {
                    showAlert(
                        title = "Gagal",
                        message = it.message ?: "Gagal menghapus transaksi"
                    )
                }

                reportLaundryViewModel.resetDeleteTransactionResponse()
            }
        }

        reportLaundryViewModel.error.observe(viewLifecycleOwner) { error ->

            if (error.isNotBlank()) {

                showAlert(
                    title = "Terjadi Kesalahan",
                    message = error
                )

                reportLaundryViewModel.resetErrorMessage()
            }
        }
    }

    private fun loadDetail() {
        val id = transactionReportID

        if (id == null || id == 0) {
            Toast.makeText(requireContext(), "Detail tidak bisa dimuat", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = reportLaundryViewModel.getReportLaundryById(id)

                if (!response.success || response.data == null) {
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val dataReport = response.data

                Log.d(TAG, "TRANSACTION ID : ${dataReport.id_transaction_laundry}")
                Log.d(TAG, "ITEM SIZE : ${dataReport.list_transaction_laundry?.size ?: 0}")

                dataReport.list_transaction_laundry?.forEach {
                    Log.d(TAG, "ITEM -> ID=${it.id_item_laundry}, WEIGHT=${it.weight_list_transaction_laundry}")
                }

                Log.d("DETAIL_LAUNDRY", "RAW DETAIL : $dataReport")
                Log.d("DETAIL_LAUNDRY", "LIST NULL? : ${dataReport.list_transaction_laundry == null}")
                Log.d("DETAIL_LAUNDRY", "LIST SIZE : ${dataReport.list_transaction_laundry?.size ?: 0}")

                detailLaundryReportAdapter.submitList(dataReport.list_transaction_laundry ?: emptyList())

                val localeID = Locale("in", "ID")
                val formatRupiah = NumberFormat.getCurrencyInstance(localeID)

                val branchName = branches.find {
                    it.id_branch == dataReport.id_branch_transaction_laundry
                }?.name_branch ?: dataReport.id_branch_transaction_laundry.toString()

                val employeeName = users.find {
                    it.id_user == dataReport.id_kurir_transaction_laundry
                }?.fullname_user ?: dataReport.id_kurir_transaction_laundry.toString()

                val dataStatus = when (dataReport.status_transaction_laundry) {
                    StatusReportLaundry.paid -> "Sudah Bayar"
                    StatusReportLaundry.unpaid -> "Belum Bayar"
                    StatusReportLaundry.completed -> "Selesai"
                    StatusReportLaundry.cancelled -> "Dibatalkan"
                }

                binding.apply {
                    idUser.text = dataReport.number_transaction_laundry.toString()
                    inputEmployment.text = employeeName
                    inputBranch.text = branchName
                    inputCustommer.text = dataReport.name_client_transaction_laundry ?: "-"
                    inputNotes.text = dataReport.notes_transaction_laundry ?: "-"
                    inputWeight.text = "${dataReport.total_weight_transaction_laundry ?: 0} Kg"
                    inputCountItem.text = dataReport.count_item_transaction_laundry.toString()
                    inputCash.text = formatRupiah.format(dataReport.cash_transaction_laundry ?: 0)
                    inputTotalPrice.text = formatRupiah.format(dataReport.total_price_transaction_laundry ?: 0)
                    inputTotalPriceTransaction.text = formatRupiah.format(dataReport.total_transaction_laundry ?: 0)
                    inputChangeMoney.text = formatRupiah.format(dataReport.change_money_transaction_laundry ?: 0)
                    inputStatus.text = dataStatus
                    inputTimeIn.text = dataReport.first_date_transaction_laundry ?: "-"
                    inputTimeOut.text = dataReport.last_date_transaction_laundry ?: "-"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error load detail laundry", e)
                Toast.makeText(requireContext(), "Gagal memuat detail: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun showEditDialog() {
    val id = transactionReportID ?: return

    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val response = reportLaundryViewModel.getReportLaundryById(id)
            val data = response.data ?: return@launch

            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_edit_laundry)

            dialog.window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            val inputCustomer = dialog.findViewById<TextInputEditText>(R.id.inputCustomer)
            val inputNotes = dialog.findViewById<TextInputEditText>(R.id.inputNotes)
            val inputCash = dialog.findViewById<TextInputEditText>(R.id.inputCash)
            val spinnerStatus = dialog.findViewById<Spinner>(R.id.spinnerStatus)
            val spinnerBranch = dialog.findViewById<Spinner>(R.id.spinnerBranch)
            val spinnerCourier = dialog.findViewById<Spinner>(R.id.spinnerCourier)
            val containerItems = dialog.findViewById<LinearLayout>(R.id.containerItems)
            val buttonSave = dialog.findViewById<Button>(R.id.buttonSave)
            val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)

            inputCustomer.setText(data.name_client_transaction_laundry ?: "")
            inputNotes.setText(data.notes_transaction_laundry ?: "")
            inputCash.setText((data.cash_transaction_laundry ?: 0).toString())

            val statusList = listOf("paid", "unpaid", "completed", "cancelled")
            spinnerStatus.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                statusList
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            val statusIndex = statusList.indexOf(data.status_transaction_laundry.name)
            if (statusIndex >= 0) spinnerStatus.setSelection(statusIndex)

            spinnerBranch.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                branches.map { it.name_branch }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            val selectedBranchIndex = branches.indexOfFirst {
                it.id_branch == data.id_branch_transaction_laundry
            }
            if (selectedBranchIndex >= 0) spinnerBranch.setSelection(selectedBranchIndex)

            spinnerCourier.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                users.map { it.fullname_user }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            val selectedCourierIndex = users.indexOfFirst {
                it.id_user == data.id_kurir_transaction_laundry
            }
            if (selectedCourierIndex >= 0) spinnerCourier.setSelection(selectedCourierIndex)

            val itemInputViews = mutableListOf<Pair<Int, TextInputEditText>>()

            data.list_transaction_laundry?.forEach { item ->
                val itemView = LayoutInflater.from(requireContext()).inflate(
                    R.layout.item_edit_laundry,
                    containerItems,
                    false
                )

                val textItemName = itemView.findViewById<TextView>(R.id.textItemName)
                val inputWeight = itemView.findViewById<TextInputEditText>(R.id.inputWeight)

                val productName = laundryProductViewModel.laundryProducts.value
                    ?.find { it.id_laundry_item == item.id_item_laundry }
                    ?.name_laundry_item ?: "Item Tidak Ditemukan"

                textItemName.text = productName
                inputWeight.setText((item.weight_list_transaction_laundry ?: 0).toString())

                itemInputViews.add((item.id_item_laundry to inputWeight) as Pair<Int, TextInputEditText>)
                containerItems.addView(itemView)
            }

            buttonSave.setOnClickListener {
                val selectedBranch = branches.getOrNull(spinnerBranch.selectedItemPosition)
                val selectedCourier = users.getOrNull(spinnerCourier.selectedItemPosition)

                val listItems = itemInputViews.map { pair ->
                    val rawWeight = pair.second.text?.toString()?.trim().orEmpty()
                    val weight = rawWeight.replace(",", ".").toDoubleOrNull() ?: 0.0

                    UpdateLaundryItem(
                        id_item_laundry = pair.first,
                        weight_list_transaction_laundry = weight.toInt()
                    )
                }

                val invalidItem = listItems.firstOrNull {
                    it.weight_list_transaction_laundry <= 0
                }

                if (inputCustomer.text.toString().trim().isBlank()) {
                    Toast.makeText(requireContext(), "Nama pelanggan wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (invalidItem != null) {
                    Toast.makeText(requireContext(), "Berat item tidak boleh kosong atau 0", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val request = UpdateLaundryFullRequest(
                    cash_transaction_laundry = inputCash.text.toString().trim().toIntOrNull() ?: 0,
                    transaction = UpdateLaundryTransactionData(
                        id_kurir_transaction_laundry = selectedCourier?.id_user,
                        id_branch_transaction_laundry = selectedBranch?.id_branch,
                        name_client_transaction_laundry = inputCustomer.text.toString().trim(),
                        status_transaction_laundry = spinnerStatus.selectedItem.toString(),
                        notes_transaction_laundry = inputNotes.text.toString().trim()
                    ),
                    list_items = listItems
                )

                updateLaundry(id, request)
                dialog.dismiss()
            }

            buttonCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka edit: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

    private fun updateLaundry(id: Int, request: UpdateLaundryFullRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = reportLaundryViewModel.updateReportLaundry(id, request)

                Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()

                if (response.success) {
                    loadDetail()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error update laundry", e)
                Toast.makeText(requireContext(), "Gagal update: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
    val id = transactionReportID ?: return

    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val response = reportLaundryViewModel.getReportLaundryById(id)
            val report = response.data ?: return@launch

            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_delete_laundry)

            dialog.window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            val textTransactionNumber =
                dialog.findViewById<TextView>(R.id.textTransactionNumber)
            val textCustomer =
                dialog.findViewById<TextView>(R.id.textCustomer)
            val buttonSoftDelete =
                dialog.findViewById<Button>(R.id.buttonSoftDelete)
            val buttonPermanentDelete =
                dialog.findViewById<Button>(R.id.buttonPermanentDelete)
            val buttonCancel =
                dialog.findViewById<Button>(R.id.buttonCancel)

            textTransactionNumber.text =
                "No. Transaksi: ${report.number_transaction_laundry}"
            textCustomer.text =
                "Pelanggan: ${report.name_client_transaction_laundry ?: "-"}"
buttonSoftDelete.setOnClickListener {

    val transactionId = report.id_transaction_laundry ?: 0

    Log.d(TAG, "===================================")
    Log.d(TAG, "SOFT DELETE CLICKED")
    Log.d(TAG, "TRANSACTION ID : $transactionId")
    Log.d(TAG, "CUSTOMER : ${report.name_client_transaction_laundry}")
    Log.d(TAG, "NUMBER : ${report.number_transaction_laundry}")

    reportLaundryViewModel.deleteReportLaundry(transactionId)

    dialog.dismiss()
}

buttonPermanentDelete.setOnClickListener {

    val transactionId = report.id_transaction_laundry ?: 0

    Log.d(TAG, "===================================")
    Log.d(TAG, "FORCE DELETE CLICKED")
    Log.d(TAG, "TRANSACTION ID : $transactionId")
    Log.d(TAG, "CUSTOMER : ${report.name_client_transaction_laundry}")
    Log.d(TAG, "NUMBER : ${report.number_transaction_laundry}")

    reportLaundryViewModel.forceDeleteLaundryTransaction(transactionId)

    dialog.dismiss()
}

            buttonCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Gagal membuka dialog hapus: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
}