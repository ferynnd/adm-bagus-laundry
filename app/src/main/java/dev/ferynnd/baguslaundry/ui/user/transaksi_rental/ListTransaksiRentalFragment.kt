package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.RentalTransaksiAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.ReportRental
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch

class ListTransaksiRentalFragment : Fragment() {
    private var _binding: KurirFragmentListTransaksiRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var transaksiRentalViewModel: RentalReportViewModel
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var transaksiRentalAdapter: RentalTransaksiAdapter

    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullTransaksiRentalList: List<ReportRental> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countProductLaundry: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())
        transaksiRentalViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        transaksiRentalViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentListTransaksiRentalBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        transaksiRentalAdapter = RentalTransaksiAdapter { transaksiRental: ReportRental ->
            onDetailClick(transaksiRental)
        }

        binding.recyclerViewTransaksiRental.adapter = transaksiRentalAdapter
        binding.recyclerViewTransaksiRental.layoutManager = LinearLayoutManager(requireContext())

        // Dapatkan user dan baru lanjut observe
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val user = userViewModel.getUserById(userId)
                userIdBranch = user.data.id_branch_user!!.toInt()

                userViewModel.users.observe(viewLifecycleOwner) { kurirList ->
                    transaksiRentalAdapter.setKurir(kurirList)
                }

                clientViewModel.clients.observe(viewLifecycleOwner) { ClientList ->
                    transaksiRentalAdapter.setClient(ClientList)
                }

                // Setelah userIdBranch tersedia, baru observe
                transaksiRentalViewModel.rentalReports.observe(viewLifecycleOwner) { productLaundry ->
                    productLaundry?.let {
                        val filteredList = productLaundry.filter { item ->
                            item.id_branch_transaction_rental == userIdBranch
                        }

                        // Simpan list untuk pencarian
                        fullTransaksiRentalList = filteredList

                        countProductLaundry = filteredList.size
                        binding.countData.text = countProductLaundry.toString()

                        transaksiRentalAdapter.submitList(filteredList)
                    }
                }
            }
        }

        // Fungsi pencarian
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // kita proses real-time, jadi tidak perlu submit
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty().lowercase()
                val filtered = fullTransaksiRentalList.filter {
                    it.recipient_name_transaction_rental!!.lowercase().contains(query)
                }
                transaksiRentalAdapter.submitList(filtered)
                binding.searchView.setIconifiedByDefault(false)
                binding.countData.text = filtered.size.toString()
                return true
            }
        })

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, UserDashboardFragment())
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

    private fun onDetailClick(transaksiRental: ReportRental) {
        Toast.makeText(context, "Detail ${transaksiRental.id_transaction_rental} akan ditampilkan", Toast.LENGTH_SHORT).show()
        val bundle = Bundle().apply {
            putInt("TRANSAKSI_RENTAL_ID", transaksiRental.id_transaction_rental)  // Mengirimkan ID supplier ke fragment berikutnya
        }
        val detailFragment = DetailListTransaksiRentalFragment()
        detailFragment.arguments = bundle  // Menetapkan argumen untuk fragment detail

        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_user, detailFragment)  // Mengganti fragment saat ini dengan DetailSupplierFragment
            .addToBackStack(null)  // Menambahkan transaksi ke back stack agar pengguna bisa kembali
            .commit()  // Menyelesaikan transaksi
    }
}