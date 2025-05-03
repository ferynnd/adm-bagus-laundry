package dev.ferynnd.baguslaundry.ui.user.transaksi_laundry

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.LaundryTransaksiAdapter
import dev.ferynnd.baguslaundry.controller.user.ListLaundryTransaksiAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BranchViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.ListTransactionReportLaundryViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentDetailListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.model.StatusReportaundry
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.collections.filter

class DetailListTransaksiLaundryFragment : Fragment() {
    private var _binding: KurirFragmentDetailListTransaksiLaundryBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var transactionLaundryViewModel: LaundryReportViewModel
    private lateinit var listTransactionLaundryViewModel: ListTransactionReportLaundryViewModel
    private lateinit var listTransaksiLaundryAdapter: ListLaundryTransaksiAdapter

    private var listTransactionLaundryID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        listTransactionLaundryViewModel =
            ViewModelProvider(this)[ListTransactionReportLaundryViewModel::class.java]
        transactionLaundryViewModel =
            ViewModelProvider(this)[LaundryReportViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentDetailListTransaksiLaundryBinding.inflate(layoutInflater)

        listTransactionLaundryID = arguments?.getInt("TRANSAKSI_ID") ?: 0

        listTransaksiLaundryAdapter = ListLaundryTransaksiAdapter()

        binding.recyclerViewTransaksiLaundry.adapter = listTransaksiLaundryAdapter
        binding.recyclerViewTransaksiLaundry.layoutManager = LinearLayoutManager(requireContext())

        try {
            if (listTransactionLaundryID != 0) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val dataTransaskiLaundry = transactionLaundryViewModel.getReportLaundryById(listTransactionLaundryID).data

                    binding.apply {

                        statusTransaksiLaundry.text = when (dataTransaskiLaundry.status_transaction_laundry) {
                            StatusReportaundry.pending -> "MENUNGGU"
                            StatusReportaundry.in_progress -> "SEDANG DIPROSES"
                            StatusReportaundry.completed -> "SELESAI"
                            StatusReportaundry.cancelled -> "DIBATALKAN"
                        }

                        val localeID = Locale("in", "ID")
                        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

                        totalHargaTransaksiLaundry.text = numberFormat.format(dataTransaskiLaundry.total_transaction_laundry ?: 0.0)
                        tunaiTransaksiLaundry.text = numberFormat.format(dataTransaskiLaundry.cash_transaction_laundry ?: 0.0)
                        kembalianTransaksiLaundry.text = numberFormat.format(dataTransaskiLaundry.change_money_transaction_laundry ?: 0.0)
                        tanggalMasukTransaksiLaundry.text = dataTransaskiLaundry.first_date_transaction_laundry ?: "-"
                        tanggalKeluarTransaksiLaundry.text = dataTransaskiLaundry.last_date_transaction_laundry ?: "-"
                        namaPelangganTransaksiLaundry.text = dataTransaskiLaundry.name_client_transaction_laundry ?: "-"
                        beratTransaksiLaundry.text = (dataTransaskiLaundry.total_weight_transaction_laundry ?: 0.0).toString()
                        noteTransaksiLaundry.text = dataTransaskiLaundry.notes_transaction_laundry ?: "-"

                    }

                    listTransactionLaundryViewModel.listTransactionLaundryReports.observe(
                        viewLifecycleOwner
                    ) { listTransactionItem ->
                        listTransactionItem?.let {
                            listTransaksiLaundryAdapter.submitList(listTransactionItem)
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Detail Tidak Bisa Dimuat", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            throw e
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
