package com.fine.trade

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a shareable HTML stock table of protectors.
 */
object DataTableShare {

    data class Export(
        val htmlFile: File,
        val protectorCount: Int
    )

    fun build(context: Context, repo: StorageRepository): Export {
        val protectors = repo.loadProtectors().sortedBy { it.protectorName.lowercase() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }

        val htmlFile = File(dir, "FineTrade_stock_$stamp.html")
        htmlFile.writeText(buildHtml(protectors), Charsets.UTF_8)

        return Export(
            htmlFile = htmlFile,
            protectorCount = protectors.size
        )
    }

    fun buildHtml(protectors: List<Protector>): String {
        fun esc(s: String) = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        return buildString {
            appendLine("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>")
            appendLine("<title>Fine Trade Stock</title>")
            appendLine(
                """
                <style>
                  body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;margin:24px;color:#0B1220;background:#F4FBF9}
                  h1{color:#0F766E;margin:0 0 4px}
                  .sub{color:#64748B;margin-bottom:20px}
                  table{border-collapse:collapse;width:100%;background:#fff;margin-bottom:28px;box-shadow:0 1px 3px rgba(0,0,0,.06)}
                  th,td{border:1px solid #DCE5EE;padding:10px 12px;text-align:left;vertical-align:top}
                  th{background:#0F766E;color:#fff}
                  tr:nth-child(even) td{background:#E6F5F3}
                  .num{width:48px;text-align:center}
                </style>
                """.trimIndent()
            )
            appendLine("</head><body>")
            appendLine("<h1>Fine Trade</h1>")
            appendLine(
                "<p class=\"sub\">Stock table · ${
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                }</p>"
            )
            appendLine("<h2>Protectors (${protectors.size})</h2>")
            appendLine("<table><thead><tr><th class=\"num\">#</th><th>Protector</th><th>Mobile Models</th></tr></thead><tbody>")
            if (protectors.isEmpty()) {
                appendLine("<tr><td colspan=\"3\">No protectors</td></tr>")
            } else {
                protectors.forEachIndexed { i, p ->
                    append("<tr><td class=\"num\">${i + 1}</td><td>${esc(p.protectorName)}</td><td>")
                    append(esc(p.mobileModels.joinToString(", ").ifBlank { "-" }))
                    appendLine("</td></tr>")
                }
            }
            appendLine("</tbody></table></body></html>")
        }
    }
}
