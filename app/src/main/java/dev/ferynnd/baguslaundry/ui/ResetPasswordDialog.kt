package dev.ferynnd.baguslaundry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dev.ferynnd.admbaguslaundry.R


class ResetPasswordDialog(
    private val onSubmit: (current: String, new: String, confirm: String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_change_password, container, false)

        val inputCurrent = view.findViewById<TextInputEditText>(R.id.inputCurrentPassword)
        val inputNew = view.findViewById<TextInputEditText>(R.id.inputNewPassword)
        val inputConfirm = view.findViewById<TextInputEditText>(R.id.inputConfirmPassword)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelReset)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmReset)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnConfirm.setOnClickListener {
            val current = inputCurrent.text.toString().trim()
            val newPass = inputNew.text.toString().trim()
            val confirm = inputConfirm.text.toString().trim()

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                showAlert(
                    title = "Perhatian!",
                    message = "Semua field wajib diisi",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.info
                )
                return@setOnClickListener
            }

            if (newPass != confirm) {
                 showAlert(
                    title = "Gagal!",
                    message = "Konfirmasi password tidak cocok",
                    backgroundColorRes = R.color.primary,
                    iconRes = R.drawable.failed
                )
                return@setOnClickListener
            }

            onSubmit(current, newPass, confirm)
            dismiss()
        }

        return view
    }
}
