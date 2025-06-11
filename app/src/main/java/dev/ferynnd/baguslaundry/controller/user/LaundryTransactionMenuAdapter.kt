package dev.ferynnd.baguslaundry.controller.user

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.databinding.CardItemDetailTransactionLaundryBinding
import dev.ferynnd.baguslaundry.model.ProductLaundry

class LaundryTransactionMenuAdapter :
    ListAdapter<ProductLaundry, LaundryTransactionMenuAdapter.MenuProductLaundryViewHolder>(MenuProductLaundryDiffCallback()) {

        // Tambahkan variabel untuk menyimpan TextWatcher agar bisa dihapus nanti
    private var currentTextWatcher: TextWatcher? = null

    inner class MenuProductLaundryViewHolder(val binding: CardItemDetailTransactionLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuProductLaundryViewHolder {
        val binding = CardItemDetailTransactionLaundryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuProductLaundryViewHolder(binding)
    }


    override fun onBindViewHolder(holder: MenuProductLaundryViewHolder, position: Int) {
        val product = getItem(position)
        holder.binding.apply {
            textName.text = product.name_laundry_item
            textPrice.text = product.price_laundry_item.toString()
            textWeightItem.setText(product.weight?.toString() ?: "")

            // Hapus TextWatcher sebelumnya jika ada
            currentTextWatcher?.let { textWeightItem.removeTextChangedListener(it) }

            // Buat TextWatcher baru
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val input = s?.toString()?.toFloatOrNull()
                    product.weight = input
                }
            }

            // Tambahkan TextWatcher dan simpan referensinya
            textWeightItem.addTextChangedListener(watcher)
            currentTextWatcher = watcher
        }
    }

}

class MenuProductLaundryDiffCallback : DiffUtil.ItemCallback<ProductLaundry>() {
    override fun areItemsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
        return oldItem.id_laundry_item == newItem.id_laundry_item
    }

    override fun areContentsTheSame(oldItem: ProductLaundry, newItem: ProductLaundry): Boolean {
        return oldItem == newItem
    }
}
