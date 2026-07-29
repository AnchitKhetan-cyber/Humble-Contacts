package com.humblesolutions.humblecontacts.ui.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.ListenerRegistration
import com.humblesolutions.humblecontacts.data.auth.PendingDeletionStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit
) {

    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val profileViewModel: ProfileViewModel = viewModel()

    // Persisted gate state, so the flow can resume after the email-link round trip.
    val store = remember { PendingDeletionStore(context) }

    // Channels that must be verified before deletion (email link AND/OR phone OTP),
    // based on what's on the account. All required channels must be completed.
    val requiredChannels = remember { profileViewModel.requiredChannels() }

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Phone OTP dialog state.
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var phoneVerificationId by remember { mutableStateOf<String?>(null) }

    // "Check your email" waiting state while the confirmation link is outstanding.
    var emailWaiting by remember { mutableStateOf(false) }

    // Password fallback (Email/Password accounts) when the confirmation link can't be sent.
    var showPasswordFallback by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    // Holds the gate-advancement logic in a plain (non-snapshot) box so the phone/email
    // completion callbacks can invoke it without a forward reference — and so reassigning
    // it each composition does not write snapshot state (which would loop recomposition).
    val gate = remember { object { var advance: () -> Unit = {} } }

    // Firestore listener for the Cloud Function deletion confirmation (Google accounts).
    val confirmListener = remember { object { var reg: ListenerRegistration? = null } }
    DisposableEffect(Unit) {
        onDispose { confirmListener.reg?.remove(); confirmListener.reg = null }
    }

    BackHandler(enabled = isDeleting || emailWaiting || showOtpDialog || showPasswordFallback) {
        // Block back navigation while a delete gate is mid-flight.
    }

    // Final step: delete the account (Auth first, then best-effort data cleanup).
    fun finalizeDeletion() {
        isDeleting = true
        profileViewModel.finalizeDeletion(
            context = context,
            onSuccess = {
                isDeleting = false
                Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                onDeleteSuccess()
            },
            onError = { message ->
                isDeleting = false
                emailWaiting = false
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Sends the phone OTP and shows the code entry dialog.
    fun startPhoneOtp() {
        val activity = context as? Activity
        if (activity == null) {
            Toast.makeText(context, "Unable to verify. Please try again.", Toast.LENGTH_LONG).show()
            return
        }
        isDeleting = true
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                // Instant / auto-retrieval — verify the credential and advance.
                profileViewModel.completePhoneCredential(
                    context = context,
                    credential = cred,
                    onDone = { isDeleting = false; showOtpDialog = false; gate.advance() },
                    onError = { msg -> isDeleting = false; Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                )
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isDeleting = false
                Toast.makeText(context, e.message ?: "Could not send verification code.", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                phoneVerificationId = verificationId
                isDeleting = false
                otpInput = ""
                showOtpDialog = true
            }
        }
        val sent = profileViewModel.sendPhoneOtp(activity, callbacks)
        if (!sent) {
            isDeleting = false
            Toast.makeText(context, "No phone number on file for this account.", Toast.LENGTH_LONG).show()
        }
    }

    // Sends the email confirmation link and shows the waiting state. The flow resumes
    // when the user taps the link and returns via deep link (see the LaunchedEffect below).
    fun startEmailLink() {
        emailWaiting = true
        profileViewModel.sendEmailLink(
            context = context,
            onSent = {
                Toast.makeText(
                    context,
                    "We've emailed you a secure link. Tap it to finish deleting your account.",
                    Toast.LENGTH_LONG
                ).show()
            },
            onError = { _ ->
                // The confirmation link can't be sent (e.g. email-link sign-in is disabled).
                // Degrade gracefully: Email/Password accounts fall back to a password prompt;
                // other providers skip the email channel and prove identity another way
                // (silent Google re-auth at finalize, or the phone OTP).
                emailWaiting = false
                if (profileViewModel.currentProviderId() == "email") {
                    passwordInput = ""
                    showPasswordFallback = true
                } else {
                    profileViewModel.skipEmailChannel(context)
                    gate.advance()
                }
            }
        )
    }

    // Google sign-in: request the Cloud Function confirmation email and wait for the user
    // to tap the link (which flips `confirmed` in Firestore — watched by the listener).
    fun startGoogleEmailVerification() {
        // Busy while the request is in flight; only claim "email sent" once it succeeds.
        isDeleting = true
        profileViewModel.requestDeletionEmail(
            context = context,
            onSent = {
                isDeleting = false
                emailWaiting = true
                Toast.makeText(
                    context,
                    "We've emailed you a confirmation link. Tap it to finish deleting your account.",
                    Toast.LENGTH_LONG
                ).show()
                confirmListener.reg?.remove()
                confirmListener.reg = profileViewModel.observeDeletionConfirmed(
                    context = context,
                    onConfirmed = {
                        confirmListener.reg?.remove(); confirmListener.reg = null
                        emailWaiting = false
                        // The Cloud Function deleted the account server-side (admin, no
                        // re-auth). Just clear local state, sign out, and go to login.
                        profileViewModel.completeServerSideDeletion(context)
                        Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                        onDeleteSuccess()
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onError = { message ->
                isDeleting = false
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    // The email channel is provider-aware: Google accounts confirm via the Cloud Function
    // email; everything else uses the email link (with password fallback on send failure).
    fun startEmailChannel() {
        if (profileViewModel.currentProviderId() == "google") startGoogleEmailVerification()
        else startEmailLink()
    }

    // Advances the gate: complete the phone OTP first (in-app), then the email step,
    // then finalize. Driven by the persisted per-channel flags so it is order-independent
    // across the email round trip.
    gate.advance = {
        when {
            requiredChannels.contains(DeleteChannel.PHONE) && !store.phoneVerified ->
                startPhoneOtp()
            requiredChannels.contains(DeleteChannel.EMAIL) && !store.emailVerified ->
                startEmailChannel()
            else ->
                finalizeDeletion()
        }
    }

    // On return from the email link (fresh Activity intent stashed the URL in the store),
    // complete the email side and continue the gate.
    LaunchedEffect(Unit) {
        val link = store.emailLink
        when {
            // Returning from the email-link (Email/Password accounts).
            store.inProgress && link != null -> {
                store.emailLink = null
                emailWaiting = true
                profileViewModel.completeEmailLink(
                    context = context,
                    link = link,
                    onDone = { emailWaiting = false; gate.advance() },
                    onError = { message ->
                        emailWaiting = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                )
            }

            // Resuming a Google Cloud-Function verification after re-entering the screen.
            store.inProgress && profileViewModel.currentProviderId() == "google" -> {
                if (store.emailVerified) {
                    gate.advance()
                } else {
                    emailWaiting = true
                    confirmListener.reg?.remove()
                    confirmListener.reg = profileViewModel.observeDeletionConfirmed(
                        context = context,
                        onConfirmed = {
                            confirmListener.reg?.remove(); confirmListener.reg = null
                            emailWaiting = false
                            profileViewModel.completeServerSideDeletion(context)
                            Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                            onDeleteSuccess()
                        },
                        onError = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            // Opened fresh with leftover state from an abandoned attempt — start clean.
            store.inProgress -> store.clear()
        }
    }

    var acknowledge1 by remember { mutableStateOf(false) }
    var acknowledge2 by remember { mutableStateOf(false) }
    var acknowledge3 by remember { mutableStateOf(false) }
    var acknowledge4 by remember { mutableStateOf(false) }
    var acknowledge5 by remember { mutableStateOf(false) }
    var acknowledge6 by remember { mutableStateOf(false) }

    var deleteText by remember {
        mutableStateOf(TextFieldValue(""))
    }

    val canDelete =
        acknowledge1 &&
                acknowledge2 &&
                acknowledge3 &&
                acknowledge4 &&
                acknowledge5 &&
                acknowledge6 &&
                deleteText.text.trim() == "DELETE"

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        "Delete Account",
                        fontWeight = FontWeight.SemiBold
                    )

                },

                navigationIcon = {

                    IconButton(
                        enabled = !isDeleting,
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = null
                        )

                    }

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),

            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            Spacer(Modifier.height(16.dp))

            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text("Step 1 of 2")
                }
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Delete Your Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Please read the following carefully before continuing.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Please confirm the following",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            DeleteChecklistItem(
                checked = acknowledge1,
                title = "Permanent Account Deletion",
                description = "I understand my Humble Contacts account will be permanently deleted."
            ) {
                acknowledge1 = it
            }

            DeleteChecklistItem(
                checked = acknowledge2,
                title = "Delete Contacts",
                description = "All saved contacts, notes and business card information will be removed."
            ) {
                acknowledge2 = it
            }

            DeleteChecklistItem(
                checked = acknowledge3,
                title = "Delete Uploaded Media",
                description = "Uploaded business card images and profile information will be deleted."
            ) {
                acknowledge3 = it
            }

            DeleteChecklistItem(
                checked = acknowledge4,
                title = "Irreversible Action",
                description = "I understand this action cannot be undone."
            ) {
                acknowledge4 = it
            }

            DeleteChecklistItem(
                checked = acknowledge5,
                title = "Export Complete",
                description = "I have exported any contacts I wish to keep."
            ) {
                acknowledge5 = it
            }

            DeleteChecklistItem(
                checked = acknowledge6,
                title = "No Recovery",
                description = "Humble Solutions cannot recover deleted accounts."
            ) {
                acknowledge6 = it
            }

            Spacer(Modifier.height(28.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        "Final Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "To prevent accidental deletion, type",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {

                        Text(
                            "DELETE",
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 8.dp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = deleteText,
                        onValueChange = {
                            deleteText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        label = {
                            Text("Type DELETE here")
                        },
                        supportingText = {
                            Text(
                                "Account deletion is case-sensitive."
                            )
                        }
                    )

                }

            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    showConfirmationDialog = true
                },
                enabled = canDelete && !isDeleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {

                if (isDeleting) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )

                    Spacer(Modifier.width(12.dp))

                    Text("Deleting Account...")

                } else {

                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Delete Account",
                        fontWeight = FontWeight.Bold
                    )

                }

            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                enabled = !isDeleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {

                Text("Cancel")

            }

            Spacer(Modifier.height(32.dp))

        }
    }

    if (showConfirmationDialog) {

        AlertDialog(

            onDismissRequest = {
                if (!isDeleting) {
                    showConfirmationDialog = false
                }
            },

            icon = {

                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )

            },

            title = {

                Text(
                    "Delete Account?"
                )

            },

            text = {

                Column {

                    Text(
                        "Are you absolutely sure you want to permanently delete your account?"
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "This will permanently remove:",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("• Your profile")
                    Text("• All saved contacts")
                    Text("• Uploaded business cards")
                    Text("• Meeting history")
                    Text("• Cloud data")

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "This action cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )

                }

            },

            dismissButton = {

                TextButton(

                    enabled = !isDeleting,

                    onClick = {
                        showConfirmationDialog = false
                    }

                ) {

                    Text("Cancel")

                }

            },

            confirmButton = {

                Button(

                    enabled = !isDeleting,

                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),

                    onClick = {
                        // Close the confirmation and start the verification gate.
                        // Nothing is deleted until every required channel is verified.
                        showConfirmationDialog = false
                        gate.advance()
                    }

                ) {

                    if (isDeleting) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )

                    } else {

                        Text("Delete Forever")

                    }

                }

            }

        )

    }

    // ── Gate: Phone OTP ──────────────────────────────────────────────────────────

    if (showOtpDialog) {

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) { showOtpDialog = false; profileViewModel.cancelDeletion(context) }
            },
            title = { Text("Enter verification code") },
            text = {
                Column {
                    Text(
                        "Enter the 6-digit code we sent to your phone to permanently delete your account."
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it.filter { c -> c.isDigit() }.take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Verification code") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting && otpInput.length == 6 && phoneVerificationId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        val vid = phoneVerificationId
                        if (vid == null) {
                            Toast.makeText(
                                context,
                                "Verification expired. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            showOtpDialog = false
                        } else {
                            isDeleting = true
                            profileViewModel.completePhoneOtp(
                                context = context,
                                verificationId = vid,
                                otp = otpInput,
                                onDone = {
                                    isDeleting = false
                                    showOtpDialog = false
                                    gate.advance()
                                },
                                onError = { msg ->
                                    isDeleting = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verify")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showOtpDialog = false; profileViewModel.cancelDeletion(context) }
                ) { Text("Cancel") }
            }
        )
    }

    // ── Gate: Password fallback (Email/Password accounts, link unavailable) ──────

    if (showPasswordFallback) {

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) { showPasswordFallback = false; profileViewModel.cancelDeletion(context) }
            },
            title = { Text("Confirm your password") },
            text = {
                Column {
                    Text("Enter your account password to permanently delete your account.")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting && passwordInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        isDeleting = true
                        profileViewModel.reauthWithPassword(
                            context = context,
                            password = passwordInput,
                            onDone = {
                                isDeleting = false
                                showPasswordFallback = false
                                gate.advance()
                            },
                            onError = { msg ->
                                isDeleting = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showPasswordFallback = false; profileViewModel.cancelDeletion(context) }
                ) { Text("Cancel") }
            }
        )
    }

    // ── Gate: Email confirmation link (waiting for the tap) ──────────────────────

    if (emailWaiting) {

        AlertDialog(
            onDismissRequest = { /* must resolve via the emailed link or Cancel */ },
            title = { Text("Check your email") },
            text = {
                Column {
                    Text(
                        "We've sent a secure confirmation link to " +
                            (profileViewModel.currentEmail ?: "your email") +
                            ". Open it on this device and tap the link to finish deleting your account."
                    )
                    if (requiredChannels.contains(DeleteChannel.PHONE)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You'll also confirm the code sent to your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        emailWaiting = false
                        confirmListener.reg?.remove(); confirmListener.reg = null
                        profileViewModel.cancelDeletion(context)
                    }
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DeleteChecklistItem(
    checked: Boolean,
    title: String,
    description: String,
    onCheckedChange: (Boolean) -> Unit
) {

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }

        }

    }

}


