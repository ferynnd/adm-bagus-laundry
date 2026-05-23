package dev.ferynnd.admbaguslaundry.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import dev.ferynnd.admbaguslaundry.ui.admin.AdminActivity

@RequiresApi(Build.VERSION_CODES.O)
fun Fragment.openAdminFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
    (requireActivity() as? AdminActivity)?.openFragment(fragment, tag, addToBackStack)
}
