package com.fine.trade

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds WhatsApp-friendly plain-text stock lists (no files).
 */
object DataTableShare {

    data class Export(
        val whatsappText: String,
        val protectorCount: Int
    )

    fun build(repo: StorageRepository): Export {
        val protectors = repo.loadProtectors().sortedBy { it.protectorName.lowercase() }
        return Export(
            whatsappText = buildWhatsAppText(protectors),
            protectorCount = protectors.size
        )
    }

    /** WhatsApp formatting: *bold*, plain lines, no attachments. */
    fun buildWhatsAppText(protectors: List<Protector>): String {
        val whenText = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("*Fine Trade — Stock List*")
            appendLine(whenText)
            appendLine()
            appendLine("*PROTECTORS (${protectors.size})*")
            appendLine("────────────────────")
            if (protectors.isEmpty()) {
                appendLine("_No protectors yet_")
            } else {
                protectors.forEachIndexed { index, p ->
                    appendLine()
                    appendLine("*${index + 1}. ${p.protectorName}*")
                    val models = p.mobileModels.joinToString(", ").ifBlank { "-" }
                    appendLine("Models: $models")
                }
            }
            appendLine()
            appendLine("────────────────────")
            appendLine("_Shared from Fine Trade_")
        }.trimEnd()
    }
}
