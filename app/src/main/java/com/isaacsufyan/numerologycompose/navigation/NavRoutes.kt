package com.isaacsufyan.numerologycompose.navigation

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("Home")
    object NumerologyDetails : NavRoutes("NumerologyDetails")
}