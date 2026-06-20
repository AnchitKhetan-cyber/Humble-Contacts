package com.humblesolutions.humblecontacts.navigation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.ui.auth.AuthViewModel
import com.humblesolutions.humblecontacts.ui.auth.LoginScreen
import com.humblesolutions.humblecontacts.ui.auth.OtpVerifyScreen
import com.humblesolutions.humblecontacts.ui.auth.PhoneAuthViewModel
import com.humblesolutions.humblecontacts.ui.auth.PhoneInputScreen
import com.humblesolutions.humblecontacts.ui.auth.RegisterScreen
import com.humblesolutions.humblecontacts.ui.contacts.AddContactScreen
import com.humblesolutions.humblecontacts.ui.contacts.ContactDetailScreen
import com.humblesolutions.humblecontacts.ui.contacts.ContactViewModel
import com.humblesolutions.humblecontacts.ui.contacts.ContactsScreen
import com.humblesolutions.humblecontacts.ui.home.HomeScreen
import com.humblesolutions.humblecontacts.ui.introduction.IntroductionScreen
import com.humblesolutions.humblecontacts.ui.profile.ProfileScreen
import com.humblesolutions.humblecontacts.ui.splash.AnimatedSplashScreen
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.humblesolutions.humblecontacts.data.repository.ProfileRepository
import com.humblesolutions.humblecontacts.ui.profile.CompleteProfileScreen
import com.humblesolutions.humblecontacts.ui.profile.DeleteAccountScreen
import com.humblesolutions.humblecontacts.ui.profile.LinkedAccountsScreen
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Defines the navigation graph for the application, including screen transitions,
 * back-stack management, and the shared ViewModel wiring required across screens.
 */

// ─── Transition specifications ────────────────────────────────────────────
//
// All screen transitions are built from two animation specs (slide + fade) so
// that timing and easing stay consistent across the app. Most screens use the
// "standard" set (slideIn / slideOut / popIn / popOut); a few auth screens use
// a fade-only variant to create a softer hand-off between Login and Register.

private const val SLIDE_DURATION_MS = 500
private const val FADE_DURATION_MS = 500

private val slideAnimationSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = SLIDE_DURATION_MS, easing = EaseOutCubic)

private val fadeAnimationSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = FADE_DURATION_MS, easing = EaseOutCubic)

/** Forward navigation: slide in from the right while fading in. */
/** Forward navigation: slide in from the right while fading in. */
private val slideIn: EnterTransition =
    slideInHorizontally(animationSpec = slideAnimationSpec) { fullWidth ->
        fullWidth
    } + fadeIn(fadeAnimationSpec)

/** Forward navigation: slide out to the left while fading out. */
private val slideOut: ExitTransition =
    slideOutHorizontally(animationSpec = slideAnimationSpec) { fullWidth ->
        -fullWidth
    } + fadeOut(fadeAnimationSpec)

/** Back navigation: slide in from the left while fading in. */
private val popIn: EnterTransition =
    slideInHorizontally(animationSpec = slideAnimationSpec) { fullWidth ->
        -fullWidth
    } + fadeIn(fadeAnimationSpec)

/** Back navigation: slide out to the right while fading out. */
private val popOut: ExitTransition =
    slideOutHorizontally(animationSpec = slideAnimationSpec) { fullWidth ->
        fullWidth
    } + fadeOut(fadeAnimationSpec)
/** Fade-only enter, used where a slide would feel too heavy (e.g. Login ↔ Register). */
private val fadeOnlyIn: EnterTransition = fadeIn(fadeAnimationSpec)

/** Fade-only exit, used where a slide would feel too heavy (e.g. Login ↔ Register). */
private val fadeOnlyOut: ExitTransition = fadeOut(fadeAnimationSpec)


private fun NavGraphBuilder.appComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { slideIn },
    exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { slideOut },
    popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { popIn },
    popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { popOut },
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        content = content
    )
}

// ─── Navigation graph ──────────────────────────────────────────────────────

/**
 * Hosts every screen in the app behind a single [NavHost] and centralizes the
 * navigation actions that are shared across multiple screens (returning home,
 * logging out, etc.) so that each screen composable only needs to express the
 * actions specific to it.
 *
 * @param startDestination the route the [NavHost] should display first, typically
 *   [Routes.SPLASH].
 * @param darkMode whether dark mode is currently enabled; forwarded to [ProfileScreen].
 * @param onDarkModeChange callback invoked when the user toggles dark mode from [ProfileScreen].
 */
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavGraph(
    startDestination: String,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    val profileRepository = remember {
        ProfileRepository()
    }

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    // Single source of truth for "go to Home after a successful sign-in", used by
    // Login, Register, and OTP verification alike.
    val navigateAfterAuthentication: () -> Unit = {

        scope.launch {

            val completed =
                profileRepository.isProfileCompleted()

            Log.d("PROFILE_CHECK", "completed = $completed")

            val route =
                if (completed)
                    Routes.HOME
                else
                    Routes.COMPLETE_PROFILE

            navController.navigate(route) {

                popUpTo(0) {
                    inclusive = true
                }

                launchSingleTop = true
            }
        }
    }


    // Single source of truth for "sign out and return to Login".
    val navigateToLogin: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Scoped to the NavHost so contact state survives navigation between
    // Home, Contacts, and Contact Detail.

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideIn },
        exitTransition = { slideOut },
        popEnterTransition = { popIn },
        popExitTransition = { popOut }
    ) {

        // ── Splash ──────────────────────────────────────────────────────────
        appComposable(route = Routes.SPLASH) {
            AnimatedSplashScreen(
                onNavigate = {

                    val isLoggedIn =
                        FirebaseAuth.getInstance().currentUser != null

                    if (!isLoggedIn) {

                        navController.navigate(Routes.INTRO) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                        }

                    } else {

                        navigateAfterAuthentication()

                    }
                }
            )
        }

        // ── Introduction ────────────────────────────────────────────────────
        appComposable(route = Routes.INTRO) {
            IntroductionScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ───────────────────────────────────────────────────────────
        // Uses a fade-only exit/pop-enter so the hand-off to/from Register feels
        // lighter than the standard slide used elsewhere.
        appComposable(
            route = Routes.LOGIN,
            exitTransition = { fadeOnlyOut },
            popEnterTransition = { fadeOnlyIn }
        ) { backStackEntry ->
            val loginViewModel: AuthViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = AuthViewModel.Factory()
            )
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER) { launchSingleTop = true }
                },
                onNavigateToPhone = {
                    navController.navigate(Routes.PHONE_INPUT) { launchSingleTop = true }
                },
                onLoginSuccess = navigateAfterAuthentication
            )
        }

        // ── Register ────────────────────────────────────────────────────────
        // Mirrors Login's fade treatment: fades in on entry, slides out normally,
        // and fades out on the pop back to Login.
        appComposable(
            route = Routes.REGISTER,
            enterTransition = { fadeOnlyIn },
            popExitTransition = { fadeOnlyOut }
        ) { backStackEntry ->
            val registerViewModel: AuthViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = AuthViewModel.Factory()
            )
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToPhone = {
                    navController.navigate(Routes.PHONE_INPUT) { launchSingleTop = true }
                },
                onRegisterSuccess = navigateAfterAuthentication
            )
        }

        appComposable(route = Routes.COMPLETE_PROFILE) {

            CompleteProfileScreen(

                onNavigateHome = {

                    navController.navigate(Routes.HOME) {

                        popUpTo(0) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }

                }

            )
        }

        appComposable(route = Routes.EDIT_PROFILE) {

            CompleteProfileScreen(

                isEditMode = true,

                onNavigateHome = {
                    navController.popBackStack()
                },

                onNavigateBack = {
                    navController.popBackStack()
                }

            )

        }

        // ── Phone Input ─────────────────────────────────────────────────────
        // PhoneAuthViewModel is scoped to this back-stack entry so that
        // OtpVerifyScreen can retrieve the same instance — it holds the
        // verificationId returned by Firebase after the SMS code is sent.
        appComposable(route = Routes.PHONE_INPUT) { backStackEntry ->
            val phoneAuthViewModel: PhoneAuthViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            )
            PhoneInputScreen(
                viewModel = phoneAuthViewModel,
                onBack = { navController.popBackStack() },
                onOtpSent = {
                    navController.navigate(Routes.OTP_VERIFY) { launchSingleTop = true }
                }
            )
        }

        // ── OTP Verify ──────────────────────────────────────────────────────
        // Retrieves the PhoneAuthViewModel scoped to the PHONE_INPUT entry above
        // so both screens share verification state.
        appComposable(route = Routes.OTP_VERIFY) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.PHONE_INPUT)
            }
            val phoneAuthViewModel: PhoneAuthViewModel = viewModel(
                viewModelStoreOwner = parentEntry
            )

            OtpVerifyScreen(
                viewModel = phoneAuthViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = navigateAfterAuthentication
            )
        }

        // ── Home ────────────────────────────────────────────────────────────
        appComposable(route = Routes.HOME) {
            HomeScreen(
                onNavigateToContacts = { navController.navigate(Routes.CONTACTS) },
                onNavigateToContact  = { id -> navController.navigate(Routes.contactDetail(id)) },
                onNavigateToScan     = { navController.navigate(Routes.SCAN) },
                onNavigateToProfile  = { navController.navigate(Routes.PROFILE) },
                onSearchClick        = { navController.navigate(Routes.CONTACTS) },
                onQrCodeScanned = { raw ->
                    when {
                        raw.startsWith("humblecontacts://contact/") -> {
                            val contactId = raw.removePrefix("humblecontacts://contact/")
                            navController.navigate("contact/$contactId")
                        }

                        raw.startsWith("BEGIN:VCARD") -> {
                            val encoded = Uri.encode(raw)
                            navController.navigate("addContact?vcard=$encoded")
                        }

                        // ── Handle your JSON contact QR ──────────────────────────────
                        raw.startsWith("{") -> {
                            try {
                                val json = JSONObject(raw)
                                val contactId = json.optString("contactId")
                                if (contactId.isNotBlank()) {
                                    navController.navigate("contact/$contactId")
                                } else {
                                    Toast.makeText(context, "Invalid contact QR", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not read QR", Toast.LENGTH_SHORT).show()
                            }
                        }
                        // ─────────────────────────────────────────────────────────────

                        raw.startsWith("tel:") -> {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse(raw)
                            )

                            context.startActivity(intent)
                        }

                        raw.startsWith("mailto:") -> {
                            val intent = Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse(raw)
                            )

                            context.startActivity(intent)
                        }

                        raw.startsWith("http://") || raw.startsWith("https://") -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(raw))
                            context.startActivity(intent)
                        }

                        else -> {
                            Log.d("QR_CONTENT", raw)

                            Toast.makeText(
                                context,
                                raw,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }

        // ── Contacts ────────────────────────────────────────────────────────
        appComposable(route = Routes.CONTACTS) {
            ContactsScreen(
                onNavigateToContact = { id -> navController.navigate(Routes.contactDetail(id)) },
                onNavigateToHome = { navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                }},
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToAdd = { navController.navigate(Routes.ADD_CONTACT) }
            )
        }

        // ── Contact Detail ──────────────────────────────────────────────────
        appComposable(
            route = Routes.CONTACT_DETAIL,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactDetailScreen(
                contactId = contactId,
                onBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                } },
                onNavigateToContacts = { navController.popBackStack() },
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) }
            )
        }

        // ── Add Contact ─────────────────────────────────────────────────────
        appComposable(route = Routes.ADD_CONTACT) {
            AddContactScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }

        // ── Scan ────────────────────────────────────────────────────────────
        // TODO: Replace with a dedicated ScanScreen once it is implemented;
        // AddContactScreen is reused here as a temporary placeholder.
        appComposable(route = Routes.SCAN) {
            AddContactScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },

                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },

                onNavigateToContacts = {
                    navController.navigate(Routes.CONTACTS) {
                        launchSingleTop = true
                    }
                },

                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Profile ─────────────────────────────────────────────────────────
        appComposable(route = Routes.PROFILE) {
            ProfileScreen(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                onNavigateToHome = { navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                } },
                onNavigateToContacts = { navController.navigate(Routes.CONTACTS) { launchSingleTop = true } },
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToDeleteAccount = {
                    navController.navigate(Routes.DELETE_ACCOUNT)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Routes.EDIT_PROFILE)
                },
                onNavigateToLinkedAccounts = {
                    navController.navigate(Routes.LINKED_ACCOUNTS)
                },
                onLogout = navigateToLogin
            )
        }

        appComposable(route = Routes.LINKED_ACCOUNTS) {

            LinkedAccountsScreen(

                onBack = {
                    navController.popBackStack()
                }

            )

        }

        appComposable(route = Routes.DELETE_ACCOUNT) {
            DeleteAccountScreen(
                onBack = {
                    navController.popBackStack()
                },
                onDeleteSuccess = {
                    navigateToLogin()
                }
            )
        }
    }
}