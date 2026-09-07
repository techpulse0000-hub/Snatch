package com.ibnluqman.vidgrab

import android.content.Context

class Settings(context: Context) {

    enum class Quality { BEST, P1080, P720, P480, AUDIO_ONLY }

    private val prefs = context.getSharedPreferences("vidgrab_prefs", Context.MODE_PRIVATE)

    var quality: Quality
        get() = Quality.valueOf(prefs.getString(KEY_QUALITY, Quality.BEST.name)!!)
        set(value) = prefs.edit().putString(KEY_QUALITY, value.name).apply()

    var lastUpdateCheckMillis: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    var storageTreeUri: String?
        get() = prefs.getString(KEY_STORAGE_URI, null)
        set(value) = prefs.edit().putString(KEY_STORAGE_URI, value).apply()

    var hasAskedStorageLocation: Boolean
        get() = prefs.getBoolean(KEY_ASKED_STORAGE, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED_STORAGE, value).apply()

    companion object {
        private const val KEY_QUALITY = "quality"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_STORAGE_URI = "storage_tree_uri"
        private const val KEY_ASKED_STORAGE = "asked_storage_location"
    }
}
