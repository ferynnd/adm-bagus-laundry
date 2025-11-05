package dev.ferynnd.baguslaundry.ui.user

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.ListItemTransactionLaundryAdapter
import dev.ferynnd.baguslaundry.data.viewmodel.product.LaundryProductViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentListItemTransactionLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry
import dev.ferynnd.baguslaundry.ui.openUserFragment
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ListItemTransactionLaundryFragment : Fragment(), ListItemTransactionLaundryAdapter.OnItemClickListener {

    private lateinit var binding: FragmentListItemTransactionLaundryBinding
    private lateinit var laundryProductViewModel: LaundryProductViewModel
    private lateinit var laundryProductAdapter: ListItemTransactionLaundryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        laundryProductViewModel = ViewModelProvider(requireActivity())[LaundryProductViewModel::class.java]
        laundryProductViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListItemTransactionLaundryBinding.inflate(inflater, container, false)
        hideBottomNavigationView()

        // Inisialisasi adapter
        laundryProductAdapter = ListItemTransactionLaundryAdapter(this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = laundryProductAdapter
        }

        // Observasi loading state
        laundryProductViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progresBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (!isLoading) View.VISIBLE else View.GONE
        }

        // Observasi error
        laundryProductViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                laundryProductViewModel.resetErrorMessage()
            }
        }

        // Observasi data produk
        laundryProductViewModel.filteredProductLaundry.observe(viewLifecycleOwner) { filteredProducts ->
            laundryProductAdapter.submitList(filteredProducts)
        }

        laundryProductViewModel.selectedItems.observe(viewLifecycleOwner) { selected ->
            binding.btnSubmit.visibility = if (selected.isNotEmpty()) View.VISIBLE else View.GONE
            binding.btnSubmit.setOnClickListener {
                openUserFragment(LaundryTransactionMenuFragment(), "MenuTransactionLaundry")
            }
        }

        binding.arrowBack.setOnClickListener {
            openUserFragment(UserDashboardFragment(), "UserDashboard")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            laundryProductViewModel.getProductLaundry()
        }

        return binding.root
    }

    private fun hideBottomNavigationView() {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.VISIBLE
    }

    override fun onItemClick(item: ProductLaundry, position: Int) {
        toggleItemSelection(item)
    }

    override fun onSelectionChanged(selectedCount: Int) {
        laundryProductViewModel.setSelectedItems(laundryProductAdapter.getSelectedItems())
    }

    private fun toggleItemSelection(item: ProductLaundry) {
        laundryProductAdapter.toggleSelection(item)
    }
}
