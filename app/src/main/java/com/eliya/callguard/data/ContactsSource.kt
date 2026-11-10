package com.eliya.callguard.data

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone

data class DeviceContact(
    val id: Long,
    val name: String,
    val numbers: List<String>,
    val photoUri: String?,
)

object ContactsSource {

    /** Reads all contacts that have at least one phone number. Requires READ_CONTACTS. */
    fun load(context: Context): List<DeviceContact> {
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.NUMBER,
            Phone.PHOTO_THUMBNAIL_URI,
        )
        val byId = LinkedHashMap<Long, DeviceContact>()
        context.contentResolver.query(
            Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(Phone.CONTACT_ID)
            val nameCol = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME_PRIMARY)
            val numCol = c.getColumnIndexOrThrow(Phone.NUMBER)
            val photoCol = c.getColumnIndexOrThrow(Phone.PHOTO_THUMBNAIL_URI)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val number = c.getString(numCol)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val existing = byId[id]
                if (existing == null) {
                    byId[id] = DeviceContact(
                        id = id,
                        name = c.getString(nameCol)?.takeIf { it.isNotBlank() } ?: number,
                        numbers = listOf(number),
                        photoUri = c.getString(photoCol),
                    )
                } else if (existing.numbers.none { PhoneNumbers.key(it) == PhoneNumbers.key(number) }) {
                    byId[id] = existing.copy(numbers = existing.numbers + number)
                }
            }
        }
        return byId.values.toList()
    }
}
