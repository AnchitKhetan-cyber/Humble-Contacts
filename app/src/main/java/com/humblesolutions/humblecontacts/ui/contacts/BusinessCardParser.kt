package com.humblesolutions.humblecontacts.ui.contacts

object BusinessCardParser {

    fun parse(text: String): ContactInfo {

        val email =
            Regex("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+")
                .find(text)
                ?.value
                ?: ""

        val phone =
            Regex("""(\+?\d[\d\s\-()]{8,})""")
                .find(text)
                ?.value
                ?.trim()
                ?: ""

        val linkedin =
            Regex("""(linkedin\.com/[^\s]+)""")
                .find(text)
                ?.value
                ?: ""

        val lines = text
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val name = lines.firstOrNull() ?: ""

        val designation =
            lines.drop(1)
                .firstOrNull {
                    !it.contains("@") &&
                            !it.contains("www") &&
                            !it.contains("http") &&
                            !it.contains(phone)
                } ?: ""

        val company =
            lines.drop(2)
                .firstOrNull {
                    !it.contains("@") &&
                            !it.contains("www")
                } ?: ""

        return ContactInfo(
            name = name,
            designation = designation,
            company = company,
            email = email,
            phone = phone,
            linkedin = linkedin
        )
    }
}