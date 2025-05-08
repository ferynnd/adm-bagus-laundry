package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.ListRentalTransaksiAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.ListTransactionReportRentalViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentDetailListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.StatusTransactionRental
import dev.ferynnd.baguslaundry.model.TypeTransactionRental
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class DetailListTransaksiRentalFragment : Fragment() {
    private var _binding: KurirFragmentDetailListTransaksiRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var transaksiRentalViewModel: RentalReportViewModel
    private lateinit var listTransactionRentalViewModel: ListTransactionReportRentalViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var rentalProductViewModel: RentalProductViewModel
    private lateinit var listTransaksiRentalAdapter: ListRentalTransaksiAdapter

    private var listTransactionRentalID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        transaksiRentalViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        transaksiRentalViewModel.init(requireContext())
        listTransactionRentalViewModel =
            ViewModelProvider(this)[ListTransactionReportRentalViewModel::class.java]
        listTransactionRentalViewModel.init(requireContext())
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())
        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
        rentalProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentDetailListTransaksiRentalBinding.inflate(layoutInflater)

        listTransactionRentalID = arguments?.getInt("TRANSAKSI_RENTAL_ID") ?: 0

        listTransaksiRentalAdapter = ListRentalTransaksiAdapter()

        binding.recyclerViewTransaksiRental.adapter = listTransaksiRentalAdapter
        binding.recyclerViewTransaksiRental.layoutManager = LinearLayoutManager(requireContext())

        if (listTransactionRentalID != 0) {
            try {
                viewLifecycleOwner.lifecycleScope.launch {
                    val dataTransaskiLaundry =
                        transaksiRentalViewModel.getReportRentalById(listTransactionRentalID).data

                    val dataRentalItem =
                        rentalProductViewModel.getProductRentalById(dataTransaskiLaundry.id_branch_transaction_rental).data

                    val dataClient =
                        clientViewModel.getClientById(dataTransaskiLaundry.id_client_transaction_rental).data

                    val dataUser =
                        userViewModel.getUserById(dataTransaskiLaundry.id_kurir_transaction_rental).data

                    binding.apply {

                         val status =
                            when (dataTransaskiLaundry.status_transaction_rental) {
                                StatusTransactionRental.WAITING_FOR_APPROVAL -> "MENUNGGU"
                                StatusTransactionRental.APPROVED -> "DISETUJUI"
                                StatusTransactionRental.OUT -> "KELUAR"
                                StatusTransactionRental.IN -> "MASUK"
                                StatusTransactionRental.CANCELLED -> "DIBATALKAN"
                            }
                        statusTransaksiRental.text = status
                        when (dataTransaskiLaundry.status_transaction_rental) {
                            StatusTransactionRental.WAITING_FOR_APPROVAL ->
                                wadahStatus.setCardBackgroundColor(
                                    ContextCompat.getColor(requireContext(), R.color.transaksiOuther)
                            )
                            StatusTransactionRental.APPROVED ->
                                wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiOuther)
                            )
                            StatusTransactionRental.OUT ->
                                wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiOut)
                            )
                            StatusTransactionRental.IN ->
                                wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiIn)
                            )
                            StatusTransactionRental.CANCELLED ->
                                wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiCancelled)
                            )
                        }

                        val tipe =
                            when (dataTransaskiLaundry.type_rental_transaction) {
                                TypeTransactionRental.BATH_TOWEL -> "Bath Towel"
                                TypeTransactionRental.HAND_TOWEL -> "Hand Towel"
                                TypeTransactionRental.GORDEN -> "Gorden"
                                TypeTransactionRental.KESET -> "Keset"
                            }
                        tipeTransaksiRental.text = tipe

                        val localeID = Locale("in", "ID")
                        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
                        val decimalFormat = DecimalFormat("#,##0.##")

                        tanggalTransaksiRental.text =
                            dataTransaskiLaundry.time_transaction_rental ?: "-"
                        namaClientTransaksiRental.text = dataClient.name_client ?: "-"
                        namaKurirTransaksiRental.text = dataUser.fullname_user ?: "-"

                        namaPenerimaTransaksiRental.text =
                            dataTransaskiLaundry.recipient_name_transaction_rental ?: "-"

                        // Jumlah item dan berat
                        jumlahItemTransaksiRental.text =
                            "${dataTransaskiLaundry.total_pcs_transaction_rental ?: 0} pcs"
                        totalBeratTransaksiRental.text =
                            "${decimalFormat.format(dataTransaskiLaundry.total_weight_transaction_rental ?: 0.0)} kg"

                        // Harga dan biaya lainnya
                        totalHargaTransaksiRental.text = numberFormat.format(
                            dataTransaskiLaundry.total_price_transaction_rental ?: 0.0
                        )
                        promoTransaksiRental.text = numberFormat.format(
                            dataTransaskiLaundry.promo_transaction_rental ?: 0.0
                        )
                        tambahanTransaksiRental.text = numberFormat.format(
                            dataTransaskiLaundry.additional_cost_transaction_rental ?: 0.0
                        )
                        hargaPerKgTransaksiRental.text =
                            "${numberFormat.format(dataTransaskiLaundry.price_weight_transaction_rental)}/kg"
                        noteTransaksiRental.text =
                            dataTransaskiLaundry.notes_transaction_rental ?: "-"

                    }

                    rentalProductViewModel.rentalProducts.observe(viewLifecycleOwner) { rentalItemList ->
                        listTransaksiRentalAdapter.setRentalItem(rentalItemList)
                    }

                    listTransactionRentalViewModel.listTransactionRentalReports.observe(
                        viewLifecycleOwner
                    ) { listTransactionItem ->
                        listTransactionItem?.let {
                            val filteredList = listTransactionItem.filter { item ->
                                item.id_rental_transaction == listTransactionRentalID
                            }

                            listTransaksiRentalAdapter.submitList(filteredList)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Detail Tidak Bisa Dimuat: ${e}",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        } else {
            Toast.makeText(requireContext(), "Detail Tidak Bisa Dimuat", Toast.LENGTH_SHORT)
                .show()
        }

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, ListTransaksiRentalFragment())
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
}