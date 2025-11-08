package dev.ferynnd.baguslaundry.ui.user

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_LAST_FRAGMENT
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BottomNavViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.NetworkViewModel
import dev.ferynnd.baguslaundry.databinding.ActivityUserBinding
import dev.ferynnd.baguslaundry.ui.BluetoothPairingFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.CreateListTransaksiRentalFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_rental.PrintPreviewRentalFragment

@RequiresApi(Build.VERSION_CODES.O)
class UserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBinding
    private lateinit var networkViewModel: NetworkViewModel
    private var noInternetDialog: AlertDialog? = null
    private lateinit var sharedPreferences: SharePrefrenceHelper

    private val bottomNavViewModel: BottomNavViewModel by viewModels()

    private var isSyncingBottomNav = false


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            return
        }

        bottomNavViewModel.isVisible.observe(this) { visible ->
            binding.bottomNav.visibility = if (visible) View.VISIBLE else View.GONE
        }

        networkViewModel = ViewModelProvider(this)[NetworkViewModel::class.java]
        sharedPreferences = SharePrefrenceHelper(this)


        networkViewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                dismissNoInternetDialog()

                val lastFragmentTag = sharedPreferences.getString(PREF_LAST_FRAGMENT).toString()

                val fragment = when (lastFragmentTag) {
                    "ListItemTransactionLaundry" -> ListItemTransactionLaundryFragment()
                    "MenuTransactionLaundry" -> LaundryTransactionMenuFragment()
                    "PrintPreviewLaundry" -> PrintPreviewFragment()
                    "PrintPreviewRental" -> PrintPreviewRentalFragment()
                    "KurirTransactionReport" -> KurirTransactionReportFragment()
                    "KurirProduct" -> KurirProductFragment()
                    "CreateTransactionRental" -> CreateListTransaksiRentalFragment()
                    "UserProfile" -> UserProfileFragment()
                    "Bluetooth" -> BluetoothPairingFragment()
                    else -> UserDashboardFragment()
                }
                 val addToBackStack = lastFragmentTag != "UserDashboard"
                openFragment(fragment, lastFragmentTag, addToBackStack)
                syncBottomNavigation(lastFragmentTag)
            } else {
                showNoInternetDialog()
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.host_fragment_user)
            val tag = currentFragment?.tag ?: "UserDashboard"
            sharedPreferences.put(PREF_LAST_FRAGMENT, tag)
        }

        binding.bottomNav.setOnItemSelectedListener {
             if (isSyncingBottomNav) {
                return@setOnItemSelectedListener true
            }

            when (it.itemId) {
                R.id.transactionMenu -> {
                    openFragment(UserDashboardFragment(), "UserDashboard", false)
                    true
                }
                R.id.itemMenu -> {
                    openFragment(KurirProductFragment(), "KurirProduct", false)
                    true
                }
                R.id.reportMenu -> {
                    openFragment(KurirTransactionReportFragment(), "KurirTransactionReport", false)
                    true
                }
                else -> false
            }
        }
    }

    private fun showNoInternetDialog() {
        if (noInternetDialog == null || noInternetDialog?.isShowing == false) {
            noInternetDialog = AlertDialog.Builder(this)
                .setTitle("Tidak Ada Koneksi Internet")
                .setMessage("Silakan aktifkan internet Anda untuk melanjutkan.")
                .setCancelable(false)
                .setPositiveButton("Buka Pengaturan") { _, _ ->
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                .show()
        }
    }

    private fun dismissNoInternetDialog() {
        noInternetDialog?.dismiss()
        noInternetDialog = null
    }

    fun openFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_user, fragment, tag)

        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }

        transaction.commit()
        sharedPreferences.put(PREF_LAST_FRAGMENT, tag)

    }

    private fun syncBottomNavigation(tag: String?) {
        isSyncingBottomNav = true

        val selectedItem = when (tag) {
            "UserDashboard" -> R.id.transactionMenu
            "KurirProduct" -> R.id.itemMenu
            "KurirTransactionReport" -> R.id.reportMenu
            else -> R.id.transactionMenu
        }

        binding.bottomNav.selectedItemId = selectedItem

        binding.bottomNav.postDelayed({
            isSyncingBottomNav = false
        }, 150)
    }

}
