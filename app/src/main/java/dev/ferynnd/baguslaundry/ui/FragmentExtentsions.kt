package dev.ferynnd.baguslaundry.ui

import androidx.fragment.app.Fragment
import dev.ferynnd.baguslaundry.ui.admin.AdminActivity
import dev.ferynnd.baguslaundry.ui.user.UserActivity

fun Fragment.openAdminFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
    (requireActivity() as? AdminActivity)?.openFragment(fragment, tag, addToBackStack)
}

fun Fragment.openUserFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
    (requireActivity() as? UserActivity)?.openFragment(fragment, tag, addToBackStack)
}
