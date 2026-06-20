//package com.humblecontacts.navigation
//
//import androidx.compose.animation.AnimatedContentTransitionScope
//import androidx.compose.runtime.Composable
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.humblesolutions.humblecontacts.ui.auth.AuthViewModel
//import com.humblesolutions.humblecontacts.ui.auth.LoginScreen
//import com.humblesolutions.humblecontacts.ui.auth.RegisterScreen
//
//
//object Routes {
//    const val LOGIN    = "login"
//    const val REGISTER = "register"
//    const val HOME     = "home"     // Your main app destination
//}
//
//// ─── Auth Nav Graph ──────────────────────────────────────────────────────
//
//@Composable
//fun AuthNavGraph(
//    navController: NavHostController = rememberNavController(),
//    startDestination: String = Routes.LOGIN,
//    onAuthComplete: () -> Unit            // Called when auth succeeds → navigate to Home
//) {
//    // Share ONE ViewModel instance across Login & Register
//    val authViewModel: AuthViewModel = viewModel()
//
//    NavHost(
//        navController    = navController,
//        startDestination = startDestination
//    ) {
//
//        composable(
//            route = Routes.LOGIN,
//            enterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
//            exitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
//            popEnterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
//            popExitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) }
//        ) {
//            LoginScreen(
//                onNavigateToRegister = {
//                    navController.navigate(Routes.REGISTER) {
//                        launchSingleTop = true
//                    }
//                },
//                onLoginSuccess = onAuthComplete,
//                viewModel          = authViewModel,
//            )
//        }
//
//        composable(
//            route = Routes.REGISTER,
//            enterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
//            exitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
//            popEnterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
//            popExitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
//        ) {
//            RegisterScreen(
//                viewModel        = authViewModel,
//                onNavigateToLogin  = { navController.popBackStack() },
//                onRegisterSuccess  = onAuthComplete
//            )
//        }
//    }
//}