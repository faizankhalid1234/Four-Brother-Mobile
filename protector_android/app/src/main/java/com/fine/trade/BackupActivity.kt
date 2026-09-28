package com.fine.trade

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.fine.trade.databinding.ActivityBackupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupBinding
    private lateinit var repo: StorageRepository
    private val cloud = CloudSyncClient()

    private val pickJson = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers do not support persistable grants; openInputStream still works.
        }
        importLocalUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiCompat.setupActivityWindow(this)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = StorageRepository(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        refreshStatus()

        binding.btnExportDrive.setOnClickListener { exportDrive() }
        binding.btnImportDrive.setOnClickListener { fetchDriveForReview() }
        binding.btnExportPhone.setOnClickListener { exportPhoneJson() }
        binding.btnShareTable.setOnClickListener {
            StockShareHelper.shareAllAsTable(this, repo)
        }
        binding.btnImportPhone.setOnClickListener {
            pickJson.launch(arrayOf("application/json", "text/*", "*/*"))
        }
        binding.btnLogout.setOnClickListener {
            // Also clear cached Google account so picker shows next time
            runCatching {
                val webClientId = getString(R.string.google_web_client_id)
                if (!webClientId.startsWith("REPLACE_")) {
                    val options = com.google.android.gms.auth.api.signin.GoogleSignInOptions
                        .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getClient(this, options)
                        .signOut()
                }
            }
            cloud.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    private fun refreshStatus() {
        val user = cloud.currentUser
        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        binding.accountStatus.text = "Signed in as ${cloud.displayLabel()}"
        binding.storageHint.text = repo.storageLocationHint()
        binding.versionLabel.text = UiCompat.appVersionText(this)
    }

    private enum class BusyAction { NONE, EXPORT_DRIVE, IMPORT_DRIVE, OTHER }

    private fun setBusy(action: BusyAction) {
        val busy = action != BusyAction.NONE
        binding.progress.visibility =
            if (action == BusyAction.OTHER) View.VISIBLE else View.GONE

        binding.exportDriveProgress.visibility =
            if (action == BusyAction.EXPORT_DRIVE) View.VISIBLE else View.GONE
        binding.btnExportDrive.text =
            if (action == BusyAction.EXPORT_DRIVE) "" else "Export to Google Drive"
        binding.btnExportDrive.isEnabled = !busy

        binding.importDriveProgress.visibility =
            if (action == BusyAction.IMPORT_DRIVE) View.VISIBLE else View.GONE
        binding.btnImportDrive.text =
            if (action == BusyAction.IMPORT_DRIVE) "" else "Import from Google Drive"
        binding.btnImportDrive.isEnabled = !busy

        binding.btnExportPhone.isEnabled = !busy
        binding.btnShareTable.isEnabled = !busy
        binding.btnImportPhone.isEnabled = !busy
        binding.btnLogout.isEnabled = !busy
    }

    private fun exportDrive() {
        setBusy(BusyAction.EXPORT_DRIVE)
        lifecycleScope.launch {
            try {
                val json = repo.exportJsonText()
                withContext(Dispatchers.IO) { cloud.exportToDrive(json) }
                Toast.makeText(
                    this@BackupActivity,
                    "Exported to Google Drive",
                    Toast.LENGTH_LONG
                ).show()
                savePhoneCopy(json)
            } catch (e: Exception) {
                Toast.makeText(
                    this@BackupActivity,
                    e.message ?: "Export failed",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setBusy(BusyAction.NONE)
            }
        }
    }

    /** Download Drive backup, show review, import only after Approve. */
    private fun fetchDriveForReview() {
        setBusy(BusyAction.IMPORT_DRIVE)
        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { cloud.importFromDrive() }
                val preview = repo.previewImport(json)
                setBusy(BusyAction.NONE)
                showImportReview(
                    title = "Review Drive backup",
                    preview = preview,
                    sourceLabel = "Drive"
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@BackupActivity,
                    e.message ?: "Could not load Drive backup",
                    Toast.LENGTH_LONG
                ).show()
                setBusy(BusyAction.NONE)
            }
        }
    }

    private fun showImportReview(
        title: String,
        preview: StorageRepository.ImportPreview,
        sourceLabel: String
    ) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val textView = TextView(this).apply {
            text = preview.summary
            textSize = 14f
            setPadding(pad, pad / 2, pad, pad)
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(textView)
            setPadding(pad / 2, 0, pad / 2, 0)
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Approve & Import") { _, _ ->
                applyApprovedImport(preview, sourceLabel)
            }
            .show()
    }

    private fun applyApprovedImport(
        preview: StorageRepository.ImportPreview,
        sourceLabel: String
    ) {
        try {
            val count = repo.importJsonText(preview.normalizedJson)
            Toast.makeText(
                this,
                "Imported $count protectors from $sourceLabel",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                e.message ?: "Import failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun exportPhoneJson() {
        try {
            val json = repo.exportJsonText()
            val file = savePhoneCopy(json)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Fine Trade backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Export JSON"))
            Toast.makeText(this, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Export failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun savePhoneCopy(json: String): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = getExternalFilesDir(null) ?: filesDir
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "fine_trade_backup_$stamp.json")
        file.writeText(json, Charsets.UTF_8)
        return file
    }

    private fun importLocalUri(uri: Uri) {
        setBusy(BusyAction.OTHER)
        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read file")
                }
                val preview = repo.previewImport(text)
                setBusy(BusyAction.NONE)
                showImportReview(
                    title = "Review JSON file",
                    preview = preview,
                    sourceLabel = "file"
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@BackupActivity,
                    e.message ?: "Could not read file",
                    Toast.LENGTH_LONG
                ).show()
                setBusy(BusyAction.NONE)
            }
        }
    }
}
