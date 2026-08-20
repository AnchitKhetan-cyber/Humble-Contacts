package com.humblesolutions.humblecontacts.ui.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.humblesolutions.humblecontacts.ui.auth.CountryCodeDropdown
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.humblesolutions.humblecontacts.ui.auth.countryCodes
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab
import com.humblesolutions.humblecontacts.utils.NetworkUtils

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    vcard: String? = null,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val viewModel: ContactViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    var fullName  by rememberSaveable { mutableStateOf("") }
    var jobRole   by rememberSaveable { mutableStateOf("") }
    var company   by rememberSaveable { mutableStateOf("") }
    var industry  by rememberSaveable { mutableStateOf("") }
    var email     by rememberSaveable { mutableStateOf("") }
    var phone     by rememberSaveable { mutableStateOf("") }
    var linkedIn  by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var notes     by rememberSaveable { mutableStateOf("") }
    var eventName by rememberSaveable { mutableStateOf("") }
    var location  by rememberSaveable { mutableStateOf("") }
    var tags      by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.listSaver(
            save = { it },
            restore = { it }
        )
    ) { mutableStateOf(emptyList<String>()) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var isSaving  by rememberSaveable { mutableStateOf(false) }

    var savingDots by rememberSaveable { mutableStateOf("") }

    // Seed prefill from a scanned vCard (if any). Parsing is best-effort and
    // never throws, so a malformed vCard just yields a blank form. The existing
    // LaunchedEffect(extractedContact) below copies these into the form fields,
    // sharing the business-card OCR prefill path.
    var extractedContact by rememberSaveable {
        mutableStateOf(
            if (vcard != null) VCardParser.parse(vcard) else ContactInfo()
        )
    }

    var isExtracting by rememberSaveable { mutableStateOf(false) }
    var extractingDots by rememberSaveable { mutableStateOf("") }
    var selectedCountry by rememberSaveable {
        mutableStateOf(
            countryCodes.firstOrNull { it.dialCode == "+91" }
                ?: countryCodes.first()
        )
    }

    var showSaveToPhoneDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showReplaceDialog by rememberSaveable { mutableStateOf(false) }

    val FocusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(extractedContact) {
        fullName = extractedContact.name
        jobRole = extractedContact.designation
        company = extractedContact.company
        email = extractedContact.email
        val rawPhone = extractedContact.phone.filter(Char::isDigit)
        phone = when {
            rawPhone.length > 10 && rawPhone.startsWith("91") -> rawPhone.drop(2)
            rawPhone.length > 10 && rawPhone.startsWith("0")  -> rawPhone.drop(1)
            else -> rawPhone
        }.take(10)
        linkedIn = extractedContact.linkedin
        address = extractedContact.address
    }

    LaunchedEffect(isExtracting) {
        if (isExtracting) {
            while (true) {
                extractingDots = "."
                kotlinx.coroutines.delay(400)

                extractingDots = ".."
                kotlinx.coroutines.delay(400)

                extractingDots = "..."
                kotlinx.coroutines.delay(400)
            }
        } else {
            extractingDots = ""
        }
    }

    LaunchedEffect(isSaving) {
        if (isSaving) {
            while (true) {
                savingDots = "."
                delay(400)

                savingDots = ".."
                delay(400)

                savingDots = "..."
                delay(400)
            }
        } else {
            savingDots = ""
        }
    }

    val imageUriSaver: Saver<List<Uri>, List<String>> = Saver(
        save = { list -> list.map { uri -> uri.toString() } },
        restore = { saved -> saved.map { str -> Uri.parse(str) } }
    )

    var imageUris by rememberSaveable(stateSaver = imageUriSaver) {
        mutableStateOf<List<Uri>>(emptyList())
    }

    val context = LocalContext.current


    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->

            bitmap?.let {

                val uri = saveBitmapAndReturnUri(context, it)

                imageUris = imageUris + uri
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = 2)
        ) { uris ->

            if (uris.isEmpty()) return@rememberLauncherForActivityResult

            val updatedList = (imageUris + uris)
                .distinct()      // Prevent duplicate URIs
                .take(2)         // Maximum 2 images

            if (updatedList.size == imageUris.size && uris.isNotEmpty()) {
                Toast.makeText(
                    context,
                    "Maximum 2 images allowed",
                    Toast.LENGTH_SHORT
                ).show()
                return@rememberLauncherForActivityResult
            }

            imageUris = updatedList
        }

    var showCameraSettingsDialog by remember { mutableStateOf(false) }
    // Counts how many times the user has denied the camera permission.
    var cameraDenialCount by rememberSaveable { mutableStateOf(0) }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                cameraDenialCount = 0
                cameraLauncher.launch(null)
            } else {
                cameraDenialCount++
                // Only surface the Settings dialog once the user has denied twice.
                if (cameraDenialCount >= 2) {
                    showCameraSettingsDialog = true
                }
            }
        }

    if (showCameraSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showCameraSettingsDialog = false },
            title = { Text("Camera permission needed") },
            text  = {
                Text(
                    "Camera access was turned off. To take a photo, enable the Camera " +
                        "permission for this app in Settings."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCameraSettingsDialog = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.ime)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ){
                FocusManager.clearFocus()
            },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add New Contact",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
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
        },
        bottomBar = {
            BottomNavBar(
                selected = NavTab.SCAN,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.HOME -> onNavigateToHome()
                        NavTab.CONTACTS -> onNavigateToContacts()
                        NavTab.SCAN -> {}
                        NavTab.PROFILE -> onNavigateToProfile()
                    }
                }
            )
        }

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            // ── Business Card Section ────────────────────────────────────────
            SectionCard(title = "Business Card") {
                // Dashed upload zone
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {

                    if (imageUris.isEmpty()) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                "Scan Business Card",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                "Upload up to 2 photos",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    } else {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {

                            imageUris.forEachIndexed { index, uri ->

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .size(180.dp)
                                ) {

                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(uri)
                                            .memoryCacheKey(uri.toString())
                                            .diskCacheKey(uri.toString())
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        shadowElevation = 4.dp
                                    ) {
                                        IconButton(
                                            onClick = {
                                                imageUris = imageUris.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Remove image",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {

                            if (imageUris.size >= 2) {
                                Toast.makeText(
                                    context,
                                    "Maximum 2 images allowed",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@OutlinedButton
                            }

                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    enabled = imageUris.isNotEmpty() && !isExtracting,
                    onClick = {

                        isExtracting = true

                        val texts = mutableListOf<String>()

                        imageUris.forEach { uri ->

                            processImage(
                                context = context,
                                imageUri = uri
                            ) { text ->

                                texts.add(text)

                                if (texts.size == imageUris.size) {

                                    val mergedText = texts.joinToString("\n\n==========\n\n")

                                    if (mergedText.isBlank()) {

                                        isExtracting = false

                                        Toast.makeText(
                                            context,
                                            "No text found. Please capture a clearer image.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        return@processImage
                                    }
                                    if (!NetworkUtils.isInternetAvailable(context)) {

                                        isExtracting = false

                                        Toast.makeText(
                                            context,
                                            "No internet connection. Please connect to the internet and try again.",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        return@processImage
                                    }

                                    viewModel.parseBusinessCard(
                                        ocrText = mergedText,
                                        onResult = {
                                            extractedContact = it
                                            isExtracting = false
                                        },
                                        onError = {
                                            isExtracting = false

                                            Toast.makeText(
                                                context,
                                                it,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isExtracting)
                            "Extracting$extractingDots"
                        else
                            "Auto Extract",
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Basic Details ────────────────────────────────────────────────
            SectionCard(title = "Basic Details") {
                ContactTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        nameError = false
                    },
                    label = "Full Name *",
                    placeholder = "Name",
                    capitalization = KeyboardCapitalization.Words
                )
                if (nameError) {
                    Text("Name is required", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = androidx.compose.ui.Modifier.padding(start = 4.dp, top = 2.dp))
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
            }

            Spacer(Modifier.height(16.dp))

            // ── Contact Info ─────────────────────────────────────────────────
            SectionCard(title = "Contact Info") {
                ContactTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "Email Id",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, null)
                    }
                )
                Spacer(Modifier.height(12.dp))
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
                            onCountrySelected = {
                                selectedCountry = it
                            }
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input ->
                                val digits = input.filter(Char::isDigit)
                                phone = when {
                                    digits.length > 10 && digits.startsWith("91") -> digits.drop(2)
                                    digits.length > 10 && digits.startsWith("0")  -> digits.drop(1)
                                    else -> digits
                                }.take(10)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDataType = ContentDataType.None },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
                            placeholder = {
                                Text("9876543210")
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Phone, null)
                            },
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
                    label = "LinkedIn Username",
                    placeholder = "username",
                    keyboardType = KeyboardType.Text,
                    leadingIcon = {
                        Icon(Icons.Outlined.Link, null)
                    }
                )
                Spacer(Modifier.height(12.dp))

                ContactTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address",
                    placeholder = "Address",
                    leadingIcon = {
                        Icon(Icons.Outlined.LocationOn, null)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Notes ────────────────────────────────────────────────────────
            SectionCard(title = "Notes") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .semantics { contentDataType = ContentDataType.None },
                    placeholder = { Text("How did you meet? What did you discuss?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor    = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor      = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Tags ─────────────────────────────────────────────────────────
            SectionCard(title = "Tags") {
                TagEditor(
                    tags = tags,
                    onTagsChange = { tags = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Save button ──────────────────────────────────────────────────
            val scope = rememberCoroutineScope()
            Button(
                enabled = !isSaving,
                onClick = {

                    if (fullName.isBlank()) {
                        nameError = true
                        return@Button
                    }

                    val linkedInUrl = if (linkedIn.isBlank()) {
                        ""
                    } else {
                        "https://www.linkedin.com/in/${linkedIn.trim()}"
                    }

                    isSaving = true

                    viewModel.addContact(
                        fullName = fullName.trim(),
                        jobRole = jobRole.trim(),
                        company = company.trim(),
                        email = email.trim(),
                        phone = if (phone.isBlank()) "" else "${selectedCountry.dialCode} ${phone.trim()}",
                        linkedIn = linkedInUrl,
                        address = address.trim(),
                        notes = notes.trim(),
                        imageUri = imageUris.firstOrNull(),
                        tags = tags
                    ) { added ->

                        isSaving = false

                        if (added) {

                            Toast.makeText(
                                context,
                                "Contact saved",
                                Toast.LENGTH_SHORT
                            ).show()

                            showSaveToPhoneDialog = true

                        } else {
                            showReplaceDialog = true
                        }
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
                    text = if (isSaving) "Saving$savingDots" else "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showSaveToPhoneDialog) {

        AlertDialog(
            onDismissRequest = {},

            icon = {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null
                )
            },

            title = {
                Text("Contact Saved")
            },

            text = {
                Text(
                    "The contact was saved to Humble Contacts. Would you also like to save it to your phone contacts?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        saveToDeviceContacts(
                            context = context,
                            name = fullName,
                            phone = if (phone.isBlank()) "" else "${selectedCountry.dialCode} ${phone.trim()}",
                            email = email,
                            company = company,
                            jobRole = jobRole
                        )

                        showSaveToPhoneDialog = false

                        onSave()
                    }
                ) {
                    Text("Save to Phone")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        showSaveToPhoneDialog = false

                        onSave()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            icon = {
                Icon(Icons.Outlined.Warning, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Contact Already Exists") },
            text = {
                Text("A contact with this name already exists. Would you like to replace it with the new details?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceDialog = false
                        val linkedInUrl = if (linkedIn.isBlank()) ""
                        else "https://www.linkedin.com/in/${linkedIn.trim()}"

                        isSaving = true

                        viewModel.replaceContact(
                            fullName = fullName.trim(),
                            jobRole = jobRole.trim(),
                            company = company.trim(),
                            email = email.trim(),
                            phone = if (phone.isBlank()) "" else "${selectedCountry.dialCode} ${phone.trim()}",
                            linkedIn = linkedInUrl,
                            address = address.trim(),
                            notes = notes.trim(),
                            imageUri = imageUris.firstOrNull(),
                            tags = tags
                        ) {
                            isSaving = false

                            Toast.makeText(
                                context,
                                "Contact replaced",
                                Toast.LENGTH_SHORT
                            ).show()

                            showSaveToPhoneDialog = true
                        }
                    }
                ) {
                    Text("Replace", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ─── Shared Composables ───────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ContactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDataType = ContentDataType.None },
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = keyboardType
            ),
            placeholder = {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            leadingIcon = leadingIcon,
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private fun saveToDeviceContacts(
    context: android.content.Context,
    name: String,
    phone: String,
    email: String,
    company: String,
    jobRole: String
) {

    val intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE

        putExtra(
            ContactsContract.Intents.Insert.NAME,
            name
        )

        putExtra(
            ContactsContract.Intents.Insert.PHONE,
            phone
        )

        putExtra(
            ContactsContract.Intents.Insert.EMAIL,
            email
        )

        putExtra(
            ContactsContract.Intents.Insert.COMPANY,
            company
        )

        putExtra(
            ContactsContract.Intents.Insert.JOB_TITLE,
            jobRole
        )
    }

    context.startActivity(intent)
}