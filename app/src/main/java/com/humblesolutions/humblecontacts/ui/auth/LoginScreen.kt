package com.humblesolutions.humblecontacts.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.humblesolutions.humblecontacts.ui.theme.Gold400
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory()),
    onNavigateToPhone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Reads the device's current theme setting (light or dark).
    // Automatically recomposes if the user toggles the system theme while the screen is visible.
    val dark = isSystemInDarkTheme()

    // ── One-shot event collector ───────────────────────────────────────────────
    // LaunchedEffect(Unit) runs once when this composable enters the composition.
    // It launches a coroutine that stays alive for the lifetime of the screen,
    // collecting events from the ViewModel's Channel.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {

                is AuthEvent.NavigateToHome -> onLoginSuccess()

                is AuthEvent.LaunchGoogleSignIn -> {
                    // ✅ Launch in a separate coroutine so the collector
                    //    is NOT blocked and can receive NavigateToHome
                    launch {
                        val activity = context as? Activity ?: return@launch
                        val helper   = GoogleSignInHelper(activity)

                        when (val result = helper.signIn()) {
                            is GoogleSignInHelper.GoogleSignInResult.Success ->
                                viewModel.onGoogleIdToken(result.idToken)

                            is GoogleSignInHelper.GoogleSignInResult.Error ->
                                viewModel.onGoogleSignInError(result.message)

                            is GoogleSignInHelper.GoogleSignInResult.Cancelled -> Unit
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    // ── Root container ─────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Tapping anywhere outside a text field clears keyboard focus.
            // indication = null removes the ripple on the background tap.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {

        // ── Decorative blob (purely visual) ───────────────────────────────────
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 70.dp, y = (-70).dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(Gold400.copy(alpha = if (dark) 0.08f else 0.05f))
        )

        // ── Scrollable content ─────────────────────────────────────────────────
        // verticalScroll on the outer Column means the entire screen (logo + card)
        // scrolls together. This prevents the card from being clipped on small screens
        // or when the keyboard is up.
        // navigationBarsPadding() adds bottom padding equal to the gesture nav bar height.
        // imePadding() adds extra bottom padding when the soft keyboard is visible,
        // so the button/fields are never hidden behind it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // App logo
            HumbleContactsLogo()

            Spacer(Modifier.height(36.dp))

            // ── Login card ─────────────────────────────────────────────────────
            // Everything inside the card is the actual login form.
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    // ── Card title ─────────────────────────────────────────────
                    Text(
                        text  = "Welcome back",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text     = "Login to get started",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // ── Global error banner ────────────────────────────────────
                    // Shown for both email errors AND Google Sign-In errors.
                    // AnimatedVisibility gives it a smooth slide-in/out animation.
                    // uiState.errorMessage is null when no error → banner is hidden.
                    ErrorMessage(
                        message   = uiState.errorMessage,
                        onDismiss = { viewModel.dismissError() }
                    )

                    // ── Email field ────────────────────────────────────────────
                    FieldLabel("Email Address")
                    HumbleTextField(
                        value         = uiState.email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        placeholder   = "Enter Email",
                        leadingIcon   = Icons.Outlined.Email,
                        keyboardType  = KeyboardType.Email,
                        // ImeAction.Next moves focus to the next field (Password)
                        // when the user taps the "Next" key on the keyboard.
                        imeAction     = ImeAction.Next,
                        isError       = uiState.emailError != null,
                        errorMessage  = uiState.emailError,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Password field ─────────────────────────────────────────
                    FieldLabel("Password")
                    HumbleTextField(
                        value         = uiState.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder   = "Enter Password",
                        leadingIcon   = Icons.Outlined.Lock,
                        keyboardType  = KeyboardType.Password,
                        // ImeAction.Done on the last field — tapping it submits the form.
                        imeAction     = ImeAction.Done,
                        isError       = uiState.passwordError != null,
                        errorMessage  = uiState.passwordError,
                        // VisualTransformation.None shows the password in plain text.
                        // PasswordVisualTransformation() replaces every character with a dot.
                        // The toggle button below switches between the two.
                        visualTransformation = if (uiState.passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.passwordVisible)
                                        Icons.Outlined.VisibilityOff
                                    else
                                        Icons.Outlined.Visibility,
                                    contentDescription = if (uiState.passwordVisible)
                                        "Hide password"
                                    else
                                        "Show password",
                                    tint     = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.submitEmail()
                            }
                        )
                    )

                    // ── Forgot password ────────────────────────────────────────
                    // Aligned to the end (right side) of the row.
                    // viewModel.forgotPassword() checks if the email field is filled,
                    // shows an error if not, and sends a reset email if it is.
                    TextButton(
                        onClick  = { viewModel.forgotPassword() },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(
                            text  = "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Gold400
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Login button ───────────────────────────────────────────
                    // isLoading = true shows a spinner inside the button and disables it,
                    // preventing double-taps while Firebase is in-flight.
                    HumbleButton(
                        text      = "Login",
                        onClick   = {
                            focusManager.clearFocus()
                            viewModel.submitEmail()
                        },
                        isLoading = uiState.isLoading
                    )

                    Spacer(Modifier.height(24.dp))

                    OrDivider("OR — Continue with")

                    Spacer(Modifier.height(16.dp))

                    // ── Social sign-in buttons ─────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── Google Sign-In button ──────────────────────────────
                        SocialButton(
                            text = "Google",
                            icon = {
                                // While a sign-in is in progress (either email OR Google),
                                // replace the Google "G" icon with a small spinner.
                                // This gives clear visual feedback that something is happening.
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
                            onClick = {
                                // Guard against double-tapping while already loading.
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
                            text = "Phone Number",
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = "Phone Login",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = onNavigateToPhone,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Bottom navigation link ─────────────────────────────────
                    // Shown for users who don't have an account yet.
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(
                            onClick = {
                                // Reset all form fields before navigating to Register
                                // so the RegisterScreen starts fresh.
                                viewModel.clearForm()
                                onNavigateToRegister()
                            }
                        ) {
                            Text(
                                text  = "Create Account",
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