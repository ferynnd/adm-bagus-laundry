package dev.ferynnd.baguslaundry.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tapadoo.alerter.Alerter
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.NetworkViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.ActivityLoginBinding
import dev.ferynnd.baguslaundry.model.UserRole
import dev.ferynnd.baguslaundry.ui.admin.AdminActivity
import dev.ferynnd.baguslaundry.ui.user.UserActivity
import kotlinx.coroutines.launch
import java.net.ConnectException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val userViewModel: UserViewModel by viewModels()
    private lateinit var sharedPreferences: SharePrefrenceHelper
    private lateinit var networkViewModel: NetworkViewModel
    private var noInternetDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = SharePrefrenceHelper(this)
        networkViewModel = ViewModelProvider(this)[NetworkViewModel::class.java]

        networkViewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                dismissNoInternetDialog()
                checkLogin()
                userViewModel.loginResult.observe(this) { result ->
                        result.onSuccess {
                            if (it.data != null) {
                                sharedPreferences.put(Constant.PREF_IS_LOGIN, true)
                                it.data.username?.let { it1 ->
                                    sharedPreferences.put(Constant.PREF_USER_NAME,
                                        it1
                                    )
                                }
                                it.token?.let { it1 -> sharedPreferences.put(Constant.PREF_USER_TOKEN, it1) }
                                it.data.id_user.let { it1 -> sharedPreferences.put(Constant.PREF_USER_ID, it1.toString()) }
                                it.data.role_user.let { it1 -> sharedPreferences.put(Constant.PREF_USER_ROLE, it1.toString()) }
                                navigateToRole(it.data.role_user)
                            }
                        }

                        result.onFailure { throwable ->
                                if (throwable is ConnectException) {
                                     runOnUiThread {
                                            Alerter.create(this)
                                                    .setTitle("Gagal!")
                                                    .setText("Gagal terhubung ke server. Periksa jaringan Anda.")
                                                    .setBackgroundColorRes(R.color.red600)
                                                    .setDuration(4000)
                                                    .show()
                                        }
                                } else {
                                    Toast.makeText(this, "Login Error Kesalahan Username atau Password", Toast.LENGTH_SHORT).show()
                                     runOnUiThread {
                                            Alerter.create(this)
                                                    .setTitle("Gagal!")
                                                    .setText("Login Error Kesalahan Username atau Password")
                                                    .setBackgroundColorRes(R.color.red600)
                                                    .setDuration(4000)
                                                    .show()
                                        }
                                }
                        }
                }
            } else {
                showNoInternetDialog()
            }
        }

        binding.buttonKirim.setOnClickListener {
            val username = binding.inputTextUsername.text.toString()
            val password = binding.inputTextPassword.text.toString()

            if ( username.isNotEmpty() && password.isNotEmpty() ) {
                 lifecycleScope.launch {
                     userViewModel.login(username, password)
                 }
            } else {
                  Alerter.create(this)
                    .setTitle("Peringatan!")
                    .setText("Masukan Username dan Password")
                    .setBackgroundColorRes(R.color.primary)
                    .setIcon(R.drawable.info)
                    .setIconColorFilter(0)
                    .setDuration(4000)
                    .show()
            }

        }

        userViewModel.alertEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { alertData ->
                Alerter.create(this)
                    .setTitle( alertData.title)
                    .setText(alertData.message)
                    .setBackgroundColorRes(alertData.backgroundColorRes)
                    .setDuration(alertData.duration)
                    .show()
            }
        }


    }

     private fun checkLogin() {
        val token = sharedPreferences.getString(Constant.PREF_USER_TOKEN, null)
        if (token.isNullOrEmpty()) {
            // Token tidak ada, arahkan pengguna ke halaman login
            return // Tidak perlu panggil LoginActivity lagi
        } else {
            // Token ada, periksa role
            val role = sharedPreferences.getString(Constant.PREF_USER_ROLE, null)
            when (role) {
                UserRole.admin.toString() -> {
                    navigateToRole(UserRole.admin)
                }
                UserRole.kurir.toString() -> {
                    navigateToRole(UserRole.kurir)
                }
                else -> {
                    sharedPreferences.clear() // Bersihkan data yang tidak valid
                    startActivity(Intent(this, LoginActivity::class.java)) // Arahkan ke login
                    finish()
                }
            }
        }
    }


    private fun navigateToRole(role: UserRole) {
        when (role) {
            UserRole.kurir -> {
                 Alerter.create(this)
                    .setTitle("Berhasil!")
                    .setText("Berhasil Login")
                    .setBackgroundColorRes(R.color.primary)
                    .setIcon(R.drawable.success)
                    .setIconColorFilter(0)
                    .setDuration(2000)
                    .setOnHideListener {
                        startActivity(Intent(this, UserActivity::class.java))
                        finish()
                    }
                    .show()
            }
            UserRole.admin -> {
                 Alerter.create(this)
                    .setTitle("Berhasil!")
                    .setText("Berhasil Login")
                    .setBackgroundColorRes(R.color.primary)
                    .setIcon(R.drawable.success)
                    .setIconColorFilter(0)
                    .setDuration(2000)
                    .setOnHideListener {
                        startActivity(Intent(this, AdminActivity::class.java))
                        finish()
                    }
                    .show()
            }
            else -> {
                Toast.makeText(this, "Login Tidak Sesuai Role", Toast.LENGTH_SHORT).show()
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