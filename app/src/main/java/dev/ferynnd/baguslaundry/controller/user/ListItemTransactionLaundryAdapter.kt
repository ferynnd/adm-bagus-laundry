package dev.ferynnd.baguslaundry.controller.user

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.ferynnd.baguslaundry.R
import dev.ferynnd.baguslaundry.model.ProductLaundry
import java.text.NumberFormat
import java.util.Locale

class ListItemTransactionLaundryAdapter (private val listener: OnItemClickListener // Listener untuk klik item dan long klik
) : RecyclerView.Adapter<ListItemTransactionLaundryAdapter.ProductLaundryViewHolder>() {

    private val items = mutableListOf<ProductLaundry>()
    private val selectedItems = mutableListOf<ProductLaundry>()

    // Interface untuk komunikasi dengan Fragment
    interface OnItemClickListener {
        fun onItemClick(item: ProductLaundry, position: Int)
        fun onSelectionChanged(selectedCount: Int)
    }

    inner class ProductLaundryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.textName)
        val itemDate: TextView = itemView.findViewById(R.id.textDate)
        val itemPrice: TextView = itemView.findViewById(R.id.textPrice)
        val itemLayout : CardView = itemView.findViewById(R.id.cardForeground)

        fun bind(item: ProductLaundry) {
            itemName.text = item.name_laundry_item
            itemDate.text = item.time_laundry_item

            val localeID = Locale("in", "ID")
            val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
            val harga = item.price_laundry_item
            itemPrice.text = formatRupiah.format(harga)

            if (item.isSelected == true) {
                itemLayout.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.selection))
            } else {
                itemLayout.setCardBackgroundColor(Color.WHITE)
            }

            itemLayout.setOnClickListener {
                listener.onItemClick(item, adapterPosition)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductLaundryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item_transaction_laundry, parent, false)
        return ProductLaundryViewHolder(view)
    }


    override fun onBindViewHolder(holder: ProductLaundryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<ProductLaundry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }


    fun toggleSelection(item: ProductLaundry) {
        item.isSelected = !item.isSelected!!
        if (item.isSelected == true) {
            selectedItems.add(item)
        } else {
            selectedItems.remove(item)
        }
        notifyItemChanged(items.indexOf(item))
        listener.onSelectionChanged(selectedItems.size)
    }

    fun getSelectedItems(): List<ProductLaundry> {
        return selectedItems
    }

}