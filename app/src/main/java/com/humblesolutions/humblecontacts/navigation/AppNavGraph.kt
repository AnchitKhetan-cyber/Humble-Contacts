package com.humblesolutions.humblecontacts.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.humblesolutions.humblecontacts.ui.auth.AuthViewModel
import com.humblesolutions.humblecontacts.ui.auth.LoginScreen
import com.humblesolutions.humblecontacts.ui.auth.RegisterScreen
import com.humblesolutions.humblecontacts.ui.introduction.IntroductionScreen
import com.humblesolutions.humblecontacts.ui.splash.AnimatedSplashScreen

@Composable
fun AppNavGraph(
    onAuthComplete: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            AnimatedSplashScreen(
                onNavigate = {
                    navController.navigate(Routes.INTRO) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.INTRO) {
            IntroductionScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.LOGIN,
            enterTransition    = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            exitTransition     = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            popExitTransition  = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) }
        ) { backStackEntry ->
            // ✅ Scoped to LOGIN's back stack entry — completely isolated
            val loginViewModel: AuthViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = AuthViewModel.Factory()
            )
            LoginScreen(
                viewModel            = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER) { launchSingleTop = true }
                },
                onLoginSuccess       = onAuthComplete
            )
        }

        composable(
            route = Routes.REGISTER,
            enterTransition    = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            exitTransition     = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popExitTransition  = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            // ✅ Scoped to REGISTER's back stack entry — completely isolated
            val registerViewModel: AuthViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = AuthViewModel.Factory()
            )
            RegisterScreen(
                viewModel         = registerViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = onAuthComplete
            )
        }
    }
}