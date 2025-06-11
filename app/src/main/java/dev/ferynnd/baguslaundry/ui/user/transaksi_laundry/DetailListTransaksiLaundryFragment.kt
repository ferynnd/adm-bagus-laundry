package dev.ferynnd.baguslaundry.ui.user.transaksi_laundry

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
import dev.ferynnd.baguslaundry.controller.user.ListLaundryTransaksiAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.ListTransactionReportLaundryViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentDetailListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.StatusReportLaundry
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DetailListTransaksiLaundryFragment : Fragment() {
    private var _binding: KurirFragmentDetailListTransaksiLaundryBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var transactionLaundryViewModel: LaundryReportViewModel
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var listTransactionLaundryViewModel: ListTransactionReportLaundryViewModel
    private lateinit var listTransaksiLaundryAdapter: ListLaundryTransaksiAdapter

    private var listTransactionLaundryID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        listTransactionLaundryViewModel =
            ViewModelProvider(this)[ListTransactionReportLaundryViewModel::class.java]
        listTransactionLaundryViewModel.init(requireContext())
        transactionLaundryViewModel =
            ViewModelProvider(this)[LaundryReportViewModel::class.java]
        transactionLaundryViewModel.init(requireContext())
        laundryProductViewModel = ViewModelProvider(this)[LaundryProductViewModel::class.java]
        laundryProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentDetailListTransaksiLaundryBinding.inflate(layoutInflater)

        listTransactionLaundryID = arguments?.getInt("TRANSAKSI_LAUNDRY_ID") ?: 0

        listTransaksiLaundryAdapter = ListLaundryTransaksiAdapter()

        binding.recyclerViewTransaksiLaundry.adapter = listTransaksiLaundryAdapter
        binding.recyclerViewTransaksiLaundry.layoutManager = LinearLayoutManager(requireContext())

        if (listTransactionLaundryID != 0) {
            try {
                viewLifecycleOwner.lifecycleScope.launch {
                    val dataTransaskiLaundry =
                        transactionLaundryViewModel.getReportLaundryById(listTransactionLaundryID).data

                    laundryProductViewModel.laundryProducts.observe(viewLifecycleOwner) { LaundryList ->
                        listTransaksiLaundryAdapter.setLaundryTransaksitem(LaundryList)
                    }
                    binding.apply {

                         val status =
                            when (dataTransaskiLaundry.status_transaction_laundry) {
                                StatusReportLaundry.paid -> "SUDAH BAYAR"
                                StatusReportLaundry.unpaid -> "BELUM BAYAR"
                                StatusReportLaundry.completed -> "SELESAI"
                                StatusReportLaundry.cancelled -> "DIBATALKAN"
                            }
                        statusTransaksiLaundry.text = status
                        when (dataTransaskiLaundry.status_transaction_laundry) {
                            StatusReportLaundry.paid -> wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiOuther)
                            )
                            StatusReportLaundry.unpaid -> wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiOuther)
                            )
                            StatusReportLaundry.completed -> wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiIn)
                            )
                            StatusReportLaundry.cancelled -> wadahStatus.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.transaksiCancelled)
                            )
                        }


                        val localeID = Locale("in", "ID")
                        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

                        totalHargaTransaksiLaundry.text = numberFormat.format(
                            dataTransaskiLaundry.total_transaction_laundry ?: 0.0
                        )
                        tunaiTransaksiLaundry.text = numberFormat.format(
                            dataTransaskiLaundry.cash_transaction_laundry ?: 0.0
                        )
                        kembalianTransaksiLaundry.text = numberFormat.format(
                            dataTransaskiLaundry.change_money_transaction_laundry ?: 0.0
                        )
                        tanggalMasukTransaksiLaundry.text =
                            dataTransaskiLaundry.first_date_transaction_laundry ?: "-"
                        tanggalKeluarTransaksiLaundry.text =
                            dataTransaskiLaundry.last_date_transaction_laundry ?: "-"
                        namaPelangganTransaksiLaundry.text =
                            dataTransaskiLaundry.name_client_transaction_laundry ?: "-"
                        beratTransaksiLaundry.text =
                            (dataTransaskiLaundry.total_weight_transaction_laundry
                                ?: 0.0).toString()
                        pcsTransaksiLaundry.text = dataTransaskiLaundry.count_item_laundry_transaction_laundry.toString()
                        noteTransaksiLaundry.text =
                            dataTransaskiLaundry.notes_transaction_laundry ?: "-"

                    }

                    listTransactionLaundryViewModel.listTransactionLaundryReports.observe(
                        viewLifecycleOwner
                    ) { listTransactionItem ->
                        listTransactionItem?.let {
                            val filteredList = listTransactionItem.filter { item ->
                                item.id_transaction_laundry == listTransactionLaundryID
                            }

                            listTransaksiLaundryAdapter.submitList(filteredList)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Detail Tidak Bisa Dimuat: ${e}", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(requireContext(), "Detail Tidak Bisa Dimuat", Toast.LENGTH_SHORT)
                .show()
        }


        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, ListTransaksiLaundryFragment())
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
