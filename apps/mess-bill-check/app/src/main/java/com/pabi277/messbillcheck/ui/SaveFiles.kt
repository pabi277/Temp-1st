package com.pabi277.messbillcheck.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Writing a file the user can find, with no storage permission and no share-sheet dependency.
 * On API 29+ this goes to the public Downloads collection; below that it lands in the app's own
 * external directory and we tell the user the path instead of pretending.
 */
object SaveFiles {

    data class Saved(val where: String)

    fun save(context: Context, name: String, mime: String, bytes: ByteArray): Saved? {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                saveViaMediaStore(context, name, mime, bytes)
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                dir.mkdirs()
                val target = File(dir, name)
                target.writeBytes(bytes)
                Saved(target.absolutePath)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveViaMediaStore(context: Context, name: String, mime: String, bytes: ByteArray): Saved? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return null
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return Saved("Download/$name")
    }

    /** Text-only PDF, A4 portrait at ~144 dpi, paginating on overflow. No third-party PDF library. */
    fun renderPdf(lines: List<String>): ByteArray {
        val pageW = 1190
        val pageH = 1684
        val margin = 72f
        val leading = 44f
        val doc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 30f
        }
        val bold = Paint(paint).apply { isFakeBoldText = true }
        var page = 1
        var y = margin + 30f
        var info = PdfDocument.PageInfo.Builder(pageW, pageH, page).build()
        var canvas = doc.startPage(info).canvas
        for (raw in lines) {
            val line = raw.replace("\t", "  ")
            if (y > pageH - margin) {
                doc.finishPage(canvas)
                page += 1
                info = PdfDocument.PageInfo.Builder(pageW, pageH, page).build()
                canvas = doc.startPage(info).canvas
                y = margin + 30f
            }
            val isHeading = line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("!") &&
                !line.contains("  ") && line.length < 40
            canvas.drawText(line, margin, y, if (isHeading) bold else paint)
            y += leading
        }
        doc.finishPage(canvas)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
