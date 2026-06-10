package com.humblesolutions.humblecontacts.ui.nfc

import org.json.JSONObject

data class NfcContactPayload(
    val name: String,
    val phone: String,
    val email: String
) {

    fun toJson(): String {
        return JSONObject()
            .put("name", name)
            .put("phone", phone)
            .put("email", email)
            .toString()
    }

    companion object {

        fun fromJson(json: String): NfcContactPayload {

            val obj = JSONObject(json)

            return NfcContactPayload(
                name = obj.getString("name"),
                phone = obj.getString("phone"),
                email = obj.getString("email")
            )
        }
    }
}

