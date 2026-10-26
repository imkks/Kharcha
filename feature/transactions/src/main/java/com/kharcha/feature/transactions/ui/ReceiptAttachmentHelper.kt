package com.kharcha.feature.transactions.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ReceiptAttachmentHelper {
    private const val RECEIPTS_DIR = "receipts"

    fun getReceiptsDir(context: Context): File {
        val dir = File(context.filesDir, RECEIPTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getReceiptFile(context: Context, transactionId: Long): File? {
        val dir = getReceiptsDir(context)
        val prefix = "${transactionId}_"
        val files = dir.listFiles { file ->
            file.name.startsWith(prefix) && file.name.endsWith(".jpg")
        } ?: return null

        return files.maxByOrNull { it.lastModified() }
    }

    fun hasReceipt(context: Context, transactionId: Long): Boolean {
        return getReceiptFile(context, transactionId) != null
    }

    fun saveReceipt(
        context: Context,
        transactionId: Long,
        sourceUri: Uri,
    ): File? {
        return try {
            val dir = getReceiptsDir(context)
            removeReceipt(context, transactionId)

            val timestamp = System.currentTimeMillis()
            val destFile = File(dir, "${transactionId}_$timestamp.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (destFile.exists() && destFile.length() > 0) destFile else null
        } catch (_: Exception) {
            null
        }
    }

    fun removeReceipt(context: Context, transactionId: Long): Boolean {
        val dir = getReceiptsDir(context)
        val prefix = "${transactionId}_"
        val existingFiles = dir.listFiles { file ->
            file.name.startsWith(prefix) && file.name.endsWith(".jpg")
        } ?: return true

        var allDeleted = true
        for (file in existingFiles) {
            if (!file.delete()) {
                allDeleted = false
            }
        }
        return allDeleted
    }

    fun loadBitmap(file: File, sampleSize: Int = 1): Bitmap? {
        return try {
            if (!file.exists()) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (_: Exception) {
            null
        }
    }
}
