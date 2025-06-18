package dev.ferynnd.baguslaundry.controller.user

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardItemDetailTransactionLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class LaundryTransactionMenuAdapter :
    ListAdapter<ProductLaundry, LaundryTransactionMenuAdapter.MenuProductLaundryViewHolder>(
        MenuProductLaundryDiffCallback()
    ) {

    // 1. Definisikan interface listener
    interface OnItemWeightChangeListener {
        fun onWeightChanged()
    }

    // 2. Variabel untuk menyimpan listener
    private var itemWeightChangeListener: OnItemWeightChangeListener? = null

    // 3. Setter untuk listener
    fun setOnItemWeightChangeListener(listener: OnItemWeightChangeListener) {
        this.itemWeightChangeListener = listener
    }

    // Tambahkan formatter mata uang di adapter juga untuk konsistensi tampilan
    private val numberFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            isGroupingUsed = true
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

    inner class MenuProductLaundryViewHolder(val binding: CardItemDetailTransactionLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MenuProductLaundryViewHolder {
        val binding = CardItemDetailTransactionLaundryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuProductLaundryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuProductLaundryViewHolder, position: Int) {
        val product = getItem(position)
        holder.binding.apply {
            textName.text = product.name_laundry_item
            textPrice.text = numberFormatter.format(product.price_laundry_item ?: 0.0)

//            val currentWeightText = product.weight?.toString() ?: ""
//            if (textWeightItem.text.toString() != currentWeightText) {
//                textWeightItem.setText(currentWeightText)
//            }
//
//            textWeightItem.tag?.let {
//                if (it is TextWatcher) {
//                    textWeightItem.removeTextChangedListener(it)
//                }
//            }

            var isEditing = false
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    isEditing = true
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (!isEditing) return
                    isEditing = false
                    val input = s?.toString()?.toBigDecimalOrNull()
                    product.weight = input ?: BigDecimal.ZERO
                    itemWeightChangeListener?.onWeightChanged()
                }
            }

//            textWeightItem.addTextChangedListener(watcher)
//            textWeightItem.tag = watcher
        }
    }


//    override fun onBindViewHolder(holder: MenuProductLaundryViewHolder, position: Int) {
//        val product = getItem(position)
//        holder.binding.apply {
//            textName.text = product.name_laundry_item
//            textPrice.text = numberFormatter.format(product.price_laundry_item ?: 0.0)
//            textWeightItem.setText(product.weight?.toString() ?: "")
//
//            textWeightItem.tag?.let {
//                if (it is TextWatcher) {
//                    textWeightItem.removeTextChangedListener(it)
//                }
//            }
//
//            // Buat TextWatcher baru
//            val watcher = object : TextWatcher {
//                override fun beforeTextChanged(
//                    s: CharSequence?,
//                    start: Int,
//                    count: Int,
//                    after: Int
//                ) {
//                }
//
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//                override fun afterTextChanged(s: Editable?) {
//                    val input = s?.toString()?.toBigDecimalOrNull()
//                    if (input != null) {
//                        product.weight = input
//                    } else if (s.isNullOrEmpty()) {
//                        product.weight = BigDecimal.ZERO // Atau null, tergantung kebutuhan default
//                    } else {
//                        Log.e("WeightInput", "Input berat tidak valid: ${s.toString()}")
//                        product.weight = BigDecimal.ZERO // Default ke 0 jika tidak valid
//                    }
//                    itemWeightChangeListener?.onWeightChanged()
//                }
//            }
//            textWeightItem.addTextChangedListener(watcher)
//            textWeightItem.tag = watcher
//        }
//    }

}

class MenuProductLaundryDiffCallback : DiffUtil.ItemCallback<ProductLaundry>() {
    override fun areItemsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
        return oldItem.id_laundry_item == newItem.id_laundry_item
    }

    override fun areContentsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
        // Ini harus membandingkan semua properti yang relevan untuk menentukan perubahan konten.
        // Jika hanya membandingkan id, perubahan berat tidak akan terdeteksi.
        return oldItem.id_laundry_item == newItem.id_laundry_item &&
                oldItem.name_laundry_item == newItem.name_laundry_item &&
                oldItem.price_laundry_item == newItem.price_laundry_item &&
                oldItem.weight == newItem.weight
    }
}