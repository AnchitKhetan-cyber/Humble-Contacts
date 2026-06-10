package com.humblesolutions.humblecontacts.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.humblesolutions.humblecontacts.data.model.Contact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ContactExporter {

    private fun csv(value: Any?): String {
        return "\"${value?.toString()?.replace("\"", "\"\"") ?: ""}\""
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun exportToCsv(
        context: Context,
        contacts: List<Contact>
    ): Boolean {

        return try {

            val formatter = SimpleDateFormat(
                "dd MMM yyyy HH:mm",
                Locale.getDefault()
            )

            val timestamp = SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())

            val fileName =
                "HumbleContacts_Export_$timestamp.csv"

            val values = ContentValues().apply {

                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.Downloads.MIME_TYPE,
                    "text/csv"
                )

                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                            "/Humble Contacts"
                )
            }

            val resolver = context.contentResolver

            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: return false

            resolver.openOutputStream(uri)?.use { stream ->

                stream.bufferedWriter().use { writer ->

                    writer.appendLine("HUMBLE CONTACTS EXPORT")
                    writer.appendLine(
                        "Generated On,${
                            formatter.format(Date())
                        }"
                    )
                    writer.appendLine(
                        "Total Contacts,${contacts.size}"
                    )
                    writer.appendLine(
                        "Exported By,Humble Contacts"
                    )
                    writer.appendLine()

                    writer.appendLine(
                        "Full Name,Job Role,Company,Industry," +
                                "Email,Phone,Tags,Meeting Location," +
                                "Event Name,Notes,Created At"
                    )

                    contacts.forEach { contact ->

                        val meetingDate = contact.meetingDate
                            ?.toDate()
                            ?.let { formatter.format(it) }
                            ?: ""

                        val createdAt = contact.createdAt
                            ?.toDate()
                            ?.let { formatter.format(it) }
                            ?: ""

                        val updatedAt = contact.updatedAt
                            ?.toDate()
                            ?.let { formatter.format(it) }
                            ?: ""

                        writer.appendLine(
                            listOf(
                                csv(contact.fullName),
                                csv(contact.jobRole),
                                csv(contact.company),
                                csv(contact.industry),
                                csv(contact.email),
                                csv(contact.phone),
                                csv(contact.tags.joinToString("; ")),
                                csv(contact.meetingLocation),
                                csv(contact.eventName),
                                csv(contact.conversationNotes.replace("\n", " ")),
                                csv(createdAt)
                            ).joinToString(",")
                        )
                    }
                }
            }

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}