@file:OptIn(ExperimentalMaterial3Api::class)

package com.khanabook.lite.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.khanabook.lite.pos.domain.util.CurrencyUtils
import com.khanabook.lite.pos.ui.theme.*
import com.khanabook.lite.pos.ui.viewmodel.HomeViewModel

import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun HomeScreen(
    onNewBill: () -> Unit,
    onSearchBill: () -> Unit,
    onOrderStatus: () -> Unit,
    onCallCustomer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val stats by viewModel.todayStats.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard",
                    color = PrimaryGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                SyncStatusHeader(connectionStatus, unsyncedCount)
            }


            Text(
                text = "Welcome back! Manage your restaurant billing efficiently.",
                color = TextGold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkBrown2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Today's Summary",
                        color = PrimaryGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Orders", stats.orderCount.toString(), Modifier.weight(1f))
                        StatItem("Revenue", CurrencyUtils.formatPrice(stats.revenue), Modifier.weight(1f))
                        StatItem("Customers", stats.customerCount.toString(), Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onNewBill() },
                colors = CardDefaults.cardColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Create New Bill",
                            color = DarkBrown1,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start taking orders",
                            color = DarkBrown2,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = DarkBrown1,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isWideScreen) {
                // Adaptive grid for tablets/landscape
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeActionCard(
                        text = "Print/Share",
                        icon = Icons.Default.Print,
                        backgroundColor = DarkBrown2,
                        modifier = Modifier.weight(1f),
                        onClick = onSearchBill
                    )
                    HomeActionCard(
                        text = "Order Status",
                        icon = Icons.Default.Info,
                        backgroundColor = DarkBrown2,
                        modifier = Modifier.weight(1f),
                        onClick = onOrderStatus
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeActionCard(
                        text = "Call Customers",
                        icon = Icons.Default.Call,
                        backgroundColor = DarkBrown2,
                        modifier = Modifier.weight(1f),
                        onClick = onCallCustomer
                    )
                    Spacer(modifier = Modifier.weight(1f)) // Empty space for balance
                }
            } else {
                // Vertical list for phones
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeActionCard(
                        text = "Print/Share",
                        icon = Icons.Default.Print,
                        backgroundColor = DarkBrown2,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSearchBill
                    )

                    HomeActionCard(
                        text = "Order Status",
                        icon = Icons.Default.Info,
                        backgroundColor = DarkBrown2,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOrderStatus
                    )

                    HomeActionCard(
                        text = "Call Customers",
                        icon = Icons.Default.Call,
                        backgroundColor = DarkBrown2,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCallCustomer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SyncStatusHeader(
    connectionStatus: com.khanabook.lite.pos.domain.util.ConnectionStatus,
    unsyncedCount: Int
) {
    val isOnline = connectionStatus == com.khanabook.lite.pos.domain.util.ConnectionStatus.Available
    
    val viewModel: com.khanabook.lite.pos.ui.viewmodel.AuthViewModel = hiltViewModel()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSessionValid = currentUser != null
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                when {
                    !isOnline -> DangerRed.copy(alpha = 0.1f)
                    !isSessionValid -> WarningYellow.copy(alpha = 0.1f)
                    unsyncedCount > 0 -> PrimaryGold.copy(alpha = 0.1f)
                    else -> Green800.copy(alpha = 0.1f)
                },
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = when {
                !isOnline -> Icons.Default.CloudOff
                !isSessionValid -> Icons.Default.Lock
                unsyncedCount > 0 -> Icons.Default.CloudSync
                else -> Icons.Default.CloudDone
            },
            contentDescription = null,
            tint = when {
                !isOnline -> DangerRed
                !isSessionValid -> WarningYellow
                unsyncedCount > 0 -> PrimaryGold
                else -> SuccessGreen
            },
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = when {
                !isOnline -> "Offline"
                !isSessionValid -> "Auth Required"
                unsyncedCount > 0 -> "Syncing ($unsyncedCount)"
                else -> "Cloud Synced"
            },
            color = when {
                !isOnline -> DangerRed
                !isSessionValid -> WarningYellow
                else -> TextLight
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun HomeActionCard(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(70.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    color = TextLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PrimaryGold,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = PrimaryGold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextGold,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

