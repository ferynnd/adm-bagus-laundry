package dev.ferynnd.baguslaundry.ui.user

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.ferynnd.baguslaundry.controller.user.ListItemTransactionLaundryAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.BottomNavViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentListItemTransactionLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.ui.openUserFragment
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ListItemTransactionLaundryFragment : Fragment(), ListItemTransactionLaundryAdapter.OnItemClickListener {

    private var _binding: FragmentListItemTransactionLaundryBinding? = null
    private val binding get() = _binding!!

    private val laundryProductViewModel: LaundryProductViewModel by activityViewModels()
    private val laundryProductAdapter by lazy { ListItemTransactionLaundryAdapter(this) }
    private val bottomNavViewModel: BottomNavViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ListItemTransactionLaundry", "onCreate() dipanggil")
        laundryProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ListItemTransactionLaundry", "onCreateView() dipanggil")
        _binding = FragmentListItemTransactionLaundryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ListItemTransactionLaundry", "onViewCreated() dipanggil")

        bottomNavViewModel.hide()
        Log.d("ListItemTransactionLaundry", "BottomNav disembunyikan")

        setupRecyclerView()
        setupObservers()
        setupListeners()
        loadData()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryProductAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        observeLoadingState()
        observeErrorMessages()
        observeProductData()
        observeSelectedItems()
    }

    private fun observeLoadingState() {
        laundryProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("ListItemTransactionLaundry", "Loading state berubah: $isLoading")
            updateLoadingUI(isLoading)
        }
    }

    private fun observeErrorMessages() {
        laundryProductViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) Log.e("ListItemTransactionLaundry", "Error: $errorMessage")
            handleError(errorMessage)
        }
    }

    private fun observeProductData() {
        laundryProductViewModel.filteredProductLaundry.observe(viewLifecycleOwner) { products ->
            Log.d("ListItemTransactionLaundry", "Data produk terupdate: ${products.size} item")
            laundryProductAdapter.submitList(products)
        }
    }

    private fun observeSelectedItems() {
        laundryProductViewModel.selectedItems.observe(viewLifecycleOwner) { selectedItems ->
            Log.d("ListItemTransactionLaundry", "Jumlah item terpilih: ${selectedItems.size}")
            updateSubmitButtonVisibility(selectedItems.isNotEmpty())
        }
    }

    private fun setupListeners() {
        binding.apply {
            arrowBack.setOnClickListener {
                Log.d("ListItemTransactionLaundry", "Klik tombol back → membuka UserDashboardFragment()")
                navigateBack()
            }
            btnSubmit.setOnClickListener {
                Log.d("ListItemTransactionLaundry", "Klik tombol Submit → membuka LaundryTransactionMenuFragment()")
                navigateToTransactionMenu()
            }
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("ListItemTransactionLaundry", "Memuat data produk laundry dari ViewModel")
            laundryProductViewModel.getProductLaundry()
        }
    }

    private fun updateLoadingUI(isLoading: Boolean) {
        binding.apply {
            progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    private fun handleError(errorMessage: String) {
        if (errorMessage.isNotBlank()) {
            showToast(errorMessage)
            laundryProductViewModel.resetErrorMessage()
        }
    }

    private fun updateSubmitButtonVisibility(hasSelectedItems: Boolean) {
        binding.btnSubmit.visibility = if (hasSelectedItems) View.VISIBLE else View.GONE
    }

    private fun navigateBack() {
        // Cek shared pref sebelum navigasi
        val sharedPref = requireContext().getSharedPreferences("YOUR_PREF_NAME", 0)
        val saved = sharedPref.getString("YOUR_KEY", "tidak ada")
        Log.d("ListItemTransactionLaundry", "Pref tersimpan: $saved")

        openUserFragment(UserDashboardFragment(), "UserDashboard")
    }

    private fun navigateToTransactionMenu() {
        openUserFragment(LaundryTransactionMenuFragment(), "MenuTransactionLaundry")
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    override fun onItemClick(item: ProductLaundry, position: Int) {
        Log.d("ListItemTransactionLaundry", "Klik item: ${item.name_laundry_item}")
        laundryProductAdapter.toggleSelection(item)
    }

    override fun onSelectionChanged(selectedCount: Int) {
        Log.d("ListItemTransactionLaundry", "onSelectionChanged → $selectedCount item")
        laundryProductViewModel.setSelectedItems(laundryProductAdapter.getSelectedItems())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ListItemTransactionLaundry", "onDestroyView() dipanggil → BottomNav muncul lagi")
        bottomNavViewModel.show()
        _binding = null
    }
}
