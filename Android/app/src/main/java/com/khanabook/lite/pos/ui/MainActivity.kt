package com.khanabook.lite.pos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.khanabook.lite.pos.ui.navigation.*
import com.khanabook.lite.pos.ui.screens.*
import com.khanabook.lite.pos.ui.theme.KhanaBookLiteTheme
import com.khanabook.lite.pos.ui.viewmodel.AuthViewModel
import com.khanabook.lite.pos.domain.manager.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KhanaBookLiteTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val currentUser by authViewModel.currentUser.collectAsState()
                val networkMonitor = remember { com.khanabook.lite.pos.domain.util.NetworkMonitor(this) }
                val connectionStatus by networkMonitor.status.collectAsState(initial = null)
                val context = this

                // Network Status Toast
                var lastStatus by remember { mutableStateOf<com.khanabook.lite.pos.domain.util.ConnectionStatus?>(null) }
                LaunchedEffect(connectionStatus) {
                    if (lastStatus != null && connectionStatus != null && lastStatus != connectionStatus) {
                        val message = if (connectionStatus == com.khanabook.lite.pos.domain.util.ConnectionStatus.Available) {
                            "Back online"
                        } else {
                            "Offline - Working Locally"
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    if (connectionStatus != null) {
                        lastStatus = connectionStatus
                    }
                }

                // Global Logout Listener
                LaunchedEffect(currentUser) {
                    val currentRoute = navController.currentDestination?.route
                    if (currentUser == null && currentRoute != "splash") {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }

                val navigateToMainTab: (Int) -> Unit = { tab ->
                    navController.navigate("main/$tab") {
                        launchSingleTop = true
                        popUpTo("main/{tab}") { saveState = true }
                        restoreState = true
                    }
                }

                val startDestination = remember {
                    val token = sessionManager.getAuthToken()
                    val isSyncCompleted = sessionManager.isInitialSyncCompleted()
                    when {
                        token == null -> "splash"
                        !isSyncCompleted -> "initial_sync"
                        else -> "splash"
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("splash") {
                        SplashScreen(
                                onTimeout = {
                                    if (currentUser != null || sessionManager.getAuthToken() != null) {
                                        if (sessionManager.isInitialSyncCompleted()) {
                                            navController.navigate("main/0") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("initial_sync") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    } else {
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
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
                                onSignUpClick = { navController.navigate("signup") },

                        )
                    }
                    composable("signup") {
                        SignUpScreen(
                                onSignUpSuccess = {
                                    // Navigate to initial_sync immediately assuming backend returns token and it's saved in SessionManager inside auth flow. Or login directly.
                                    navController.navigate("initial_sync") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                },
                                onLoginClick = { navController.popBackStack() }
                        )
                    }
                    composable("main/{tab}") { backStackEntry ->
                        val selectedTab =
                                backStackEntry.arguments?.getString("tab")?.toIntOrNull() ?: 0
                        val scannedText = backStackEntry.savedStateHandle.get<String>("scanned_text_menu_config")
                        val returnToSettingsMenu =
                                backStackEntry.savedStateHandle.get<Boolean>("menu_config_return_to_menu") == true
                        MainScreen(
                                initialTab = selectedTab,
                                onNewBill = { navController.navigate("new_bill") },
                                onSearchBill = { navController.navigate("search_bill") },
                                onOrderStatus = { navController.navigate("order_status") },
                                onCallCustomer = { navController.navigate("call_customer") },
                                onScanClick = { categoryName ->
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("ocr_category_name", categoryName)
                                    navController.navigate("ocr_scanner/menu_config")
                                },
                                scannedText = scannedText,
                                onScannedTextConsumed = { backStackEntry.savedStateHandle.remove<String>("scanned_text_menu_config") },
                                returnToSettingsMenu = returnToSettingsMenu,
                                onSettingsMenuReturnConsumed = {
                                    backStackEntry.savedStateHandle.remove<Boolean>("menu_config_return_to_menu")
                                }
                        )
                    }
                    composable("new_bill") {
                        NewBillScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("ocr_scanner/{source}") { backStackEntry ->
                        val source = backStackEntry.arguments?.getString("source") ?: "new_bill"
                        val selectedCategoryName =
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<String>("ocr_category_name")
                        OcrScannerScreen(
                                selectedCategoryName = selectedCategoryName,
                                onTextScanned = { text ->
                                    navController.previousBackStackEntry?.savedStateHandle?.set("scanned_text_$source", text)
                                    navController.previousBackStackEntry?.savedStateHandle?.set("menu_config_return_to_menu", true)
                                    navController.previousBackStackEntry?.savedStateHandle?.remove<String>("ocr_category_name")
                                    navController.popBackStack()
                                },
                                onBack = {
                                    navController.previousBackStackEntry?.savedStateHandle?.remove<String>("ocr_category_name")
                                    navController.popBackStack()
                                }
                        )
                    }
                    composable("search_bill") {
                        SearchScreen(
                                title = "Search & Bill",
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("order_status") {
                        SearchScreen(
                                title = "Check Order Status",
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
