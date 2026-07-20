package com.r2r.readytorevise.navigation

sealed class Screen(val route: String) {

    data object Login : Screen("login")

    data object Register : Screen("register")

    data object Dashboard : Screen("dashboard")

    data object Upload : Screen("upload")

    data object Revision : Screen("revision")

    data object Analytics : Screen("analytics")

    data object Profile : Screen("profile")
}
