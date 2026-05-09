package com.humblesolutions.humblecontacts.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.ui.auth.AuthViewModel
import com.humblesolutions.humblecontacts.ui.auth.LoginScreen
import com.humblesolutions.humblecontacts.ui.auth.RegisterScreen
import com.humblesolutions.humblecontacts.ui.contacts.AddContactScreen
import com.humblesolutions.humblecontacts.ui.contacts.ContactDetailScreen
import com.humblesolutions.humblecontacts.ui.contacts.ContactsScreen
import com.humblesolutions.humblecontacts.ui.home.HomeScreen
import com.humblesolutions.humblecontacts.ui.introduction.IntroductionScreen
import com.humblesolutions.humblecontacts.ui.nfc.NfcScreen
import com.humblesolutions.humblecontacts.ui.profile.ProfileScreen
import com.humblesolutions.humblecontacts.ui.splash.AnimatedSplashScreen

// ─── Transition specs ─────────────────────────────────────────────────────────

private const val SLIDE_DURATION = 250
private const val FADE_DURATION  = 200

private val slideTween: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = SLIDE_DURATION, easing = FastOutSlowInEasing)

private val fadeTween: FiniteAnimationSpec<Float> =
    tween(durationMillis = FADE_DURATION, easing = FastOutSlowInEasing)

private val slideIn:     EnterTransition = slideInHorizontally(slideTween)  {  it / 4 } + fadeIn(fadeTween)
private val slideOut:    ExitTransition  = slideOutHorizontally(slideTween) { -it / 4 } + fadeOut(fadeTween)
private val popIn:       EnterTransition = slideInHorizontally(slideTween)  { -it / 4 } + fadeIn(fadeTween)
private val popOut:      ExitTransition  = slideOutHorizontally(slideTween) {  it / 4 } + fadeOut(fadeTween)
private val fadeOnly:    EnterTransition = fadeIn(fadeTween)
private val fadeOnlyOut: ExitTransition  = fadeOut(fadeTween)


// ─── Nav graph ────────────────────────────────────────────────────────────────

@Composable
fun AppNavGraph(
    startDestination: String,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    // ── Single source of truth for "go to Home after auth" ───────────────────
    // Used by both Login and Register so there's no duplication
    val navigateToHome: () -> Unit = {
        navController.navigate(Routes.HOME) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    // ── Single source of truth for "log out and go to Login" ─────────────────
    val navigateToLogin: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController      = navController,
        startDestination   = startDestination,
        enterTransition    = { fadeIn(tween(0)) },
        exitTransition     = { fadeOut(tween(0)) },
        popEnterTransition = { fadeIn(tween(0)) },
        popExitTransition  = { fadeOut(tween(0)) }
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(route = Routes.SPLASH) {
            AnimatedSplashScreen(
                onNavigate = {
                    navController.navigate(Routes.INTRO) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Intro ─────────────────────────────────────────────────────────────
        composable(
            route              = Routes.INTRO,
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) {
            IntroductionScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(
            route              = Routes.LOGIN,
            enterTransition    = { slideIn },
            exitTransition     = { fadeOnlyOut },
            popEnterTransition = { fadeOnly },
            popExitTransition  = { popOut }
        ) { backStackEntry ->
            val loginViewModel: AuthViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory             = AuthViewModel.Factory()
            )
            LoginScreen(
                viewModel            = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER) { launchSingleTop = true }
                },
                onLoginSuccess = navigateToHome   // ✅ directly navigates, no lambda indirection
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(
            route              = Routes.REGISTER,
            enterTransition    = { fadeOnly },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { fadeOnlyOut }
        ) { backStackEntry ->
            val registerViewModel: AuthViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory             = AuthViewModel.Factory()
            )
            RegisterScreen(
                viewModel         = registerViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = navigateToHome   // ✅ same navigateToHome
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(
            route              = Routes.HOME,
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) {
            HomeScreen(
                onNavigateToContacts = { navController.navigate(Routes.CONTACTS) },
                onNavigateToContact  = { id -> navController.navigate(Routes.contactDetail(id)) },
                onNavigateToScan     = { navController.navigate(Routes.SCAN) },
                onNavigateToNfc      = { navController.navigate(Routes.NFC) },
                onNavigateToProfile  = { navController.navigate(Routes.PROFILE) },
                onSearchClick        = { navController.navigate(Routes.CONTACTS) }
            )
        }

        // ── Contacts ──────────────────────────────────────────────────────────
        composable(
            route              = Routes.CONTACTS,
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) {
            ContactsScreen(
                onNavigateToContact = { id -> navController.navigate(Routes.contactDetail(id)) },
                onNavigateToHome    = { navController.navigate(Routes.HOME) { launchSingleTop = true } },
                onNavigateToScan    = { navController.navigate(Routes.SCAN) },
                onNavigateToNfc     = { navController.navigate(Routes.NFC) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToAdd     = { navController.navigate(Routes.ADD_CONTACT) }
            )
        }

        // ── Contact Detail ────────────────────────────────────────────────────
        composable(
            route              = Routes.CONTACT_DETAIL,
            arguments          = listOf(navArgument("contactId") { type = NavType.StringType }),
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactDetailScreen(
                contactId            = contactId,
                onBack               = { navController.popBackStack() },
                onNavigateToHome     = { navController.navigate(Routes.HOME) { launchSingleTop = true } },
                onNavigateToContacts = { navController.popBackStack() },
                onNavigateToScan     = { navController.navigate(Routes.SCAN) },
                onNavigateToNfc      = { navController.navigate(Routes.NFC) },
                onNavigateToProfile  = { navController.navigate(Routes.PROFILE) }
            )
        }

        // ── Add Contact ───────────────────────────────────────────────────────
        composable(
            route              = Routes.ADD_CONTACT,
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) {
            AddContactScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }

        // ── Scan ──────────────────────────────────────────────────────────────
        composable(
            route              = Routes.SCAN,
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) {
            AddContactScreen(   // placeholder until ScanScreen is ready
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }

        // ── NFC ───────────────────────────────────────────────────────────────
        composable(
            route              = Routes.NFC,
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) {
            NfcScreen(
                onBack               = { navController.popBackStack() },
                onNavigateToHome     = { navController.navigate(Routes.HOME) { launchSingleTop = true } },
                onNavigateToContacts = { navController.navigate(Routes.CONTACTS) { launchSingleTop = true } },
                onNavigateToScan     = { navController.navigate(Routes.SCAN) },
                onNavigateToProfile  = { navController.navigate(Routes.PROFILE) }
            )
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(
            route              = Routes.PROFILE,
            enterTransition    = { slideIn },
            exitTransition     = { slideOut },
            popEnterTransition = { popIn },
            popExitTransition  = { popOut }
        ) {
            ProfileScreen(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,

                onNavigateToHome     = {
                    navController.navigate(Routes.HOME) {
                        launchSingleTop = true
                    }
                },

                onNavigateToContacts = {
                    navController.navigate(Routes.CONTACTS) {
                        launchSingleTop = true
                    }
                },

                onNavigateToScan = {
                    navController.navigate(Routes.SCAN)
                },

                onNavigateToNfc = {
                    navController.navigate(Routes.NFC)
                },

                onLogout = navigateToLogin
            )
        }
    }
}