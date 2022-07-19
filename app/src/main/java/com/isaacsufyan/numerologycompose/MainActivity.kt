package com.isaacsufyan.numerologycompose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.isaacsufyan.numerologycompose.component.Home
import com.isaacsufyan.numerologycompose.component.NumerologyDetails
import com.isaacsufyan.numerologycompose.navigation.NavRoutes
import com.isaacsufyan.numerologycompose.ui.theme.NumerologycomposeTheme
import com.isaacsufyan.numerologycompose.viewmodel.SharedViewModel

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    private val viewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NumerologycomposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavGraph(viewModel)
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun NavGraph(viewModel: SharedViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route,
    ) {
        composable(NavRoutes.Home.route) {
            Home(navController = navController, viewModel)
        }

        composable(NavRoutes.NumerologyDetails.route) {
            val result = navController.previousBackStackEntry?.savedStateHandle?.get<Int>("Result")
//            LaunchedEffect(key1 = it) {
//
//            }
            if (result != null) NumerologyDetails(result = result)
        }
    }
}