package dev.ferynnd.baguslaundry.ui.user.transaksi_rental

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.ClientViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.product.RentalProductViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.report.RentalReportViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentCreateListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.model.Client
import dev.ferynnd.baguslaundry.model.ConditionRental
import dev.ferynnd.baguslaundry.model.ProductRental
import dev.ferynnd.baguslaundry.model.StatusRental
import dev.ferynnd.baguslaundry.model.RentalTransactionItem
import dev.ferynnd.baguslaundry.model.RentalTransactionRequest
import dev.ferynnd.baguslaundry.ui.user.UserDashboardFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_laundry.ListTransaksiLaundryFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.ListTransaksiRentalFragment.FilterOption
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        rentalProductViewModel = ViewModelProvider(this)[RentalProductViewModel::class.java]
        rentalProductViewModel.init(requireContext())

        rentalReportViewModel = ViewModelProvider(this)[RentalReportViewModel::class.java]
        rentalReportViewModel.init(requireContext())

        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        clientViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
       _binding = KurirFragmentCreateListTransaksiRentalBinding.inflate(layoutInflater)

        sharePrefrences = SharePrefrenceHelper(requireContext())
        userId = sharePrefrences.getString(PREF_USER_ID)!!.toInt()

        binding.arrowBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, UserDashboardFragment()).addToBackStack(null)
                .commit()
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

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Helper function untuk mendapatkan data dari rental item input
    private fun addRentalItemInput(): List<RentalTransactionItem> {
        val rentalItems = mutableListOf<RentalTransactionItem>()

        for (i in 0 until binding.containerRental.childCount) {
            val itemView = binding.containerRental.getChildAt(i)

            val spinner = itemView.findViewById<Spinner>(R.id.spinnerRental)
            val countEditText = itemView.findViewById<EditText>(R.id.input_pcs_list_transaksi_rental)
            val beratEditText = itemView.findViewById<EditText>(R.id.input_berat_list_transaksi_rental)
            val statusAutoComplete = itemView.findViewById<AutoCompleteTextView>(R.id.input_status_list_transaksi_rental)
            val kondisiAutoComplete = itemView.findViewById<AutoCompleteTextView>(R.id.input_kondisi_list_transaksi_rental)

            // Ambil ID item rental dari tag
            val idItemRental = spinner.getTag(R.id.spinnerRental) as? Int ?: continue

            // Konversi display name ke value untuk status
            val statusDisplayName = statusAutoComplete.text.toString()
            val statusValue = when (statusDisplayName) {
                "Masuk" -> "in"
                "Keluar" -> "out"
                "Dibatalkan" -> "cancelled"
                else -> "in" // default
            }

            // Konversi display name ke value untuk kondisi
            val kondisiDisplayName = kondisiAutoComplete.text.toString()
            val kondisiValue = when (kondisiDisplayName) {
                "Bersih" -> "clean"
                "Kotor" -> "dirty"
                "Rusak" -> "damaged"
                else -> "clean" // default
            }

            // Validasi dan ambil count
            val count = try {
                val countText = countEditText.text.toString()
                if (countText.isNotEmpty()) countText.toInt() else 1
            } catch (e: NumberFormatException) {
                1 // default jika error
            }

            // Validasi dan ambil weight
            val weight = try {
                val weightText = beratEditText.text.toString()
                if (weightText.isNotEmpty()) weightText.toDouble() else 0.0
            } catch (e: NumberFormatException) {
                0.0 // default jika error
            }

            // Buat rental transaction item
            val rentalItem = RentalTransactionItem(
                id_item_rental = idItemRental,
                status_list_transaction_rental = statusValue,
                condition_list_transaction_rental = kondisiValue,
                count_list_transaction_rental = count,
                weight_list_transaction_rental = weight
            )

            rentalItems.add(rentalItem)
        }

        return rentalItems
    }
}