package dev.ferynnd.baguslaundry.ui.admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_LAST_FRAGMENT
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.NetworkViewModel
import dev.ferynnd.baguslaundry.databinding.ActivityAdminBinding
import dev.ferynnd.baguslaundry.ui.admin.product.laundry.AdminListProductLaundryFragment
import dev.ferynnd.baguslaundry.ui.admin.product.rental.AdminListProductRentalFragment
import dev.ferynnd.baguslaundry.ui.admin.report.laundry.AdminListReportLaundryFragment
import dev.ferynnd.baguslaundry.ui.admin.report.rental.AdminListReportRentalFragment

@RequiresApi(Build.VERSION_CODES.O)
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var networkViewModel: NetworkViewModel
    private var noInternetDialog: AlertDialog? = null
    private lateinit var sharedPreferences: SharePrefrenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        networkViewModel = ViewModelProvider(this)[NetworkViewModel::class.java]
        sharedPreferences = SharePrefrenceHelper(this)

        networkViewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                dismissNoInternetDialog()

                if (savedInstanceState == null) {
                    val lastFragmentTag = sharedPreferences.getString(PREF_LAST_FRAGMENT)
                    val fragment = when (lastFragmentTag) {
                        "AdminBranch" -> AdminListBranchFragment()
                        "AdminUser" -> AdminListUserFragment()
                        "AdminClient" -> AdminListClientFragment()
                        "AdminProfile" -> AdminProfileFragment()
                        "AdminListReportRental" -> AdminListReportRentalFragment()
                        "AdminListReportLaundry" -> AdminListReportLaundryFragment()
                        "AdminListProductRental" -> AdminListProductRentalFragment()
                        "AdminListProductLaundry" -> AdminListProductLaundryFragment()
                        else -> AdminDashboardFragment()
                    }

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.host_fragment_admin, fragment, lastFragmentTag)
                        .commit()
                }
            } else {
                showNoInternetDialog()
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.host_fragment_admin)
            val tag = currentFragment?.tag ?: "AdminDashboard"
            sharedPreferences.put(PREF_LAST_FRAGMENT, tag)
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
        sharedPreferences.put(PREF_LAST_FRAGMENT, tag)

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_admin, fragment, tag)
        if (addToBackStack) transaction.addToBackStack(tag)
        transaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.put(PREF_LAST_FRAGMENT, "AdminDashboard")
    }

}
