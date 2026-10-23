package com.kharcha.engine.sms

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import com.kharcha.core.model.RawSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InboxReader
    @Inject
    constructor(private val contentResolver: ContentResolver) {
        @Suppress("TooGenericExceptionCaught")
        suspend fun getSmsCount(sinceTimestamp: Long = 0L): Int =
            withContext(Dispatchers.IO) {
                val uri = Uri.parse("content://sms/inbox")
                val (selection, selectionArgs) =
                    if (sinceTimestamp > 0L) {
                        Pair("date >= ?", arrayOf(sinceTimestamp.toString()))
                    } else {
                        Pair(null, null)
                    }

                var cursor: Cursor? = null
                try {
                    cursor = contentResolver.query(uri, arrayOf("_id"), selection, selectionArgs, null)
                    cursor?.count ?: 0
                } catch (e: Exception) {
                    0
                } finally {
                    cursor?.close()
                }
            }

        @Suppress("TooGenericExceptionCaught")
        suspend fun readSmsBatch(
            lastProcessedId: Long,
            batchSize: Int,
            sinceTimestamp: Long = 0L,
        ): List<RawSms> =
            withContext(Dispatchers.IO) {
                val result = mutableListOf<RawSms>()
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "body", "date")
                val (selection, selectionArgs) =
                    if (sinceTimestamp > 0L) {
                        Pair(
                            "_id > ? AND date >= ?",
                            arrayOf(lastProcessedId.toString(), sinceTimestamp.toString()),
                        )
                    } else {
                        Pair("_id > ?", arrayOf(lastProcessedId.toString()))
                    }
                val sortOrder = "_id ASC"

                var cursor: Cursor? = null
                try {
                    cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                    cursor?.let {
                        val idIndex = it.getColumnIndex("_id")
                        val addressIndex = it.getColumnIndex("address")
                        val bodyIndex = it.getColumnIndex("body")
                        val dateIndex = it.getColumnIndex("date")

                        while (it.moveToNext() && result.size < batchSize) {
                            val id = if (idIndex != -1) it.getLong(idIndex) else 0L
                            val address = if (addressIndex != -1) it.getString(addressIndex).orEmpty() else ""
                            val body = if (bodyIndex != -1) it.getString(bodyIndex).orEmpty() else ""
                            val date = if (dateIndex != -1) it.getLong(dateIndex) else 0L

                            result.add(RawSms(id, address, body, date, null))
                        }
                    }
                } catch (e: Exception) {
                    // Return collected items on failure
                } finally {
                    cursor?.close()
                }
                result
            }
    }
