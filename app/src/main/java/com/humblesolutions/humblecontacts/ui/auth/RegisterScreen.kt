package com.humblesolutions.humblecontacts.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.humblecontacts.data.auth.GoogleSignInHelper
import com.humblesolutions.humblecontacts.ui.theme.DarkBackground
import com.humblesolutions.humblecontacts.ui.theme.DarkSurfaceVariant
import com.humblesolutions.humblecontacts.ui.theme.Gold400
import com.humblesolutions.humblecontacts.ui.theme.Navy600
import com.humblesolutions.humblecontacts.ui.theme.Navy900
import com.humblesolutions.humblecontacts.ui.theme.SurfaceVariant
import com.humblesolutions.humblecontacts.ui.theme.TextOnNavy

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory())
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val dark = MaterialTheme.colorScheme.background == DarkBackground
    var termsAccepted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // One-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                // Email/Google sign-in both succeed → this event is fired → navigate away
                is AuthEvent.NavigateToHome -> onRegisterSuccess()

                // ViewModel wants us to launch the Google picker.
                // This runs inside a coroutine so we can call the suspend helper directly.
                is AuthEvent.LaunchGoogleSignIn -> {
                    // Credential Manager requires the Activity context, not just any Context.
                    // LocalContext.current inside an Activity IS the Activity, so this cast is safe.
                    // If for any reason it's not (e.g. inside a Dialog), the ?: return@collect
                    // guard prevents a crash.
                    val activity = context as? Activity ?: return@collect

                    val helper = GoogleSignInHelper(activity)

                    // helper.signIn() suspends here — the coroutine pauses while the
                    // Google account picker is visible on screen. No thread blocking.
                    when (val result = helper.signIn()) {

                        is GoogleSignInHelper.GoogleSignInResult.Success -> {
                            // We have the idToken (a JWT from Google).
                            // Hand it to the ViewModel which forwards it to Firebase.
                            viewModel.onGoogleIdToken(result.idToken)
                        }

                        is GoogleSignInHelper.GoogleSignInResult.Error -> {
                            // Something went wrong (network failure, bad config, etc.)
                            // Show a user-readable error via the existing error banner.
                            viewModel.onGoogleSignInError(result.message)
                        }

                        is GoogleSignInHelper.GoogleSignInResult.Cancelled -> {
                            // User pressed back / dismissed the picker.
                            // This is intentional — silently do nothing.
                            // Do NOT show an error message for a deliberate dismissal.
                        }
                    }
                }

                // Events meant for other screens — ignore here
                else -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {

        // ── Decorative blob ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 70.dp, y = (-70).dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(Gold400.copy(alpha = 0.05f))
        )

        // ── Scrollable content ───────────────────────────────────────────────
        // FIX: verticalScroll is on the OUTER column, not inside the Card.
        // This lets everything scroll naturally including the logo.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Logo
            HumbleContactsLogo()

            Spacer(Modifier.height(36.dp))

            // ── Card ─────────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                // FIX: No verticalScroll here — parent column handles it
                Column(modifier = Modifier.padding(28.dp)) {

                    Text(
                        text  = "Create Account",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text     = "Sign up to get started",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Global error banner
                    ErrorMessage(
                        message   = uiState.errorMessage,
                        onDismiss = { viewModel.dismissError() }
                    )

                    // ── Full Name ─────────────────────────────────────────────
                    FieldLabel("Full Name")
                    HumbleTextField(
                        value           = uiState.name,
                        onValueChange   = { viewModel.onNameChange(it) },
                        placeholder     = "Enter First and Last Name",
                        leadingIcon     = Icons.Outlined.Person,
                        keyboardType    = KeyboardType.Text,
                        imeAction       = ImeAction.Next,
                        isError         = uiState.nameError != null,
                        errorMessage    = uiState.nameError,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Email ─────────────────────────────────────────────────
                    FieldLabel("Email Address")
                    HumbleTextField(
                        value           = uiState.email,
                        onValueChange   = { viewModel.onEmailChange(it) },
                        placeholder     = "Enter Email",
                        leadingIcon     = Icons.Outlined.Email,
                        keyboardType    = KeyboardType.Email,
                        imeAction       = ImeAction.Next,
                        isError         = uiState.emailError != null,
                        errorMessage    = uiState.emailError,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Phone ─────────────────────────────────────────────────
                    FieldLabel("Phone Number")
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (dark) DarkSurfaceVariant else SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = "🇮🇳 +91",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HumbleTextField(
                            value           = uiState.phone,
                            onValueChange   = { viewModel.onPhoneChange(it.filter(Char::isDigit)) },
                            placeholder     = "Mobile Number",
                            leadingIcon     = Icons.Outlined.Phone,
                            keyboardType    = KeyboardType.Phone,
                            imeAction       = ImeAction.Next,
                            modifier        = Modifier.weight(1f),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Password ──────────────────────────────────────────────
                    FieldLabel("Password")
                    HumbleTextField(
                        value         = uiState.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder   = "Enter Password",
                        leadingIcon   = Icons.Outlined.Lock,
                        keyboardType  = KeyboardType.Password,
                        imeAction     = ImeAction.Next,
                        isError       = uiState.passwordError != null,
                        errorMessage  = uiState.passwordError,
                        visualTransformation = if (uiState.passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector        = if (uiState.passwordVisible)
                                        Icons.Outlined.VisibilityOff
                                    else
                                        Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password",
                                    tint               = MaterialTheme.colorScheme.outline,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    PasswordStrengthBar(strength = uiState.passwordStrength)

                    Spacer(Modifier.height(16.dp))

                    // ── Confirm Password ──────────────────────────────────────
                    FieldLabel("Confirm Password")
                    HumbleTextField(
                        value         = uiState.confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                        placeholder   = "Re-enter Password",
                        leadingIcon   = Icons.Outlined.Lock,
                        keyboardType  = KeyboardType.Password,
                        imeAction     = ImeAction.Done,
                        isError       = uiState.confirmError != null,
                        errorMessage  = uiState.confirmError,
                        visualTransformation = if (uiState.confirmVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleConfirmVisibility() }) {
                                Icon(
                                    imageVector        = if (uiState.confirmVisible)
                                        Icons.Outlined.VisibilityOff
                                    else
                                        Icons.Outlined.Visibility,
                                    contentDescription = "Toggle confirm",
                                    tint               = MaterialTheme.colorScheme.outline,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { focusManager.clearFocus() }  // safe — inside lambda, not composition
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Terms ─────────────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked         = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors          = CheckboxDefaults.colors(
                                checkedColor   = if (dark) Gold400 else Navy600,
                                checkmarkColor = if (dark) Navy900 else TextOnNavy
                            )
                        )
                        Text(
                            text  = "I agree to the ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(
                            onClick        = { /* open Terms */ },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text  = "Terms",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Gold400
                            )
                        }
                        Text(
                            text  = " & ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(
                            onClick        = { /* open Privacy */ },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text  = "Privacy",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Gold400
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Register button ───────────────────────────────────────
                    HumbleButton(
                        text      = "Create Account",
                        onClick   = { viewModel.submitRegister() },
                        isLoading = uiState.isLoading,
                        enabled   = termsAccepted
                    )

                    Spacer(Modifier.height(24.dp))

                    OrDivider("OR — Continue with")

                    Spacer(Modifier.height(16.dp))

                    // ── Social buttons ────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SocialButton(
                            text     = "Google",
                            icon     = {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color       = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    GoogleIcon()
                                }
                            },
                            onClick  = {
                                if (!uiState.isLoading) {
                                    // Tells the ViewModel the user wants to sign in with Google.
                                    // ViewModel sends AuthEvent.LaunchGoogleSignIn via the Channel.
                                    // The LaunchedEffect above receives it and calls GoogleSignInHelper.
                                    viewModel.onGoogleSignInClicked()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SocialButton(
                            text     = "Facebook",
                            icon     = { FacebookIcon() },
                            onClick  = { /* TODO: Facebook Sign-In */ },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Bottom link ───────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Already have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(onClick = onNavigateToLogin) {
                            Text(
                                text  = "Login",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Gold400
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}