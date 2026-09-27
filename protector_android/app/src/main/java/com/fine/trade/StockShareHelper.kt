package com.fine.trade

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider

object StockShareHelper {
    /** Share protectors as a single HTML table file. */
    fun shareAllAsTable(activity: Activity, repo: StorageRepository) {
        try {
            val export = DataTableShare.build(activity, repo)
            if (export.protectorCount == 0) {
                Toast.makeText(activity, "No data to share yet", Toast.LENGTH_SHORT).show()
                return
            }

            val htmlUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                export.htmlFile
            )

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, htmlUri)
                putExtra(Intent.EXTRA_SUBJECT, "Fine Trade stock table")
                clipData = ClipData.newUri(activity.contentResolver, "Stock HTML", htmlUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(send, "Share HTML table"))
            Toast.makeText(
                activity,
                "Sharing ${export.protectorCount} protectors as HTML table",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                e.message ?: "Share failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
