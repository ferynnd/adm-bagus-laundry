package dev.ferynnd.baguslaundry.data.helper

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Helper untuk menyimpan draft transaksi rental
 * Menggunakan SharedPreferences - paling reliable untuk persist data
 */
class TransactionDraftHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "transaction_draft_prefs",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_RENTAL_ITEMS = "draft_rental_items"
        private const val KEY_NOMOR_NOTA = "draft_nomor_nota"
        private const val KEY_CLIENT_NAME = "draft_client_name"
        private const val KEY_CLIENT_ID = "draft_client_id"
        private const val KEY_CLIENT_ADDRESS = "draft_client_address"
        private const val KEY_NAMA_PENERIMA = "draft_nama_penerima"
        private const val KEY_NOTE_TRANSAKSI = "draft_note_transaksi"
    }

    data class RentalItemDraft(
        val selectedItemId: Int = 0,
        val selectedItemPosition: Int = 0,
        val selectedItemName: String = "",
        val status: String = "",
        val kondisi: String = "",
        val berat: String = "",
        val pcs: String = ""
    )

    // Save rental items
    fun saveRentalItems(items: List<RentalItemDraft>) {
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_RENTAL_ITEMS, json).apply()
    }

    // Get rental items
    fun getRentalItems(): List<RentalItemDraft> {
        val json = prefs.getString(KEY_RENTAL_ITEMS, null)
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<RentalItemDraft>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Save form fields
    fun saveNomorNota(value: String) {
        prefs.edit().putString(KEY_NOMOR_NOTA, value).apply()
    }

    fun getNomorNota(): String {
        return prefs.getString(KEY_NOMOR_NOTA, "") ?: ""
    }

    fun saveClient(name: String, id: Int, address: String) {
        prefs.edit()
            .putString(KEY_CLIENT_NAME, name)
            .putInt(KEY_CLIENT_ID, id)
            .putString(KEY_CLIENT_ADDRESS, address)
            .apply()
    }

    fun getClientName(): String {
        return prefs.getString(KEY_CLIENT_NAME, "") ?: ""
    }

    fun getClientId(): Int {
        return prefs.getInt(KEY_CLIENT_ID, 0)
    }

    fun getClientAddress(): String {
        return prefs.getString(KEY_CLIENT_ADDRESS, "") ?: ""
    }

    fun saveNamaPenerima(value: String) {
        prefs.edit().putString(KEY_NAMA_PENERIMA, value).apply()
    }

    fun getNamaPenerima(): String {
        return prefs.getString(KEY_NAMA_PENERIMA, "") ?: ""
    }

    fun saveNoteTransaksi(value: String) {
        prefs.edit().putString(KEY_NOTE_TRANSAKSI, value).apply()
    }

    fun getNoteTransaksi(): String {
        return prefs.getString(KEY_NOTE_TRANSAKSI, "") ?: ""
    }

    // Check if has data
    fun hasUnsavedData(): Boolean {
        return getRentalItems().isNotEmpty() ||
                getNomorNota().isNotEmpty() ||
                getClientName().isNotEmpty() ||
                getNamaPenerima().isNotEmpty() ||
                getNoteTransaksi().isNotEmpty()
    }

    // Clear all draft data
    fun clearAllData() {
        prefs.edit()
            .remove(KEY_RENTAL_ITEMS)
            .remove(KEY_NOMOR_NOTA)
            .remove(KEY_CLIENT_NAME)
            .remove(KEY_CLIENT_ID)
            .remove(KEY_CLIENT_ADDRESS)
            .remove(KEY_NAMA_PENERIMA)
            .remove(KEY_NOTE_TRANSAKSI)
            .apply()
    }
}