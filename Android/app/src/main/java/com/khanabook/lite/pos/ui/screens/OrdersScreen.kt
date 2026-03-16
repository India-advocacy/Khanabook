@file:OptIn(ExperimentalMaterial3Api::class)

package com.khanabook.lite.pos.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.khanabook.lite.pos.domain.model.OrderDetailRow
import com.khanabook.lite.pos.domain.model.OrderStatus
import com.khanabook.lite.pos.domain.model.PaymentMode
import com.khanabook.lite.pos.ui.theme.*
import com.khanabook.lite.pos.ui.viewmodel.ReportsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
    settingsViewModel: com.khanabook.lite.pos.ui.viewmodel.SettingsViewModel = hiltViewModel()
) {
    val allRows by viewModel.orderDetailsTable.collectAsState()
    val profile by settingsViewModel.profile.collectAsState()
    val enabledModes = remember(profile) { profile?.let { com.khanabook.lite.pos.domain.manager.PaymentModeManager.getEnabledModes(it) } ?: listOf(PaymentMode.CASH) }
    var selectedPeriod by remember { mutableIntStateOf(0) }
    
    // Date Range Picker State
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    LaunchedEffect(selectedPeriod) {
        if (selectedPeriod != 3) {
            val (from, to) = periodRange(selectedPeriod)
            viewModel.loadReports(from, to)
        }
    }

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val fromStr = sdf.format(Date(start))
                        val toStr = sdf.format(Date(end))
                        viewModel.setCustomDateRange(fromStr, toStr)
                    }
                    showDateRangePicker = false
                }) {
                    Text("OK", color = PrimaryGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel", color = PrimaryGold)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = DarkBrown2)
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkBrown2,
                    titleContentColor = PrimaryGold,
                    headlineContentColor = PrimaryGold,
                    weekdayContentColor = TextGold,
                    dayContentColor = TextLight,
                    selectedDayContainerColor = PrimaryGold,
                    selectedDayContentColor = DarkBrown1,
                    todayContentColor = PrimaryGold
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBrown1, DarkBrown2)
                )
            )
    ) {
        // Main Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryGold)
                }
                Text(
                    text = "Order Details",
                    modifier = Modifier.weight(1f),
                    color = PrimaryGold,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                // Empty spacer to balance the back button
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Custom Tabs
            PeriodTabs(
                selectedTabIndex = selectedPeriod,
                onTabSelected = { 
                    selectedPeriod = it 
                    if (it == 3) showDateRangePicker = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Table Header
            TableHeader()

            // Table Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(allRows) { row ->
                    OrderTableRow(
                        row = row,
                        enabledModes = enabledModes,
                        onStatusChange = { newStatus ->
                            viewModel.updateOrderStatus(row.billId, newStatus)
                        },
                        onPayModeChange = { newMode ->
                            viewModel.updatePaymentMode(row.billId, newMode.dbValue)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Daily", "Weekly", "Monthly", "Custom")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            OrderFilterChip(
                label = title,
                isSelected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OrderFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) PrimaryGold else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, BorderGold),
        contentColor = if (isSelected) DarkBrown1 else TextLight
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("D.No", 1f)
        HeaderCell("L.No", 1.2f)
        HeaderCell("Current\nStatus", 1.5f)
        HeaderCell("Amount", 1.3f)
        HeaderCell("Mode", 1.2f)
        HeaderCell("Status", 1.5f)
        HeaderCell("Date", 1.5f)
    }
}

@Composable
fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = TextGold,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        lineHeight = 12.sp
    )
}

@Composable
fun OrderTableRow(
    row: OrderDetailRow, 
    enabledModes: List<PaymentMode>,
    onStatusChange: (String) -> Unit,
    onPayModeChange: (PaymentMode) -> Unit
) {
    var statusExpanded by remember { mutableStateOf(false) }
    var payModeExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(ParchmentBG)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(row.dailyNo, 1f)
        TableCell(row.lifetimeNo.toString(), 1.2f)
        TableCell(row.currentStatus, 1.5f, fontSize = 10.sp)
        TableCell("\u20b9${String.format("%.2f", row.salesAmount)}", 1.3f, fontWeight = FontWeight.Bold)
        
        // Pay Mode Dropdown (Clickable Badge)
        Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
            val color = getPayModeColor(row.payMode)
            Surface(
                onClick = { payModeExpanded = true },
                color = color,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.payMode.displayLabel,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            DropdownMenu(
                expanded = payModeExpanded,
                onDismissRequest = { payModeExpanded = false },
                modifier = Modifier.background(ParchmentBG)
            ) {
                enabledModes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.displayLabel, color = DarkBrown1, fontSize = 12.sp) },
                        onClick = {
                            onPayModeChange(mode)
                            payModeExpanded = false
                        }
                    )
                }
            }
        }

        // Order Status Dropdown
        Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
            val statusColor = if (row.orderStatus == OrderStatus.COMPLETED) SuccessGreen else DangerRed
            Surface(
                onClick = { statusExpanded = true },
                color = statusColor,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (row.orderStatus == OrderStatus.COMPLETED) "Completion" else "Cancelled",
                        color = Color.White,
                        fontSize =8.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                }
            }

            DropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false },
                modifier = Modifier.background(ParchmentBG)
            ) {
                DropdownMenuItem(
                    text = { Text("Completion", color = DarkBrown1, fontSize = 12.sp) },
                    onClick = {
                        onStatusChange(OrderStatus.COMPLETED.dbValue)
                        statusExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Cancelled", color = DarkBrown1, fontSize = 12.sp) },
                    onClick = {
                        onStatusChange(OrderStatus.CANCELLED.dbValue)
                        statusExpanded = false
                    }
                )
            }
        }

        TableCell(com.khanabook.lite.pos.domain.util.DateUtils.formatDisplayDate(row.salesDate), 1.5f, fontSize = 9.sp)
    }
}

@Composable
fun RowScope.TableCell(
    text: String, 
    weight: Float, 
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = Color.Black,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        lineHeight = 12.sp
    )
}

private fun getPayModeColor(mode: PaymentMode): Color {
    return when (mode) {
        PaymentMode.CASH -> SuccessGreen
        PaymentMode.UPI -> Color(0xFF5D4037) // Brownish as in UI
        PaymentMode.POS -> Color(0xFF673AB7) // Purple
        PaymentMode.ZOMATO -> VegGreen 
        PaymentMode.SWIGGY -> Color(0xFFE65100) // Deep Orange
        else -> Color(0xFF455A64)
    }
}

// Remote formatDisplayDate in favor of Utils.formatDisplayDate


private fun periodRange(tab: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    
    val start: Calendar = when (tab) {
        0 -> { // Daily
            (cal.clone() as Calendar).apply { 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0) 
                set(Calendar.MILLISECOND, 0)
            }
        }
        1 -> { // Weekly
            (cal.clone() as Calendar).apply { 
                add(Calendar.DAY_OF_YEAR, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0) 
                set(Calendar.MILLISECOND, 0)
            }
        }
        2 -> { // Monthly
            (cal.clone() as Calendar).apply { 
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0) 
                set(Calendar.MILLISECOND, 0)
            }
        }
        else -> cal
    }
    
    val end: Calendar = Calendar.getInstance().apply { 
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59) 
        set(Calendar.MILLISECOND, 999)
    }
    
    return start.timeInMillis to end.timeInMillis
}


