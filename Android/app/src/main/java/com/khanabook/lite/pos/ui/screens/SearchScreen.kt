@file:OptIn(ExperimentalMaterial3Api::class)

package com.khanabook.lite.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.khanabook.lite.pos.domain.util.*
import com.khanabook.lite.pos.ui.components.KhanaDatePickerField
import com.khanabook.lite.pos.ui.theme.*
import com.khanabook.lite.pos.ui.viewmodel.BillingViewModel
import com.khanabook.lite.pos.ui.viewmodel.SearchViewModel
import com.khanabook.lite.pos.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
        title: String,
        onBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: SearchViewModel = hiltViewModel(),
        settingsViewModel: SettingsViewModel = hiltViewModel(),
        billingViewModel: BillingViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var lifetimeQuery by remember { mutableStateOf("") }
    var dailyId by remember { mutableStateOf("") }
    var dailyDate by remember {
        mutableStateOf(
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date())
        )
    }
    val result by viewModel.searchResult.collectAsState()
    val profile by settingsViewModel.profile.collectAsState()
    val context = LocalContext.current

    Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                        title = {
                            Text(
                                    title,
                                    color = PrimaryGold,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = PrimaryGold
                                )
                            }
                        },
                        actions = {
                            if (result != null) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = PrimaryGold
                                    )
                                }
                            }
                        },
                        colors =
                                TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = DarkBrown1
                                )
                )
            }
    ) { padding ->
        Column(
                modifier =
                        Modifier.padding(padding)
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(DarkBrown1, DarkBrown2)))
                                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Inputs Section
            Column(modifier = Modifier.wrapContentHeight()) {
                TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkBrown1,
                        contentColor = PrimaryGold
                ) {
                    Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Daily ID") }
                    )
                    Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Lifetime ID") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                            value = dailyId,
                            onValueChange = { dailyId = it },
                            label = { Text("Daily Order ID", color = TextGold) },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextLight,
                                            unfocusedTextColor = TextLight,
                                            focusedBorderColor = PrimaryGold,
                                            unfocusedBorderColor = BorderGold
                                    ),
                            singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    KhanaDatePickerField(
                            label = "Select Date",
                            selectedDate = dailyDate,
                            onDateSelected = { dailyDate = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                            onClick = {
                                if (dailyId.isNotBlank() && dailyDate.isNotBlank()) {
                                    viewModel.searchByDailyId(dailyId, dailyDate)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            enabled = dailyId.isNotEmpty()
                    ) {
                        Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = DarkBrown1,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Order", color = DarkBrown1, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                            value = lifetimeQuery,
                            onValueChange = { lifetimeQuery = it },
                            label = { Text("Lifetime Order ID", color = TextGold) },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextLight,
                                            unfocusedTextColor = TextLight,
                                            focusedBorderColor = PrimaryGold,
                                            unfocusedBorderColor = BorderGold
                                    ),
                            singleLine = true,
                            keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType =
                                                    androidx.compose.ui.text.input.KeyboardType
                                                            .Number
                                    )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                            onClick = {
                                lifetimeQuery.toIntOrNull()?.let {
                                    viewModel.searchByLifetimeId(it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            enabled = lifetimeQuery.isNotEmpty()
                    ) {
                        Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = DarkBrown1,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Order", color = DarkBrown1, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Result Section
            val currentResult = result
            if (currentResult != null) {
                Card(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        colors = CardDefaults.cardColors(containerColor = DarkBrown2),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        border =
                                androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        BorderGold.copy(alpha = 0.5f)
                                )
                ) {
                    Column(modifier = Modifier.padding(16.dp).wrapContentHeight()) {
                        // Constant Header
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        "Order #${currentResult.bill.lifetimeOrderId}",
                                        color = PrimaryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                )
                                Text(
                                        currentResult.bill.createdAt,
                                        color = TextGold,
                                        fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        "Phone: ${currentResult.bill.customerWhatsapp ?: "N/A"}",
                                        color = TextLight,
                                        fontSize = 12.sp
                                )
                            }
                            Surface(
                                    color =
                                            if (currentResult.bill.paymentStatus == "success")
                                                    SuccessGreen.copy(alpha = 0.1f)
                                            else DangerRed.copy(alpha = 0.1f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Text(
                                        currentResult.bill.paymentStatus.uppercase(),
                                        color =
                                                if (currentResult.bill.paymentStatus == "success")
                                                        SuccessGreen
                                                else DangerRed,
                                        fontSize = 10.sp,
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                ),
                                        fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = BorderGold.copy(alpha = 0.2f)
                        )

                        // Scrollable Body (Items)
                        Column(
                                modifier =
                                        Modifier.wrapContentHeight()
                                                .verticalScroll(rememberScrollState())
                        ) {
                            currentResult.items.forEach { item ->
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                            "${item.itemName} x${item.quantity}",
                                            color = TextLight,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                            "₹${String.format("%.2f", item.itemTotal)}",
                                            color = TextLight,
                                            fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Constant Footer
                        HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = BorderGold.copy(alpha = 0.2f)
                        )

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                    "Total Amount",
                                    color = PrimaryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                            )
                            Text(
                                    "₹${String.format("%.2f", currentResult.bill.totalAmount)}",
                                    color = PrimaryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (title.contains("Status", ignoreCase = true)) {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Payment Mode", color = TextGold, fontSize = 10.sp)

                                    Surface(
                                            color = Color.Black.copy(alpha = 0.2f),
                                            shape =
                                                    androidx.compose.foundation.shape
                                                            .RoundedCornerShape(4.dp),
                                            border =
                                                    androidx.compose.foundation.BorderStroke(
                                                            0.5.dp,
                                                            BorderGold.copy(alpha = 0.5f)
                                                    )
                                    ) {
                                        Row(
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                        ),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                    com.khanabook.lite.pos.domain.model.PaymentMode
                                                            .fromDbValue(
                                                                    currentResult.bill.paymentMode
                                                            )
                                                            .displayLabel,
                                                    color = TextLight,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily ID", color = TextGold, fontSize = 10.sp)
                                    Text(
                                            currentResult.bill.dailyOrderDisplay,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                        onClick = {
                                            result?.let { shareBillAsPdf(context, it, profile) }
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = SuccessGreen
                                                ),
                                        shape =
                                                androidx.compose.foundation.shape
                                                        .RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                            Icons.Default.Share,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share", color = Color.White, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                        onClick = {
                                            result?.let {
                                                directPrint(context, it, profile, billingViewModel.printerManager)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape =
                                                androidx.compose.foundation.shape
                                                        .RoundedCornerShape(8.dp),
                                        colors =
                                                ButtonDefaults.outlinedButtonColors(
                                                        contentColor = PrimaryGold
                                                ),
                                        border =
                                                androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        PrimaryGold
                                                )
                                ) {
                                    Icon(
                                            Icons.Default.Print,
                                            null,
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Print", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = TextGold.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                "Search for an order to view details",
                                color = TextGold.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
