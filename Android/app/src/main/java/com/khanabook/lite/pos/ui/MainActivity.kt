package com.khanabook.lite.pos.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.khanabook.lite.pos.R
import com.khanabook.lite.pos.domain.manager.SessionManager
import com.khanabook.lite.pos.domain.util.ConnectionStatus
import com.khanabook.lite.pos.domain.util.NetworkMonitor
import com.khanabook.lite.pos.ui.screens.*
import com.khanabook.lite.pos.ui.theme.KhanaBookLiteTheme
import com.khanabook.lite.pos.ui.viewmodel.AuthViewModel
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    private var lastBackPressTime: Long = 0

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // This is triggered when the user intentionally leaves the app (Home gesture, Home button, Recents)
        // Useful for detecting if the user swiped to Home or switched apps.
        android.util.Log.d("MainActivity", "User leaving app - intentionally navigated away (Home/Recents gesture)")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KhanaBookLiteTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val menuViewModel: MenuViewModel = hiltViewModel()
                val currentUser by authViewModel.currentUser.collectAsState()
                val networkMonitor = remember { NetworkMonitor(this) }
                val connectionStatus by networkMonitor.status.collectAsState(initial = null)
                val context = this

                // Handle Double Back Press to Exit
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route
                
                // If on a top-level screen, intercept back and check for double-tap
                if (currentRoute?.startsWith("main/") == true) {
                    androidx.activity.compose.BackHandler {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000) {
                            finish()
                        } else {
                            Toast.makeText(context, context.getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
                            lastBackPressTime = currentTime
                        }
                    }
                }

                // Network Status Monitoring
                var lastStatus by remember { mutableStateOf<ConnectionStatus?>(null) }
                LaunchedEffect(connectionStatus) {
                    if (lastStatus != null && connectionStatus != null && lastStatus != connectionStatus) {
                        val message = if (connectionStatus == ConnectionStatus.Available) {
                            context.getString(R.string.back_online)
                        } else {
                            context.getString(R.string.offline_working_locally)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    if (connectionStatus != null) {
                        lastStatus = connectionStatus
                    }
                }

                // Authentication Observer
                LaunchedEffect(currentUser) {
                    val currentRoute = navController.currentDestination?.route
                    if (currentUser == null && currentRoute != "splash") {
                        navController.navigate("login") { 
                            popUpTo(0) { inclusive = true } 
                        }
                    }
                }

                val startDestination = "splash"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("splash") {
                        SplashScreen(
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToMain = {
                                navController.navigate("main/0") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToInitialSync = {
                                navController.navigate("initial_sync") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("initial_sync") {
                        InitialSyncScreen(
                            onSyncCompleteNavigateToMain = {
                                navController.navigate("main/0") {
                                    popUpTo("initial_sync") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                if (sessionManager.isInitialSyncCompleted()) {
                                    navController.navigate("main/0") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("initial_sync") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            },
                            onSignUpClick = { navController.navigate("signup") }
                        )
                    }
                    composable("signup") {
                        SignUpScreen(
                            onSignUpSuccess = {
                                if (sessionManager.isInitialSyncCompleted()) {
                                    navController.navigate("main/0") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("initial_sync") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                }
                            },
                            onLoginClick = { navController.popBackStack() }
                        )
                    }
                    composable("main/{tab}") { backStackEntry ->
                        val selectedTab = backStackEntry.arguments?.getString("tab")?.toIntOrNull() ?: 0
                        MainScreen(
                            initialTab = selectedTab,
                            onNewBill = { navController.navigate("new_bill") },
                            onSearchBill = { navController.navigate("search_bill") },
                            onOrderStatus = { navController.navigate("order_status") },
                            onCallCustomer = { navController.navigate("call_customer") },
                            menuViewModel = menuViewModel,
                            onScanClick = { categoryName ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("ocr_category_name", categoryName)
                                navController.navigate("ocr_scanner/menu_config")
                            }
                        )
                    }
                    composable("new_bill") {
                        NewBillScreen(
                            onBack = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize(),
                            navController = navController
                        )
                    }
                    composable("ocr_scanner/{source}") { backStackEntry ->
                        val source = backStackEntry.arguments?.getString("source") ?: "menu_config"
                        val isBarcodeScan = source == "billing"
                        val selectedCategoryName = if (!isBarcodeScan) {
                            navController.previousBackStackEntry?.savedStateHandle?.get<String>("ocr_category_name")
                        } else null
                        
                        OcrScannerScreen(
                            selectedCategoryName = selectedCategoryName,
                            viewModel = menuViewModel,
                            navController = navController,
                            returnBarcode = isBarcodeScan,
                            onBack = {
                                if (!isBarcodeScan) {
                                    navController.previousBackStackEntry?.savedStateHandle?.remove<String>("ocr_category_name")
                                }
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("search_bill") {
                        SearchScreen(
                            title = context.getString(R.string.search_bill),
                            onBack = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("order_status") {
                        SearchScreen(
                            title = context.getString(R.string.check_order_status),
                            onBack = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("call_customer") {
                        CallCustomerScreen(
                            onBack = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
