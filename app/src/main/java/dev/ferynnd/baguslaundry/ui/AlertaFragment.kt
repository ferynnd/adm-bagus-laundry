package dev.ferynnd.baguslaundry.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tapadoo.alerter.Alerter
import dev.ferynnd.baguslaundry.R

fun Fragment.showAlert(
    title: String,
    message: String,
    duration: Long = 3000,
    backgroundColorRes: Int = R.color.primary,
    iconRes: Int? = null
) {
    val alerter = Alerter.create(requireActivity())
        .setTitle(title)
        .setText(message)
        .setBackgroundColorRes(backgroundColorRes)
        .setDuration(duration)

    iconRes?.let {
        alerter.setIcon(it)
            .setIconColorFilter(0)
    }

    alerter.show()
}

//Panggilnya
//// Contoh sukses
//showAlert(
//    title = "Cabang berhasil dibuat!",
//    message = "Cabang ${dataBranch.name_branch} berhasil ditambahkan",
//    iconRes = R.drawable.success
//)
//
//// Contoh error
//showAlert(
//    title = "Gagal membuat cabang",
//    message = "Terjadi kesalahan: ${e.message}",
//    backgroundColorRes = R.color.red600,
//    iconRes = R.drawable.failed,
//    duration = 5000
//)
//

fun Fragment.showConfirmationAlert(
    title: String,
    message: String,
    confirmText: String = "OK",
    cancelText: String = "BATAL",
    duration: Long = 10000,
    backgroundColorRes: Int = R.color.red600,
    onConfirm: () -> Unit
) {
    val alerter = Alerter.create(requireActivity())
        .setTitle(title)
        .setText(message)
        .setBackgroundColorRes(backgroundColorRes)
        .setDuration(duration)
        .addButton(confirmText, R.style.AlertButtonDelete) {
            onConfirm()
            Alerter.hide()
        }
        .addButton(cancelText, R.style.AlertButtonCancel) {
            Alerter.hide()
        }

    alerter.show()
}

//showConfirmationAlert(
//    title = "Konfirmasi Hapus Cabang",
//    message = "Cabang ${branch.name_branch} akan dihapus secara permanen.",
//    confirmText = "HAPUS",
//    cancelText = "BATAL",
//) {
//    // Ini kode yang dijalankan saat tekan tombol HAPUS
//    lifecycleScope.launch {
//        try {
//            branchViewModel.deleteBranch(branch)
//            showAlert(
//                title = "Cabang berhasil dihapus!",
//                message = "Cabang ${branch.name_branch} berhasil dihapus",
//                iconRes = R.drawable.success
//            )
//        } catch (e: Exception) {
//            showAlert(
//                title = "Gagal menghapus cabang",
//                message = "Terjadi kesalahan: ${e.message}",
//                backgroundColorRes = android.R.color.holo_red_dark,
//                iconRes = R.drawable.failed,
//                duration = 5000
//            )
//        }
//    }
//}
