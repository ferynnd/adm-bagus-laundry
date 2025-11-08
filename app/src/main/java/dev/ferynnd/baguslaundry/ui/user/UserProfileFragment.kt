package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_TOKEN
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.BottomNavViewModel
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentProfileBinding
import dev.ferynnd.baguslaundry.model.UserGender
import dev.ferynnd.baguslaundry.ui.ResetPasswordDialog
import dev.ferynnd.baguslaundry.ui.showAlert
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharePrefrenceHelper: SharePrefrenceHelper

    private val bottomNavViewModel : BottomNavViewModel by activityViewModels()

    private var savedUserData: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java].apply {
            init(requireContext())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::binding.isInitialized) {
            outState.putString("role_user", binding.tvRole.text.toString())
            outState.putString("username", binding.tvUsername.text.toString())
            outState.putString("fullname", binding.tvFullname.text.toString())
            outState.putString("phone", binding.tvPhone.text.toString())
            outState.putString("gender", binding.tvGender.text.toString())
            outState.putString("address", binding.tvAddress.text.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        sharePrefrenceHelper = SharePrefrenceHelper(requireContext())
        bottomNavViewModel.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.savedUserData = savedInstanceState

        if (savedInstanceState != null) {
            binding.tvRole.text = savedInstanceState.getString("role_user")
            binding.tvUsername.text = savedInstanceState.getString("username")
            binding.tvFullname.text = savedInstanceState.getString("fullname")
            binding.tvPhone.text = savedInstanceState.getString("phone")
            binding.tvGender.text = savedInstanceState.getString("gender")
            binding.tvAddress.text = savedInstanceState.getString("address")
            return
        }

        loadUserData()
    }

    private fun loadUserData() {
        val userId = sharePrefrenceHelper.getString(PREF_USER_ID, null)
        val userToken = sharePrefrenceHelper.getString(PREF_USER_TOKEN, null)

        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val response = userViewModel.getUserById(userId.toInt())
                val dataUser = response.data

                if (dataUser != null) {
                    binding.tvRole.text = dataUser.role_user.toString()
                    binding.tvUsername.text = dataUser.username
                    binding.tvFullname.text = dataUser.fullname_user
                    binding.tvPhone.text = dataUser.phone_user
                    binding.tvGender.text = when (dataUser.gender_user) {
                        UserGender.male -> "Laki-laki"
                        UserGender.female -> "Perempuan"
                        else -> "-"
                    }
                    binding.tvAddress.text = dataUser.address_user
                } else {
                    showAlert(
                        title = "Gagal Memuat",
                        message = "Data pengguna tidak ditemukan di server.",
                        backgroundColorRes = R.color.red600,
                        iconRes = R.drawable.failed,
                        duration = 3000
                    )
                }

            }
        } else {
            showAlert(
                title = "Gagal",
                message = "User ID tidak ditemukan di preferensi.",
                backgroundColorRes = R.color.red600,
                iconRes = R.drawable.failed,
                duration = 3000
            )
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnPassword.setOnClickListener {
            val dialog = ResetPasswordDialog { current, new, confirm ->
                viewLifecycleOwner.lifecycleScope.launch {
                    userViewModel.changePassword(userToken.toString(), current, new, confirm)
                }
            }
            dialog.show(parentFragmentManager, "ResetPasswordDialog")
        }

        userViewModel.alertEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { alertData ->
                showAlert(
                    title = alertData.title,
                    message = alertData.message,
                    backgroundColorRes = alertData.backgroundColorRes,
                    iconRes = alertData.iconRes,
                    duration = alertData.duration
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNavViewModel.show()
    }
}
