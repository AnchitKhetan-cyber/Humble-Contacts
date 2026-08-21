package com.humblesolutions.humblecontacts.ui.contacts

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.ui.auth.CountryCodeDropdown
import com.humblesolutions.humblecontacts.ui.auth.countryCodes

/**
 * Dedicated screen for editing an existing contact. Loads the contact from the
 * realtime list, pre-fills the form once, and updates it in place via
 * [ContactViewModel.updateContact] — fields the form doesn't touch (media,
 * favourite, createdAt, business card, meeting date/location, event) are
 * preserved. Reuses the Add screen's [SectionCard] / [ContactTextField] and the
 * [TagEditor] so the styling matches.
 *
 * Note: changing the business-card photo is intentionally not offered here — the
 * existing image is preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    contactId: String,
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val viewModel: ContactViewModel = viewModel()
    val context = LocalContext.current

    // The contact being edited (null until the realtime list has loaded it).
    var original by remember { mutableStateOf<Contact?>(null) }
    // Saveable so a rotation doesn't re-run the one-time prefill over live edits.
    var hasPrefilled by rememberSaveable { mutableStateOf(false) }

    var fullName by rememberSaveable { mutableStateOf("") }
    var jobRole  by rememberSaveable { mutableStateOf("") }
    var company  by rememberSaveable { mutableStateOf("") }
    var industry by rememberSaveable { mutableStateOf("") }
    var email    by rememberSaveable { mutableStateOf("") }
    var phone    by rememberSaveable { mutableStateOf("") }
    var linkedIn by rememberSaveable { mutableStateOf("") }
    var address  by rememberSaveable { mutableStateOf("") }
    var newNote  by rememberSaveable { mutableStateOf("") }
    var tags     by rememberSaveable(
        stateSaver = listSaver(save = { it }, restore = { it })
    ) { mutableStateOf(emptyList<String>()) }
    var selectedCountry by rememberSaveable {
        mutableStateOf(
            countryCodes.firstOrNull { it.dialCode == "+91" } ?: countryCodes.first()
        )
    }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var isSaving  by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(contactId, viewModel.contacts) {
        val existing = viewModel.contacts.firstOrNull { it.contactId == contactId }
            ?: return@LaunchedEffect

        // Keep the base contact available for save; prefill the fields only once.
        original = existing
        if (hasPrefilled) return@LaunchedEffect

        fullName = existing.fullName
        jobRole = existing.jobRole
        company = existing.company
        industry = existing.industry
        email = existing.email
        address = existing.address
        tags = existing.tags

        // Phone is stored as "<dialCode> <number>" (or ""). Split it back so the
        // country selector and number field prefill correctly.
        val storedPhone = existing.phone
        if (storedPhone.isNotBlank()) {
            val dial = storedPhone.substringBefore(" ", "")
            val matched = countryCodes.firstOrNull { it.dialCode == dial }
            if (matched != null && storedPhone.contains(" ")) {
                selectedCountry = matched
                phone = storedPhone.substringAfter(" ").trim()
            } else {
                phone = storedPhone.filter { it.isDigit() }
            }
        }

        // LinkedIn is stored as a full URL; the field edits just the username.
        linkedIn = existing.linkedIn.removePrefix("https://www.linkedin.com/in/")

        hasPrefilled = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Contact", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->

        if (original == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SectionCard(title = "Basic Details") {
                ContactTextField(
                    value = fullName,
                    onValueChange = { fullName = it; nameError = false },
                    label = "Full Name *",
                    placeholder = "Name",
                    capitalization = KeyboardCapitalization.Words
                )
                if (nameError) {
                    Text(
                        "Name is required",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                ContactTextField(
                    value = jobRole,
                    onValueChange = { jobRole = it },
                    label = "Job Role",
                    placeholder = "Role/Position",
                    capitalization = KeyboardCapitalization.Words
                )
                Spacer(Modifier.height(12.dp))
                ContactTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = "Company",
                    placeholder = "Company Name",
                    capitalization = KeyboardCapitalization.Words
                )
                Spacer(Modifier.height(12.dp))
                ContactTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = "Industry",
                    placeholder = "Industry",
                    capitalization = KeyboardCapitalization.Words
                )
            }

            SectionCard(title = "Contact Info") {
                ContactTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "Email Id",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = { Icon(Icons.Outlined.Email, null) }
                )
                Spacer(Modifier.height(12.dp))

                // Phone with country-code picker.
                Column {
                    Text(
                        "Phone",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountryCodeDropdown(
                            selectedCountry = selectedCountry,
                            onCountrySelected = { selectedCountry = it }
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input -> phone = input.filter(Char::isDigit).take(10) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            placeholder = { Text("9876543210") },
                            leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ContactTextField(
                    value = linkedIn,
                    onValueChange = { linkedIn = it },
                    label = "LinkedIn",
                    placeholder = "username"
                )
                Spacer(Modifier.height(12.dp))
                ContactTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address",
                    placeholder = "Address",
                    capitalization = KeyboardCapitalization.Words
                )
            }

            SectionCard(title = "Tags") {
                TagEditor(tags = tags, onTagsChange = { tags = it })
            }

            SectionCard(title = "Add Note") {
                OutlinedTextField(
                    value = newNote,
                    onValueChange = { newNote = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = {
                        Text(
                            "Add a note (appended to existing notes)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Button(
                enabled = !isSaving,
                onClick = {
                    if (fullName.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val linkedInUrl =
                        if (linkedIn.isBlank()) "" else "https://www.linkedin.com/in/${linkedIn.trim()}"
                    val phoneValue =
                        if (phone.isBlank()) "" else "${selectedCountry.dialCode} ${phone.trim()}"

                    isSaving = true
                    viewModel.updateContact(
                        original = original!!,
                        fullName = fullName.trim(),
                        jobRole = jobRole.trim(),
                        company = company.trim(),
                        industry = industry.trim(),
                        email = email.trim(),
                        phone = phoneValue,
                        linkedIn = linkedInUrl,
                        address = address.trim(),
                        newNote = newNote.trim(),
                        tags = tags
                    ) {
                        isSaving = false
                        Toast.makeText(context, "Contact updated", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isSaving) "Updating…" else "Update",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
