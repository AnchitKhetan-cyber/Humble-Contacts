package com.humblesolutions.humblecontacts.navigation

import android.net.Uri

object Routes {
    const val SPLASH         = "splash"
    const val INTRO          = "intro"
    const val LOGIN          = "login"
    const val REGISTER       = "register"
    const val HOME           = "home"
    const val CONTACTS       = "contacts"
    const val CONTACT_DETAIL = "contact/{contactId}"
    const val ADD_CONTACT    = "add_contact"
    // Edit an existing contact by reusing the Add Contact form in edit mode.
    const val EDIT_CONTACT   = "edit_contact/{contactId}"
    // Registration pattern for the add-contact screen with an optional vCard to
    // prefill from. Plain `add_contact` navigation still matches this (vcard = null).
    const val ADD_CONTACT_WITH_ARGS = "add_contact?vcard={vcard}"
    const val SCAN           = "scan"
    const val PROFILE        = "profile"

    const val DELETE_ACCOUNT = "delete_account"

    const val COMPLETE_PROFILE = "complete_profile"

    const val LINKED_ACCOUNTS = "linked_accounts"

    const val EDIT_PROFILE = "edit_profile"

    const val PHONE_INPUT = "phone_input"
    const val OTP_VERIFY  = "otp_verify"

    // Helper to build the contact detail route with a real ID
    fun contactDetail(id: String) = "contact/$id"

    // Helper to build the add-contact route carrying a raw vCard string. The
    // vCard is URL-encoded so its newlines/colons/`+` don't break the route.
    fun addContactWithVCard(rawVCard: String) = "add_contact?vcard=${Uri.encode(rawVCard)}"
}