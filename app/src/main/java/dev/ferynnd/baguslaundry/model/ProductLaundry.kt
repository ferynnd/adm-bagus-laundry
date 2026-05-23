package dev.ferynnd.baguslaundry.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class ProductLaundry(
    val id_laundry_item: Int? = null,
    val id_branch_laundry_item: Int? = null,
    val name_laundry_item: String?= null,
    val price_laundry_item: BigDecimal? = null,
    val time_laundry_item: String? = null,
    val is_active_laundry_item: IsActiveLaundryItem? = IsActiveLaundryItem.active,
    var isSelected: Boolean? = false,
) : Parcelable

enum class IsActiveLaundryItem {
    active,
    inactive
}