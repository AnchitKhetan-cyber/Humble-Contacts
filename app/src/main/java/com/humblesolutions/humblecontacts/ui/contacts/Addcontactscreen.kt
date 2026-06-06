package com.humblesolutions.humblecontacts.ui.contacts

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onBack:    () -> Unit = {},
    onSave:    () -> Unit = {}
) {
    var fullName  by remember { mutableStateOf("") }
    var jobRole   by remember { mutableStateOf("") }
    var company   by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var linkedIn  by remember { mutableStateOf("") }
    var notes     by remember { mutableStateOf("") }

    var extractedContact by remember {
        mutableStateOf(ContactInfo())
    }

    LaunchedEffect(extractedContact) {
        fullName = extractedContact.name
        jobRole = extractedContact.designation
        company = extractedContact.company
        email = extractedContact.email
        phone = extractedContact.phone
        linkedIn = extractedContact.linkedin
    }

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val context = LocalContext.current

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->

            bitmap?.let {

                val uri = saveBitmapAndReturnUri(
                    context,
                    it
                )

                imageUri = uri
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            imageUri = uri
        }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    if (imageUri != null) {

                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Business Card",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )

                    } else {

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
                                "Take a photo or upload from gallery",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
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
                    onClick = {

                        imageUri?.let { uri ->

                            processImage(
                                context = context,
                                imageUri = uri
                            ) { text ->

                                extractedContact =
                                    BusinessCardParser.parse(text)
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
                ContactTextField(value = fullName,  onValueChange = { fullName  = it }, label = "Full Name *",  placeholder = "John Doe")
                Spacer(Modifier.height(12.dp))
                ContactTextField(value = jobRole,   onValueChange = { jobRole   = it }, label = "Job Role",     placeholder = "Product Designer")
                Spacer(Modifier.height(12.dp))
                ContactTextField(value = company,   onValueChange = { company   = it }, label = "Company",      placeholder = "Figma")
            }

            Spacer(Modifier.height(16.dp))

            // ── Contact Info ─────────────────────────────────────────────────
            SectionCard(title = "Contact Info") {
                ContactTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "john@company.com",
                    leadingIcon = { Icon(Icons.Outlined.Email, null, modifier = Modifier.size(18.dp)) }
                )
                Spacer(Modifier.height(12.dp))
                ContactTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone",
                    placeholder = "+1 (555) 000-0000",
                    leadingIcon = { Icon(Icons.Outlined.Phone, null, modifier = Modifier.size(18.dp)) }
                )
                Spacer(Modifier.height(12.dp))
                ContactTextField(
                    value = linkedIn,
                    onValueChange = { linkedIn = it },
                    label = "LinkedIn",
                    placeholder = "linkedin.com/in/johndoe",
                    leadingIcon = { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp)) }
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
                        .height(100.dp),
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

            Spacer(Modifier.height(24.dp))

            // ── Save button ──────────────────────────────────────────────────
            Button(
                onClick = {

                    val intent = Intent(
                        ContactsContract.Intents.Insert.ACTION
                    ).apply {

                        type = ContactsContract.RawContacts.CONTENT_TYPE

                        putExtra(
                            ContactsContract.Intents.Insert.NAME,
                            fullName
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

                    onSave()
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
                    "Save Contact",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
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
    leadingIcon: (@Composable () -> Unit)? = null
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
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            leadingIcon = leadingIcon,
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor    = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor      = MaterialTheme.colorScheme.primary
            )
        )
    }
}