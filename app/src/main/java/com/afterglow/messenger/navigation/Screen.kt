package com.afterglow.messenger.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ConversationList : Screen("conversations")
    data object Search : Screen("search")

    data object ChooseUsername : Screen("choose-username/{uid}") {
        fun createRoute(uid: String): String = "choose-username/$uid"
    }

    data object Chat : Screen("chat/{conversationId}/{otherUsername}") {
        fun createRoute(conversationId: String, otherUsername: String): String {
            val encoded = URLEncoder.encode(otherUsername, "UTF-8")
            return "chat/$conversationId/$encoded"
        }
    }
}
