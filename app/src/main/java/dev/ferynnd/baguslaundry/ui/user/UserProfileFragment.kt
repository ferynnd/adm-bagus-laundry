package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.ferynnd.baguslaundry.data.helper.Constant.Companion.PREF_USER_ID
import dev.ferynnd.baguslaundry.data.helper.SharePrefrenceHelper
import dev.ferynnd.baguslaundry.data.viewmodel.UserViewModel
import dev.ferynnd.baguslaundry.databinding.FragmentProfileBinding
import dev.ferynnd.baguslaundry.model.UserGender
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharePrefrenceHelper: SharePrefrenceHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        sharePrefrenceHelper = SharePrefrenceHelper(requireContext())

        val userId = sharePrefrenceHelper.getString(PREF_USER_ID, null)

        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val dataUser = userViewModel.getUserById(userId.toInt()).data
                binding.tvRole.text = dataUser.role_user.toString()
                binding.tvUsername.text = dataUser.username
                binding.tvFullname.text = dataUser.fullname_user
                binding.tvPhone.text = dataUser.phone_user
                val dataGender = when(dataUser.gender_user){
                    UserGender.male -> "Laki-laki"
                    UserGender.female -> "Perempuan"
                }
                binding.tvGender.text = dataGender
                binding.tvAddress.text = dataUser.address_user
                binding.tvStatus.text = dataUser.is_active_user.toString()
            }
        }

        binding.btnBack.setOnClickListener{
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

}