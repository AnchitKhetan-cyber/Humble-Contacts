package com.humblesolutions.humblecontacts.utils

import com.google.firebase.Timestamp
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.model.ContactNote

/**
 * Parses a CSV file into [Contact]s — the inverse of [ContactExporter]. Columns
 * are matched **by header name** (case/space-tolerant, order-independent), so a
 * file exported by this app round-trips and reasonably generic CSVs also work;
 * unknown columns are ignored and missing ones tolerated.
 *
 * A "Full Name" column is required — rows with a blank name are skipped and
 * counted. Dates are set fresh on import (not parsed); `contactId`/`ownerId`/
 * `createdAt` are filled by [com.humblesolutions.humblecontacts.data.repository.ContactRepository.addContact].
 */
object ContactImporter {

    data class ImportResult(
        val contacts: List<Contact>,
        val skippedRows: Int
    )

    fun parse(csv: String): ImportResult {
        val rows = tokenize(csv)
        if (rows.isEmpty()) return ImportResult(emptyList(), 0)

        // Find the header row rather than assuming it's the first line: this app's
        // own export writes a title + metadata preamble (and a blank line) before
        // the real "Full Name,…" header. The header is the first row that contains
        // a "full name" cell; anything before it is preamble and ignored. A plain
        // CSV with the header on line 1 is matched there just the same.
        val headerIdx = rows.indexOfFirst { row ->
            row.any { it.trim().lowercase() == "full name" }
        }
        if (headerIdx == -1) return ImportResult(emptyList(), 0)

        val header = rows[headerIdx].map { it.trim().lowercase() }
        val dataRows = rows.drop(headerIdx + 1)

        val contacts = mutableListOf<Contact>()
        var skipped = 0

        for (row in dataRows) {
            // Ignore entirely blank lines (a trailing newline yields one).
            if (row.all { it.isBlank() }) continue

            fun col(name: String): String {
                val idx = header.indexOf(name)
                return if (idx in row.indices) row[idx].trim() else ""
            }

            val fullName = col("full name")
            if (fullName.isBlank()) {
                skipped++
                continue
            }

            val tags = col("tags")
                .split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val notesText = col("notes")
            val notes =
                if (notesText.isBlank()) emptyList()
                else listOf(ContactNote(text = notesText, createdAt = Timestamp.now()))

            contacts.add(
                Contact(
                    fullName = fullName,
                    jobRole = col("job role"),
                    company = col("company"),
                    industry = col("industry"),
                    email = col("email"),
                    phone = col("phone"),
                    meetingLocation = col("meeting location"),
                    eventName = col("event name"),
                    tags = tags,
                    conversationNotes = notes,
                    meetingDate = Timestamp.now(),
                    entryMethod = "import"
                )
            )
        }

        return ImportResult(contacts, skipped)
    }

    /**
     * Splits raw CSV text into rows of fields per RFC 4180: double-quoted fields
     * may contain commas and newlines, and `""` inside a quoted field is a literal
     * quote. Handles CRLF / CR / LF line endings.
     */
    internal fun tokenize(text: String): List<List<String>> {
        val normalized = text.replace("\r\n", "\n").replace("\r", "\n")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < normalized.length) {
            val c = normalized[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < normalized.length && normalized[i + 1] == '"' -> {
                        field.append('"'); i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field.setLength(0) }
                c == '\n' -> {
                    row.add(field.toString()); field.setLength(0)
                    rows.add(row); row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        // Flush a trailing field/row when the text doesn't end with a newline.
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
