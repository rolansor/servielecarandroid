package com.example.serviaux

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.serviaux.ui.navigation.Routes
import com.example.serviaux.ui.navigation.ServiauxBottomNavBar
import com.example.serviaux.ui.navigation.ServiauxNavGraph
import com.example.serviaux.ui.theme.ServiauxTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ServiauxTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in setOf(
                    Routes.DASHBOARD,
                    Routes.CUSTOMER_LIST,
                    Routes.VEHICLE_LIST,
                    Routes.WORK_ORDER_LIST
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            ServiauxBottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    val navRoute = if (route == Routes.WORK_ORDER_LIST_BASE) {
                                        Routes.workOrderList()
                                    } else {
                                        route
                                    }
                                    navController.navigate(navRoute) {
                                        popUpTo(Routes.DASHBOARD) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        ServiauxNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
