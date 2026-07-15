package com.afterglow.messenger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afterglow.messenger.ui.auth.ChooseUsernameScreen
import com.afterglow.messenger.ui.auth.LoginScreen
import com.afterglow.messenger.ui.auth.RegisterScreen
import com.afterglow.messenger.ui.chat.ChatScreen
import com.afterglow.messenger.ui.conversations.ConversationListScreen
import com.afterglow.messenger.ui.search.UserSearchScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AfterglowNavGraph() {
    val navController = rememberNavController()

    // Firebase Auth persists sessions on its own, so a signed-in user skips
    // straight to their conversations — this is what satisfies "log in
    // automatically and remain signed in unless they log out."
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.ConversationList.route
    } else {
        Screen.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Screen.ConversationList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNeedsUsername = { uid ->
                    navController.navigate(Screen.ChooseUsername.createRoute(uid))
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Screen.ConversationList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNeedsUsername = { uid ->
                    navController.navigate(Screen.ChooseUsername.createRoute(uid))
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ChooseUsername.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = checkNotNull(backStackEntry.arguments?.getString("uid"))
            ChooseUsernameScreen(
                uid = uid,
                onDone = {
                    navController.navigate(Screen.ConversationList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ConversationList.route) {
            ConversationListScreen(
                onOpenConversation = { conversationId, otherUsername ->
                    navController.navigate(Screen.Chat.createRoute(conversationId, otherUsername))
                },
                onNewConversation = { navController.navigate(Screen.Search.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Search.route) {
            UserSearchScreen(
                onConversationStarted = { conversationId, otherUsername ->
                    navController.navigate(Screen.Chat.createRoute(conversationId, otherUsername)) {
                        popUpTo(Screen.ConversationList.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("otherUsername") { type = NavType.StringType }
            )
        ) {
            ChatScreen(onBack = { navController.popBackStack() })
        }
    }
}
