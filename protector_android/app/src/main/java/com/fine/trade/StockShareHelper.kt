package com.fine.trade

import android.app.Activity
import android.content.Intent
import android.widget.Toast

object StockShareHelper {
    /** Share all protectors as WhatsApp-friendly plain text (no files). */
    fun shareAllAsTable(activity: Activity, repo: StorageRepository) {
        try {
            val export = DataTableShare.build(repo)
            if (export.protectorCount == 0) {
                Toast.makeText(activity, "No data to share yet", Toast.LENGTH_SHORT).show()
                return
            }

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Fine Trade stock list")
                putExtra(Intent.EXTRA_TEXT, export.whatsappText)
            }
            activity.startActivity(Intent.createChooser(send, "Share via WhatsApp"))
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                e.message ?: "Share failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
