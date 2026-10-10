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

    /**
     * Text-only PDF, A4 portrait in PostScript points (1/72"), paginating on overflow. No
     * third-party PDF library, so there is no extra licence or Data Safety line to keep honest.
     * The framework builder is `create()`, and `finishPage` takes the Page, not its Canvas.
     */
    fun renderPdf(lines: List<String>): ByteArray {
        val pageW = 595
        val pageH = 842
        val margin = 36f
        val leading = 15f
        val doc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
        }
        val bold = Paint(paint).apply {
            isFakeBoldText = true
            textSize = 11.5f
        }
        val small = Paint(paint).apply {
            color = Color.DKGRAY
            textSize = 8.5f
        }
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
        var y = margin + 12f
        for (raw in lines) {
            val line = raw.replace("\t", "  ")
            if (y > pageH - margin) {
                doc.finishPage(page)
                pageNumber += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
                y = margin + 12f
            }
            val paint = when {
                line.startsWith("!") -> small
                line.startsWith(" ") -> small
                line.isBlank() -> paint
                line.length < 40 && !line.contains("  ") -> bold
                else -> paint
            }
            page.canvas.drawText(line, margin, y, paint)
            y += leading
        }
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
