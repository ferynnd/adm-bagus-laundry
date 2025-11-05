package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

import android.os.Build
import android.os.Bundle
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
    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var clientId: Int = 0

    private var fullRentalList: List<ProductRental> = listOf()
    private var clientList: List<Client> = listOf()

    // Untuk tracking data yang sudah dimuat
    private var isUserDataLoaded = false
    private var isRentalDataLoaded = false
    private var isClientDataLoaded = false

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

    private var rentalTransactionItems: MutableList<RentalTransactionItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        rentalProductViewModel =
            ViewModelProvider(this)[RentalProductViewModel::class.java].apply { init(requireContext()) }
        rentalReportViewModel =
            ViewModelProvider(this)[RentalReportViewModel::class.java].apply { init(requireContext()) }
        clientViewModel =
            ViewModelProvider(this)[ClientViewModel::class.java].apply { init(requireContext()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentCreateListTransaksiRentalBinding.inflate(layoutInflater)
        hideBottomNavigationView()

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        if (userId != 0) {
            updateLoadingState(true)
            setupObservers() // Panggil setupObservers di sini untuk menginisialisasi semua observer
            loadInitialData()
        } else {
            Toast.makeText(context, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        setupClickListeners()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() {
        binding.arrowBack.setOnClickListener {
            openUserFragment(UserDashboardFragment(), "UserDashboard")
        }

        binding.btnAddItem.setOnClickListener {
            if (fullRentalList.isNotEmpty()) {
                addRentalItemInput()
            } else {
                Toast.makeText(
                    requireContext(), "Data rental kosong untuk cabang ini", Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (validateRentalTransaction()) {
                processTransaction()
            }
        }
    }

    private fun setupObservers() {
        // Observer khusus untuk transaction loading (untuk submit)
        rentalReportViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Hanya update loading state jika sedang submit transaction
            if (isUserDataLoaded && isRentalDataLoaded && isClientDataLoaded) {
                updateLoadingState(isLoading)
            }
        }

        // Observer untuk create transaction response
        rentalReportViewModel.createTransactionResponse.observe(viewLifecycleOwner) { response ->
            handleTransactionResponse(response)
        }
    }

    private fun loadInitialData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load semua data secara berurutan
                loadUserData()

            } catch (e: Exception) {
                updateLoadingState(false)
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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

                // Setelah user data berhasil, load data lainnya secara parallel
                coroutineScope {
                    launch { loadRentalProductsData() }
                    launch { loadClientData() }
                }

            } else {
                updateLoadingState(false)
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat data pengguna",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            updateLoadingState(false)
            Toast.makeText(
                requireContext(),
                "Gagal memuat data pengguna: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
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
            Toast.makeText(
                requireContext(),
                "Gagal memuat data product item: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
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
        }
    }

    private fun handleRentalProductsData(rentalList: List<ProductRental>) {
        val filtered = rentalList.filter { item ->
            item.id_branch_rental_item?.toInt() == userIdBranch
        }
        fullRentalList = filtered

        if (filtered.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Tidak ada item rental untuk cabang ini",
                Toast.LENGTH_SHORT
            ).show()
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
            clientId = selectedClient?.id_client ?: 0
            binding.alamatKlien.text = selectedClient?.full_address_client ?: "-"
        }

        if (clientList.isEmpty()) {
            binding.alamatKlien.text = "-"
        }
    }

    private fun handleTransactionResponse(response: DefaultRequest<RentalTransactionData>?) {
        try {
            if (response != null) {
                if (response.success) {
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
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Gagal membuat transaksi.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                rentalReportViewModel.resetCreateTransactionResponse()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Gagal memproses response: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addRentalItemInput() {
        val itemView = layoutInflater.inflate(
            R.layout.kurir_list_item_transaksi_rental_input,
            binding.containerRental,
            false
        )

        // Spinner setup
        val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            fullRentalList.map { it.name_rental_item }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // AutoCompleteTextView setup
        val statusAutoComplete =
            itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)
        val kondisiAutoComplete =
            itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)

        // Tambahkan adapter dan item untuk status dan kondisi
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statusOptions.map { it.displayName }
        )
        statusAutoComplete.setAdapter(statusAdapter)

        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            conditionOptions.map { it.displayName }
        )
        kondisiAutoComplete.setAdapter(conditionAdapter)

        // Listener spinner untuk menyimpan ID
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                spinner.setTag(R.id.spinnerRental, fullRentalList[position].id_rental_item)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Tombol hapus
        val trashIcon = itemView.findViewById<ImageView>(R.id.trash_create_list_transaksi)
        trashIcon.setOnClickListener {
            binding.containerRental.removeView(itemView)
            if (binding.containerRental.childCount == 0) {
                Toast.makeText(requireContext(), "Semua item dihapus", Toast.LENGTH_SHORT).show()
            }
        }

        // Tambahkan itemView ke container
        binding.containerRental.addView(itemView)
    }

    private fun validateRentalTransaction(): Boolean {
        // Cek apakah ada item rental
        if (binding.containerRental.childCount == 0) {
            Toast.makeText(
                requireContext(), "Tambahkan minimal satu item rental", Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Validasi item rental satu per satu
        for (i in 0 until binding.containerRental.childCount) {
            val itemView = binding.containerRental.getChildAt(i)
            val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
            val pcsEditText =
                itemView.findViewById<EditText>(R.id.input_pcs_list_transaksi_rental)
            val kondisiDropdown =
                itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
            val statusDropdown =
                itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)

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
            val selectedStatusValue =
                statusOptions.find { it.displayName == selectedStatus }?.value
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
            val selectedKondisiValue =
                conditionOptions.find { it.displayName == selectedKondisi }?.value
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
        rentalTransactionItems.clear()

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
            val statusDropdown =
                itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)
            val selectedStatusCondition = statusDropdown.text.toString()
            val selectedStatusValue =
                statusOptions.find { it.displayName == selectedStatusCondition }?.value

            val kondisiDropdown =
                itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)
            val selectedKondisiCondition = kondisiDropdown.text.toString()
            val selectedKondisiValue =
                conditionOptions.find { it.displayName == selectedKondisiCondition }?.value

            val beratEditText =
                itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
            val beratText = beratEditText.text.toString().trim()

            // Handle empty weight - bisa null atau default value
            val berat = if (beratText.isEmpty()) {
                0.0 // atau bisa null jika data class mendukung nullable
            } else {
                beratText.toDoubleOrNull() ?: 0.0
            }

            val pcsEditText =
                itemView.findViewById<EditText>(R.id.input_pcs_list_transaksi_rental)
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

        showAlert(
            title = "Berhasil!",
            message = "Transaksi berhasil disimpan",
            backgroundColorRes = R.color.primary,
            iconRes = R.drawable.success
        )
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