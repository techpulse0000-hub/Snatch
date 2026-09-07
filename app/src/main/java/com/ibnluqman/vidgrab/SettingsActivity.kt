package com.ibnluqman.vidgrab

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.Manifest
import android.os.Build
import com.ibnluqman.vidgrab.databinding.ActivityMainBinding

class SettingsActivity : AppCompatActivity() {

    private fun currentFolderLabel(): String {
        val uriString = settings.storageTreeUri ?: return "Default (Movies/Snatch)"
        return try {
            val uri = android.net.Uri.parse(uriString)
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            val readable = if (docId.startsWith("primary:")) {
                "Internal storage/${docId.removePrefix("primary:")}"
            } else {
                androidx.documentfile.provider.DocumentFile.fromTreeUri(this, uri)?.name ?: docId
            }
            readable
        } catch (e: Exception) {
            "Custom folder"
        }
    }

    private fun refreshFolderLabel() {
        findViewById<android.widget.TextView>(R.id.currentFolderText).text =
            "Currently saving to: ${currentFolderLabel()}"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: Settings

    private val folderPicker = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settings.storageTreeUri = uri.toString()
            refreshFolderLabel()
            Toast.makeText(this, "Storage folder updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = Settings(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val checkedId = when (settings.quality) {
            Settings.Quality.BEST -> R.id.radioBest
            Settings.Quality.P1080 -> R.id.radio1080
            Settings.Quality.P720 -> R.id.radio720
            Settings.Quality.P480 -> R.id.radio480
            Settings.Quality.AUDIO_ONLY -> R.id.radioAudio
        }
        binding.qualityGroup.check(checkedId)

        binding.qualityGroup.setOnCheckedChangeListener { _, checkedRadioId ->
            settings.quality = when (checkedRadioId) {
                R.id.radioBest -> Settings.Quality.BEST
                R.id.radio1080 -> Settings.Quality.P1080
                R.id.radio720 -> Settings.Quality.P720
                R.id.radio480 -> Settings.Quality.P480
                R.id.radioAudio -> Settings.Quality.AUDIO_ONLY
                else -> Settings.Quality.BEST
            }
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.changeFolderButton)
            .setOnClickListener { folderPicker.launch(null) }
        refreshFolderLabel()

    }
}
