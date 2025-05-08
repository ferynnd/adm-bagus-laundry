package dev.ferynnd.baguslaundry.ui.user.transaksi_laundry

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
import dev.ferynnd.baguslaundry.controller.user.LaundryTransaksiAdapter
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.LaundryReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentDetailListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiLaundryBinding
import dev.ferynnd.baguslaundry.model.ReportLaundry
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import kotlinx.coroutines.launch

class ListTransaksiLaundryFragment : Fragment() {
    private var _binding: KurirFragmentListTransaksiLaundryBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var transaksiLaundryViewModel: LaundryReportViewModel
    private lateinit var transaksiLaundryAdapter: LaundryTransaksiAdapter

    private lateinit var sharePrefrences: SharePrefrenceHelper

    private var fullTransaksiLaundryList: List<ReportLaundry> = listOf()

    private var userId: Int = 0
    private var userIdBranch: Int = 0
    private var countListTransaksiLaundry: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        transaksiLaundryViewModel = ViewModelProvider(this)[LaundryReportViewModel::class.java]
        transaksiLaundryViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = KurirFragmentListTransaksiLaundryBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        transaksiLaundryAdapter = LaundryTransaksiAdapter { transaksiLaundry: ReportLaundry ->
            onDetailClick(transaksiLaundry)
        }

        binding.recyclerViewTransaksiLaundry.adapter = transaksiLaundryAdapter
        binding.recyclerViewTransaksiLaundry.layoutManager = LinearLayoutManager(requireContext())

        // Dapatkan user dan baru lanjut observe
        if (userId != 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val user = userViewModel.getUserById(userId)
                userIdBranch = user.data.id_branch_user!!.toInt()

                // Setelah userIdBranch tersedia, baru observe
                transaksiLaundryViewModel.laundryReports.observe(viewLifecycleOwner) { productLaundry ->
                    productLaundry?.let {
                        val filteredList = productLaundry.filter { item ->
                            item.id_branch_transaction_laundry == userIdBranch
                        }

                        // Simpan list untuk pencarian
                        fullTransaksiLaundryList = filteredList

                        countListTransaksiLaundry = filteredList.size
                        binding.countData.text = countListTransaksiLaundry.toString()

                        if (fullTransaksiLaundryList.isNotEmpty()) {
                            binding.recyclerViewTransaksiLaundry.visibility = View.VISIBLE
                            binding.containerDataNotFound.visibility = View.GONE

                            transaksiLaundryAdapter.submitList(filteredList)
                        }else {
                            binding.recyclerViewTransaksiLaundry.visibility = View.GONE
                            binding.containerDataNotFound.visibility = View.VISIBLE
                        }
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
                val filtered = fullTransaksiLaundryList.filter {
                    it.name_client_transaction_laundry!!.lowercase().contains(query)
                }
                transaksiLaundryAdapter.submitList(filtered)
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

    private fun onDetailClick(transaksiLaundry: ReportLaundry) {
        Toast.makeText(context, "Detail ${transaksiLaundry.id_transaction_laundry} akan ditampilkan", Toast.LENGTH_SHORT).show()
        val bundle = Bundle().apply {
            putInt("TRANSAKSI_LAUNDRY_ID", transaksiLaundry.id_transaction_laundry ?: 0)
        }
        val detailFragment = DetailListTransaksiLaundryFragment()
        detailFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_user, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}