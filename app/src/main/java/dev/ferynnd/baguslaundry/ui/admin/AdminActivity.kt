package dev.ferynnd.baguslaundry.ui.admin

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.NetworkViewModel
import dev.ferynnd.baguslaundry.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var networkViewModel: NetworkViewModel
    private var noInternetDialog: AlertDialog? = null
    private lateinit var sharedPreferences: SharePrefrenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        // Inisialisasi binding
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        networkViewModel = ViewModelProvider(this)[NetworkViewModel::class.java]



        networkViewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                dismissNoInternetDialog()
                  if (savedInstanceState == null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.host_fragment_admin, AdminDashboardFragment())
                            .commit()
                  }
            } else {
                showNoInternetDialog()
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
}