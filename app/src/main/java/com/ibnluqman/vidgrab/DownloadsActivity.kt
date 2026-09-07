package com.ibnluqman.vidgrab

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File


class DownloadsActivity : AppCompatActivity() {


    private val settings by lazy { Settings(this) }

    private val folderPicker = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settings.storageTreeUri = uri.toString()
            settings.hasAskedStorageLocation = true
            Toast.makeText(this, "Downloads will now save to your chosen folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun maybeAskStorageLocation() {
        if (settings.hasAskedStorageLocation) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Where should downloads be saved?")
            .setMessage("Choose a folder now, or use the default location (your Movies/Music folder).")
            .setPositiveButton("Choose folder") { _, _ -> folderPicker.launch(null) }
            .setNegativeButton("Use default") { _, _ -> settings.hasAskedStorageLocation = true }
            .setCancelable(false)
            .show()
    }
    private lateinit var dao: DownloadDao
    private lateinit var adapter: DownloadsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        dao = AppDatabase.get(applicationContext).downloadDao()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val list = findViewById<RecyclerView>(R.id.downloadsList)
        val emptyState = findViewById<View>(R.id.emptyState)
        val linkInput = findViewById<TextInputEditText>(R.id.linkInput)
        val downloadButton = findViewById<MaterialButton>(R.id.downloadButton)

        adapter = DownloadsAdapter(
            onPauseResume = { record -> togglePauseResume(record) },
            onPlay = { record -> playRecord(record) },
            onDelete = { record -> deleteRecord(record) }
        )
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        downloadButton.setOnClickListener {
            val url = linkInput.text?.toString()?.trim().orEmpty()
            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                Toast.makeText(this, "Paste a valid video link first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                dao.insert(DownloadRecord(url = url, forcedQuality = null))
                val intent = Intent(applicationContext, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_ENQUEUE
                }
                ContextCompat.startForegroundService(applicationContext, intent)
            }
            linkInput.text?.clear()

            lifecycleScope.launch {
                dao.observeAll().collect { records ->
                    emptyState.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                    list.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
                    adapter.submitList(records)
                }
            }

            maybeAskStorageLocation()
        }

        lifecycleScope.launch {
            dao.observeAll().collect { records ->
                emptyState.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                list.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
                adapter.submitList(records)
            }
        }
    }

    private fun togglePauseResume(record: DownloadRecord) {
        val statusEnum = runCatching { DownloadRecord.Status.valueOf(record.status) }.getOrNull()
        val action = if (statusEnum == DownloadRecord.Status.PAUSED || statusEnum == DownloadRecord.Status.FAILED) {
            DownloadService.ACTION_RESUME
        } else {
            DownloadService.ACTION_PAUSE
        }
        val intent = Intent(this, DownloadService::class.java).apply {
            this.action = action
            putExtra(DownloadService.EXTRA_RECORD_ID, record.id)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun playRecord(record: DownloadRecord) {
        val path = record.filePath ?: return
        if (record.mediaType == "PHOTO") {
            try {
                val uri = Uri.parse(path)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No photo viewer app found on this device", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URI, path)
            putExtra(PlayerActivity.EXTRA_TITLE, record.title)
        }
        startActivity(intent)
    }

    private fun deleteRecord(record: DownloadRecord) {
        val intent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_DELETE
            putExtra(DownloadService.EXTRA_RECORD_ID, record.id)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.downloads_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
