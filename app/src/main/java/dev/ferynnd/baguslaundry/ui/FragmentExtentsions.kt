package dev.ferynnd.baguslaundry.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import dev.ferynnd.baguslaundry.ui.admin.AdminActivity
import dev.ferynnd.baguslaundry.ui.user.UserActivity

@RequiresApi(Build.VERSION_CODES.O)
fun Fragment.openAdminFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
    (requireActivity() as? AdminActivity)?.openFragment(fragment, tag, addToBackStack)
}

@RequiresApi(Build.VERSION_CODES.O)
fun Fragment.openUserFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
    (requireActivity() as? UserActivity)?.openFragment(fragment, tag, addToBackStack)
}
