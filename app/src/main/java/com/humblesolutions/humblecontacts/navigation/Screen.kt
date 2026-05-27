package com.humblesolutions.humblecontacts.navigation

object Routes {
    const val SPLASH         = "splash"
    const val INTRO          = "intro"
    const val LOGIN          = "login"
    const val REGISTER       = "register"
    const val HOME           = "home"
    const val CONTACTS       = "contacts"
    const val CONTACT_DETAIL = "contact/{contactId}"
    const val ADD_CONTACT    = "add_contact"
    const val SCAN           = "scan"
    const val NFC            = "nfc"
    const val PROFILE        = "profile"

    const val PHONE_INPUT = "phone_input"
    const val OTP_VERIFY  = "otp_verify"

    // Helper to build the contact detail route with a real ID
    fun contactDetail(id: String) = "contact/$id"
}