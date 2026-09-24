package com.fourbrothers.protector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

object ShareCardBuilder {
    private const val WIDTH = 1080
    private const val PAD = 56f
    private const val RADIUS = 40f

    fun buildPngFile(context: Context, protector: Protector): File {
        val bitmap = render(protector)
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "protector_${protector.id}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    private fun render(protector: Protector): Bitmap {
        val models = protector.mobileModels.ifEmpty { listOf("Not set") }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB7E4DF.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B1220.toInt()
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val modelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B5C56.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE6F5F3.toInt()
            style = Paint.Style.FILL
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 24f
        }

        val contentWidth = WIDTH - PAD * 2
        val titleLines = wrapText(protector.protectorName, namePaint, contentWidth)
        val modelLines = models.map { wrapText(it, modelPaint, contentWidth - 72f) }

        val headerH = 210f
        val nameBlockH = titleLines.size * 62f
        val modelsBlockH = modelLines.sumOf { it.size } * 44f + models.size * 28f + models.size * 18f
        val height = (headerH + 56f + 34f + nameBlockH + 48f + 34f + modelsBlockH + 72f + 40f).toInt()
            .coerceAtLeast(720)

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Soft page background
        canvas.drawColor(0xFFEEF3F7.toInt())

        val card = RectF(28f, 28f, WIDTH - 28f, height - 28f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        canvas.drawRoundRect(card, RADIUS, RADIUS, cardPaint)

        // Shadow-ish border
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFFDCE5EE.toInt()
        }
        canvas.drawRoundRect(card, RADIUS, RADIUS, stroke)

        // Gradient header
        val headerPath = Path().apply {
            addRoundRect(
                RectF(card.left, card.top, card.right, card.top + headerH),
                floatArrayOf(RADIUS, RADIUS, RADIUS, RADIUS, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                card.left, card.top, card.right, card.top + headerH,
                intArrayOf(0xFF0B5C56.toInt(), 0xFF0F766E.toInt(), 0xFF14B8A6.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(headerPath, headerPaint)

        // Decorative circles
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
        canvas.drawCircle(card.right - 80f, card.top + 70f, 110f, glow)
        canvas.drawCircle(card.left + 90f, card.top + headerH - 20f, 70f, glow)

        canvas.drawText("FOUR BROTHERS", card.left + PAD, card.top + 72f, brandPaint)
        canvas.drawText("Protector Card", card.left + PAD, card.top + 130f, titlePaint)

        var y = card.top + headerH + 64f
        canvas.drawText("PROTECTOR", card.left + PAD, y, labelPaint)
        y += 52f
        titleLines.forEach { line ->
            canvas.drawText(line, card.left + PAD, y, namePaint)
            y += 62f
        }

        y += 28f
        canvas.drawText("FITS THESE MOBILE MODELS", card.left + PAD, y, labelPaint)
        y += 36f

        models.forEachIndexed { index, model ->
            val lines = modelLines[index]
            val pillH = lines.size * 44f + 28f
            val pill = RectF(card.left + PAD, y, card.right - PAD, y + pillH)
            canvas.drawRoundRect(pill, 22f, 22f, pillPaint)

            val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F766E.toInt() }
            canvas.drawRoundRect(
                RectF(pill.left, pill.top + 14f, pill.left + 10f, pill.bottom - 14f),
                6f, 6f, accent
            )

            var ty = y + 40f
            lines.forEach { line ->
                canvas.drawText(line, pill.left + 36f, ty, modelPaint)
                ty += 44f
            }
            y += pillH + 18f
        }

        canvas.drawText(
            "Shared from FOUR BROTHERS",
            card.left + PAD,
            card.bottom - 36f,
            footerPaint
        )

        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("-")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val trial = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(trial) <= maxWidth) {
                current = trial
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines.ifEmpty { listOf(text) }
    }
}
