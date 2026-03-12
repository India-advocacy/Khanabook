package com.khanabook.lite.pos.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.khanabook.lite.pos.ui.screens.HomeScreen
import com.khanabook.lite.pos.ui.screens.ReportsScreen
import com.khanabook.lite.pos.ui.screens.OrdersScreen
import com.khanabook.lite.pos.ui.screens.SettingsScreen
import com.khanabook.lite.pos.ui.theme.DarkBrown1
import com.khanabook.lite.pos.ui.theme.PrimaryGold
import com.khanabook.lite.pos.ui.theme.TextLight

import com.khanabook.lite.pos.ui.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.khanabook.lite.pos.ui.navigation.TabItem
import com.khanabook.lite.pos.ui.navigation.NavigationUtils

@Composable
fun MainScreen(
    initialTab: Int = 0,
    onNewBill: () -> Unit,
    onSearchBill: () -> Unit,
    onOrderStatus: () -> Unit,
    onCallCustomer: () -> Unit,
    onScanClick: (String?) -> Unit = {},
    scannedText: String? = null,
    onScannedTextConsumed: () -> Unit = {},
    returnToSettingsMenu: Boolean = false,
    onSettingsMenuReturnConsumed: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val visibleTabs = remember { NavigationUtils.getVisibleTabs() }

    var selectedTabIndex by remember(initialTab, visibleTabs) { 
        val initialVisibleIndex = visibleTabs.indexOfFirst { it.originalIndex == initialTab }
        mutableIntStateOf(if (initialVisibleIndex != -1) initialVisibleIndex else 0) 
    }
    
    Scaffold(
        bottomBar = {
            AppBottomBar(
                visibleTabs = visibleTabs,
                currentSelectedIndex = selectedTabIndex, 
                onTabSelected = { selectedTabIndex = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val currentTab = visibleTabs[selectedTabIndex]
            val backToHome = { selectedTabIndex = 0 }
            when (currentTab.label) {
                "Home" -> HomeScreen(onNewBill, onSearchBill, onOrderStatus, onCallCustomer)
                "Reports" -> ReportsScreen(onBack = backToHome)
                "Orders" -> OrdersScreen(onBack = backToHome)
                "Settings" -> SettingsScreen(
                    onBack = backToHome,
                    onScanClick = onScanClick,
                    scannedText = scannedText,
                    onScannedTextConsumed = onScannedTextConsumed,
                    returnToMenuRoot = returnToSettingsMenu,
                    onReturnToMenuRootConsumed = onSettingsMenuReturnConsumed
                )
            }
        }
    }
}

@Composable
fun AppBottomBar(
    visibleTabs: List<TabItem>,
    currentSelectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(containerColor = DarkBrown1) {
        visibleTabs.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = currentSelectedIndex == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryGold,
                    unselectedIconColor = TextLight,
                    selectedTextColor = PrimaryGold,
                    unselectedTextColor = TextLight,
                    indicatorColor = DarkBrown1
                )
            )
        }
    }
}
