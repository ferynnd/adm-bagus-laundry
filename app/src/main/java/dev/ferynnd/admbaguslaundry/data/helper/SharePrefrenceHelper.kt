package dev.ferynnd.admbaguslaundry.data.helper

import android.content.Context
import android.content.SharedPreferences

class SharePrefrenceHelper ( context: Context) {


    private val PREF_NAME = "AppSharePref"
    private val  sharedPref : SharedPreferences
    val editor : SharedPreferences.Editor

    init {
        sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        editor = sharedPref.edit()
    }

    fun put(key : String, value: String) {
        editor.putString(key, value)
            .apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPref.getString(key, defaultValue)
    }


    fun put(key: String, value: Boolean) {
        editor.putBoolean(key,value)
            .apply()
    }

    fun getBoolean(key: String) : Boolean {
        return sharedPref.getBoolean(key, false)
    }

    fun clear() {
        editor.clear()
            .apply()
    }

     fun putSync(key: String, value: String) {
        sharedPref.edit().putString(key, value).commit()
    }


}