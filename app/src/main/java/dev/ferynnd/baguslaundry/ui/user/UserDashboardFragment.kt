package dev.ferynnd.baguslaundry.ui.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_NAME
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.KurirFragmentListTransaksiRentalBinding
import dev.ferynnd.baguslaundry.databinding.KurirFragmentUserDashboardBinding
import dev.ferynnd.baguslaundry.ui.LoginActivity
import dev.ferynnd.baguslaundry.ui.user.product_laundry.ListProductLaundryFragment
import dev.ferynnd.baguslaundry.ui.user.product_rental.ListProductRentalFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_laundry.CreateListTransaksiLaundryFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_laundry.ListTransaksiLaundryFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.CreateListTransaksiRentalFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.ListTransaksiRentalFragment
import kotlinx.coroutines.launch


class UserDashboardFragment : Fragment() {

    private lateinit var binding: KurirFragmentUserDashboardBinding
    private lateinit var sharePrefrences: SharePrefrenceHelper
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = KurirFragmentUserDashboardBinding.inflate(inflater, container, false)

        sharePrefrences = SharePrefrenceHelper(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
              try {
                  val nameUser = sharePrefrences.getString(PREF_USER_NAME, null)
                  binding.headerName.text = nameUser
              } catch ( e : Exception) {
                  throw e
              }
        }

        binding.menuIcon.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.menu_popup, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_setting -> {
                        Toast.makeText(requireContext(), "Menu: Setting", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_user, UserProfileFragment())
                            .addToBackStack("setting")
                            .commit()
                        true
                    }

                    R.id.menu_logout -> {
                       logoutDialog()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }

        binding.menuProduct.setOnClickListener {
            showDialogMenu(" PRODUK")
        }

        binding.menuReport.setOnClickListener {
            showDialogMenu(" LAPORAN")
        }

        binding.menuTransaksiLaundry.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, CreateListTransaksiLaundryFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.menuTransaksiRental.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.host_fragment_user, CreateListTransaksiRentalFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root

    }


    private fun logoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah kamu yakin ingin logout?")
            .setPositiveButton("Ya") { dialog, _ ->
                sharePrefrences.clear()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDialogMenu(textMenu : String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_menu_product)

        dialog.findViewById<TextView>(R.id.textHeaderSecond).text = textMenu
        when (textMenu) {
            " LAPORAN" -> {
                val btnRental: LinearLayout = dialog.findViewById(R.id.iconProductRental)
                btnRental.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, ListTransaksiRentalFragment())
                        .addToBackStack(null)
                        .commit()
                    dialog.dismiss()
                }

                val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
                btnLaundry.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, ListTransaksiLaundryFragment())
                        .addToBackStack(null)
                        .commit()
                    dialog.dismiss()
                }
            }

            " PRODUK" -> {
                  val btnRental: LinearLayout = dialog.findViewById(R.id.iconProductRental)
                    btnRental.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_user, ListProductRentalFragment())
                            .addToBackStack(null)
                            .commit()
                        dialog.dismiss()
                    }

                    val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
                    btnLaundry.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_user, ListProductLaundryFragment())
                            .addToBackStack(null)
                            .commit()
                        dialog.dismiss()
                    }
            }
        }

        dialog.show()
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }


}