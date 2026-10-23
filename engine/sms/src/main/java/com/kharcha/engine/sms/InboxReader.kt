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
        suspend fun readSmsBatch(
            lastProcessedId: Long,
            batchSize: Int,
        ): List<RawSms> =
            withContext(Dispatchers.IO) {
                val result = mutableListOf<RawSms>()
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "body", "date", "sub_id")
                val selection = "_id > ?"
                val selectionArgs = arrayOf(lastProcessedId.toString())
                val sortOrder = "_id ASC LIMIT $batchSize"

                var cursor: Cursor? = null
                try {
                    cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                    cursor?.let {
                        val idIndex = it.getColumnIndex("_id")
                        val addressIndex = it.getColumnIndex("address")
                        val bodyIndex = it.getColumnIndex("body")
                        val dateIndex = it.getColumnIndex("date")
                        val subIdIndex = it.getColumnIndex("sub_id")

                        while (it.moveToNext()) {
                            val id = if (idIndex != -1) it.getLong(idIndex) else 0L
                            val address = if (addressIndex != -1) it.getString(addressIndex) ?: "" else ""
                            val body = if (bodyIndex != -1) it.getString(bodyIndex) ?: "" else ""
                            val date = if (dateIndex != -1) it.getLong(dateIndex) else 0L
                            val subId = if (subIdIndex != -1) it.getInt(subIdIndex) else null

                            result.add(RawSms(id, address, body, date, subId))
                        }
                    }
                } finally {
                    cursor?.close()
                }
                result
            }
    }
