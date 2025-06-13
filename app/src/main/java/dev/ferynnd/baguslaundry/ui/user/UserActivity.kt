package dev.ferynnd.baguslaundry.ui.user

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.controller.user.KurirProductAdapter
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.NetworkViewModel
import dev.ferynnd.baguslaundry.databinding.ActivityUserBinding
import dev.ferynnd.baguslaundry.ui.user.product_laundry.ListProductLaundryFragment
import dev.ferynnd.baguslaundry.ui.user.transaksi_laundry.ListTransaksiLaundryFragment

class UserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBinding
    private lateinit var networkViewModel: NetworkViewModel
    private var noInternetDialog: AlertDialog? = null
    private lateinit var sharedPreferences: SharePrefrenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        networkViewModel = ViewModelProvider(this)[NetworkViewModel::class.java]

        networkViewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                dismissNoInternetDialog()
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_user, UserDashboardFragment())
                        .commit()
                }
            } else {
                showNoInternetDialog()
            }
        }

        replaceFragment(UserDashboardFragment())

        binding.bottomNav.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.transactionMenu -> replaceFragment(UserDashboardFragment())
                R.id.itemMenu -> replaceFragment(KurirProductFragment())
                R.id.reportMenu -> replaceFragment(KurirTransactionReportFragment())
            }
            true
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

    private fun replaceFragment(fragment: Fragment){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.host_fragment_user, fragment).commit()
    }

    private fun dismissNoInternetDialog() {
        noInternetDialog?.dismiss()
        noInternetDialog = null
    }
}
