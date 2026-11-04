package com.kharcha.core.common.statement

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object PdfTextExtractor {

    private var initialized = false

    private fun ensureInitialized(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    /**
     * Text of the PDF at [uri]. Bank statements are often password-protected:
     * without the right [password] this throws [PdfPasswordRequiredException].
     * The password is only used in-memory to open the file; it isn't stored.
     */
    fun extractText(context: Context, uri: Uri, password: String? = null): String {
        ensureInitialized(context)

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open PDF at $uri")

        return inputStream.use { stream ->
            val document = try {
                PDDocument.load(stream, password ?: "")
            } catch (_: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
                throw PdfPasswordRequiredException(wrongPassword = password != null)
            }
            document.use { PDFTextStripper().getText(it) }
        }
    }
}

/** The PDF is locked; [wrongPassword] when a password was provided and failed. */
class PdfPasswordRequiredException(val wrongPassword: Boolean) : Exception("PDF is password-protected")
