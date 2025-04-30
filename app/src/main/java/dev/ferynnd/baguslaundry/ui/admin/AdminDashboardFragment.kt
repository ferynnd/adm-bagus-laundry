package dev.ferynnd.baguslaundry.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_NAME
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentAdminDashboardBinding
import dev.ferynnd.baguslaundry.ui.LoginActivity
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private lateinit var binding: FragmentAdminDashboardBinding
    private lateinit var sharePrefrences: SharePrefrenceHelper
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)

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
                        // Aksi ke halaman setting
                        Toast.makeText(requireContext(), "Menu: Setting", Toast.LENGTH_SHORT).show()
                        // startActivity(Intent(this, SettingActivity::class.java))
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

        binding.menuBranch.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.host_fragment_admin, ListBranchFragment())
//                .addToBackStack("branch")
//                .commit()
        }

        binding.menuClient.setOnClickListener {
//             parentFragmentManager.beginTransaction()
//                .replace(R.id.host_fragment_admin, ListClientFragment())
//                 .addToBackStack("client")
//                .commit()
        }


        binding.menuProduct.setOnClickListener {
//           showDialogMEnu(" PRODUK")
        }

        binding.menuReport.setOnClickListener {
//            showDialogMEnu(" LAPORAN")
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

//    private fun showDialogMEnu(textMenu : String) {
//        val dialog = Dialog(requireContext())
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(true)
//        dialog.setContentView(R.layout.dialog_menu_product)
//
//        dialog.findViewById<TextView>(R.id.textHeaderSecond).text = textMenu
//        when (textMenu) {
//            " LAPORAN" -> {
//                val btnRental: LinearLayout = dialog.findViewById(R.id.iconProductRental)
//                btnRental.setOnClickListener {
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.host_fragment_admin, ListReportRentalFragment())
//                        .addToBackStack("rental")
//                        .commit()
//                    dialog.dismiss()
//                }
//
//                val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
//                btnLaundry.setOnClickListener {
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.host_fragment_admin, ListReportLaundryFragment())
//                        .addToBackStack("laundry")
//                        .commit()
//                    dialog.dismiss()
//                }
//            }
//
//            " PRODUK" -> {
//                  val btnRental: LinearLayout = dialog.findViewById(R.id.iconProductRental)
//                    btnRental.setOnClickListener {
//                        parentFragmentManager.beginTransaction()
//                            .replace(R.id.host_fragment_admin, ListProductRentalFragment())
//                            .addToBackStack("rental")
//                            .commit()
//                        dialog.dismiss()
//                    }
//
//                    val btnLaundry: LinearLayout = dialog.findViewById(R.id.iconProductLaundry)
//                    btnLaundry.setOnClickListener {
//                        parentFragmentManager.beginTransaction()
//                            .replace(R.id.host_fragment_admin, ListProductLaundryFragment())
//                            .addToBackStack("laundry")
//                            .commit()
//                        dialog.dismiss()
//                    }
//            }
//        }
//
//        dialog.show()
//        val window = dialog.window
//        window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//    }


}