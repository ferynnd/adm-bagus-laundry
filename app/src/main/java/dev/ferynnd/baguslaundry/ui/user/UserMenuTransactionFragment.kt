package dev.ferynnd.baguslaundry.ui.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.databinding.FragmentUserMenuTransactionBinding

class UserMenuTransactionFragment : Fragment() {

    private lateinit var binding: FragmentUserMenuTransactionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUserMenuTransactionBinding.inflate(inflater, container, false)

        binding.menuLaundry.setOnClickListener {

        }


        return binding.root
    }

}