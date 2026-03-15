package com.FreeRave.remindme.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Add : Screen("add") {
        fun createRoute() = "add"
    }
    
    object Edit : Screen("edit/{reminderId}") {
        fun createRoute(reminderId: String) = "edit/$reminderId"
    }
    object Settings : Screen("settings")
    object AccountSecurity : Screen("account_security") 
    object ActiveSessions : Screen("active_sessions") // Added new screen
    object SignOut : Screen("signout")
    object VerifyEmail : Screen("verify_email")
    object AiChat : Screen("ai_chat") // Added AI Chat screen
    object Categories : Screen("categories") // Added Categories screen
    object Templates : Screen("templates") // Added Templates screen
    object Support : Screen("support") // Added Support screen
}
