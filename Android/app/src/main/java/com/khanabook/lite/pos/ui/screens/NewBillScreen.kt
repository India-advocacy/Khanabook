@file:OptIn(ExperimentalMaterial3Api::class)

package com.khanabook.lite.pos.ui.screens

import android.graphics.BitmapFactory
import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khanabook.lite.pos.data.local.entity.ItemVariantEntity
import com.khanabook.lite.pos.domain.manager.BillCalculator
import com.khanabook.lite.pos.domain.manager.PaymentModeManager
import com.khanabook.lite.pos.domain.model.*
import com.khanabook.lite.pos.domain.util.*
import com.khanabook.lite.pos.ui.components.ParchmentTextField
import com.khanabook.lite.pos.ui.theme.*
import com.khanabook.lite.pos.ui.viewmodel.BillingViewModel
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel
import com.khanabook.lite.pos.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun NewBillScreen(
        onBack: () -> Unit,
        modifier: Modifier = Modifier,
        billingViewModel: BillingViewModel = hiltViewModel(),
        menuViewModel: MenuViewModel = hiltViewModel(),
        navController: androidx.navigation.NavController? = null
) {
    var step by remember { mutableIntStateOf(1) }
    val cartItems by billingViewModel.cartItems.collectAsStateWithLifecycle()
    val summary by billingViewModel.billSummary.collectAsStateWithLifecycle()
    val error by billingViewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            billingViewModel.clearError()
        }
    }

    // Success Toast for Order Placed
    LaunchedEffect(step) {
        if (step == 4) {
            android.widget.Toast.makeText(context, "Order Placed Successfully!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = DarkBrown1,
        topBar = {
            Column(modifier = Modifier.background(DarkBrown1)) {
                CenterAlignedTopAppBar(
                    title = { Text("New Bill", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { if (step == 1) onBack() else if (step == 2) step = 1 else if (step == 3) step = 2 else onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryGold)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBrown1)
                )
                
                if (step < 4) {
                    BillStepper(currentStep = step)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues).background(DarkBrown1)) {
            when (step) {
                1 ->
                        CustomerInfoStep(
                                onNext = { name, whatsapp ->
                                    billingViewModel.setCustomerInfo(name, whatsapp)
                                    step = 2
                                },
                                onBack = onBack,
                                hideHeader = true
                        )
                2 ->
                        MenuSelectionStep(
                                billingViewModel,
                                menuViewModel,
                                onBack = { step = 1 },
                                onProceedToPayment = { step = 3 },
                                total = summary.total,
                                itemCount = cartItems.sumOf { it.quantity },
                                hideHeader = true,
                                navController = navController
                        )
                3 ->
                        PaymentStep(
                                billingViewModel,
                                onBackToMenu = { step = 2 },
                                onComplete = { step = 4 }
                        )
                4 ->
                        SuccessStep(
                                billingViewModel,
                                settingsViewModel = hiltViewModel(),
                                onDone = onBack
                        )
            }
        }
    }
}

@Composable
fun CustomerInfoStep(onNext: (String, String) -> Unit, onBack: () -> Unit, hideHeader: Boolean = false) {
    var name by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }

    // Phone number is mandatory and must be a valid 10-digit number.
    val isWhatsappValid = whatsapp.isNotEmpty() && isValidPhone(whatsapp)
    val isNextEnabled = isWhatsappValid

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
    ) {
        if (!hideHeader) {
            // Back arrow + title aligned in the same row
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryGold
                    )
                }
                Column {
                    Text(
                            "New Bill",
                            color = PrimaryGold,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                    )
                    Text("Customer Details", color = TextGold, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        val showPhoneError = whatsapp.isNotEmpty() && !isValidPhone(whatsapp)
        OutlinedTextField(
                value = whatsapp,
                onValueChange = { if (it.length <= 10) whatsapp = it },
                label = { Text("Customer WhatsApp Number *") },
                modifier = Modifier.fillMaxWidth(),
                colors = menuTextFieldColors(),
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = VegGreen) },
                isError = showPhoneError,
                supportingText = {
                    if (showPhoneError) Text("Enter a valid 10-digit number", color = DangerRed)
                },
                keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = menuTextFieldColors(),
                leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryGold) }
        )

        Spacer(modifier = Modifier.height(48.dp))
        Button(
                onClick = { if (isNextEnabled) onNext(name, whatsapp) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = if (isNextEnabled) PrimaryGold else Color.Gray
                        ),
                shape = RoundedCornerShape(12.dp),
                enabled = isNextEnabled
        ) {
            Text(
                    "Continue",
                    color = if (isNextEnabled) DarkBrown1 else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
            )
        }
    }
}

@Composable
fun OrderTypeButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
            modifier =
                    modifier.height(40.dp)
                            .background(
                                    if (isSelected) PrimaryGold else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                            )
                            .clickable { onClick() },
            contentAlignment = Alignment.Center
    ) {
        Text(
                text,
                color = if (isSelected) DarkBrown1 else TextGold,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun MenuSelectionStep(
        billingViewModel: BillingViewModel,
        menuViewModel: MenuViewModel,
        onBack: () -> Unit,
        onProceedToPayment: () -> Unit,
        total: Double,
        itemCount: Int,
        hideHeader: Boolean = false,
        navController: NavController? = null
) {
    val categories by menuViewModel.categories.collectAsStateWithLifecycle()
    val items by menuViewModel.menuItems.collectAsStateWithLifecycle()
    val cartItems by billingViewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCategoryId by menuViewModel.selectedCategoryId.collectAsStateWithLifecycle()

    // Observe barcode scan result returned from OcrScannerScreen via StateFlow
    val scannedBarcode by (navController
        ?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("scanned_barcode", "")
        ?: kotlinx.coroutines.flow.MutableStateFlow(""))
        .collectAsStateWithLifecycle()

    LaunchedEffect(scannedBarcode) {
        if (scannedBarcode.isNotEmpty()) {
            billingViewModel.handleScannedBarcode(scannedBarcode)
            navController?.currentBackStackEntry?.savedStateHandle?.set("scanned_barcode", "")
        }
    }

    val derivedItemCount by remember {
        derivedStateOf { cartItems.sumOf { it.quantity } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hideHeader) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        null,
                        tint = PrimaryGold,
                        modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("New Bill", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (categories.isNotEmpty()) {
            val selectedIndex =
                    categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0)
            ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = PrimaryGold,
                    contentColor = DarkBrown1,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                    modifier =
                                            Modifier.tabIndicatorOffset(
                                                    tabPositions[selectedIndex]
                                            ),
                                    color = DarkBrown1
                            )
                        }
                    }
            ) {
                categories.forEach { category ->
                    Tab(
                            selected = category.id == selectedCategoryId,
                            onClick = { menuViewModel.selectCategory(category.id) },
                            text = {
                                Text(
                                        category.name,
                                        fontSize = 13.sp,
                                        fontWeight =
                                                if (category.id == selectedCategoryId)
                                                        FontWeight.Bold
                                                else FontWeight.Medium
                                )
                            },
                            selectedContentColor = DarkBrown1,
                            unselectedContentColor = DarkBrown1.copy(alpha = 0.7f)
                    )
                }
            }
        }

        LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.menuItem.id }) { menuWithVariants ->
                val item = menuWithVariants.menuItem
                val variants = menuWithVariants.variants
                var showVariantPicker by remember { mutableStateOf(false) }

                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBrown2),
                        shape = RoundedCornerShape(12.dp)
                ) {
                    if (variants.isEmpty()) {
                        // â”€â”€ No variants: simple add/remove row â”€â”€
                        val cartItem =
                                cartItems.find { it.item.id == item.id && it.variant == null }
                        Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            FoodTypeIcon(item.foodType)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        item.name,
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                )
                                Text("₹${item.basePrice}", color = TextGold, fontSize = 12.sp)
                            }
                            QuantitySelector(
                                    quantity = cartItem?.quantity ?: 0,
                                    onAdd = { billingViewModel.addToCart(item) },
                                    onRemove = { billingViewModel.removeFromCart(item) }
                            )
                        }
                    } else {
                        // â”€â”€ Has variants: show header + per-variant rows â”€â”€
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FoodTypeIcon(item.foodType)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                            item.name,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                    )
                                    Text(
                                            "${variants.size} Variants",
                                            color = PrimaryGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderGold.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            variants.forEach { variant ->
                                val variantCartItem =
                                        cartItems.find {
                                            it.item.id == item.id && it.variant?.id == variant.id
                                        }
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                            variant.variantName,
                                            color = TextGold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                            "₹${"%.0f".format(variant.price)}",
                                            color = PrimaryGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(end = 12.dp)
                                    )
                                    QuantitySelector(
                                            quantity = variantCartItem?.quantity ?: 0,
                                            onAdd = { billingViewModel.addToCart(item, variant) },
                                            onRemove = {
                                                billingViewModel.removeFromCart(item, variant)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showVariantPicker) {
                    VariantPickerDialog(
                            itemName = item.name,
                            variants = variants,
                            onDismiss = { showVariantPicker = false },
                            onSelect = { variant ->
                                billingViewModel.addToCart(item, variant)
                                showVariantPicker = false
                            }
                    )
                }
            }
        }

        // Bottom bar: Scan barcode + Proceed
        Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            "$derivedItemCount Items Added",
                            color = DarkBrown1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                    )
                    Text(
                            "₹${"%.2f".format(total)}",
                            color = DarkBrown1,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Barcode Scan Button
                    if (navController != null) {
                        OutlinedButton(
                                onClick = { navController.navigate("ocr_scanner/billing") },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, DarkBrown1),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkBrown1),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Scan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Button(
                            onClick = onProceedToPayment,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBrown1),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            enabled = derivedItemCount > 0
                    ) {
                        Text(
                                "Proceed",
                                color = PrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuantitySelector(quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    if (quantity == 0) {
        OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGold),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold),
                contentPadding = PaddingValues(horizontal = 12.dp)
        ) { Text("Add", fontSize = 12.sp) }
    } else {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(PrimaryGold, RoundedCornerShape(4.dp)).height(32.dp)
        ) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, null, tint = DarkBrown1, modifier = Modifier.size(16.dp))
            }
            Text("$quantity", color = DarkBrown1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, null, tint = DarkBrown1, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PaymentStep(viewModel: BillingViewModel, onBackToMenu: () -> Unit, onComplete: () -> Unit) {
    val summary by viewModel.billSummary.collectAsState()
    val settingsVM: SettingsViewModel = hiltViewModel()
    val profile by settingsVM.profile.collectAsState()
    val enabledModes =
            remember(profile) {
                profile?.let { PaymentModeManager.getEnabledModes(it) } ?: listOf(PaymentMode.CASH)
            }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }
    var expanded by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }

    // Split amounts
    var p1Text by remember { mutableStateOf("") }
    var p2Text by remember { mutableStateOf("") }

    // Set default mode and initialize amounts
    LaunchedEffect(enabledModes) {
        if (enabledModes.isNotEmpty()) {
            selectedMode =
                    if (enabledModes.contains(PaymentMode.UPI)) PaymentMode.UPI
                    else enabledModes.first()
        }
    }

    val isUpiMode =
            selectedMode == PaymentMode.UPI ||
                    selectedMode == PaymentMode.PART_CASH_UPI ||
                    selectedMode == PaymentMode.PART_UPI_POS

    val isSplitMode =
            selectedMode == PaymentMode.PART_CASH_UPI ||
                    selectedMode == PaymentMode.PART_CASH_POS ||
                    selectedMode == PaymentMode.PART_UPI_POS

    LaunchedEffect(selectedMode, summary.total) {
        if (isSplitMode) {
            val half = summary.total / 2.0
            p1Text = "%.2f".format(half)
            p2Text = "%.2f".format(summary.total - half)
        }
    }

    val p1 = p1Text.toDoubleOrNull() ?: 0.0
    val p2 = p2Text.toDoubleOrNull() ?: 0.0
    val isAmountValid =
            if (isSplitMode) {
                BillCalculator.validatePartPayment(p1, p2, summary.total)
            } else true

    Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isUpiMode) {
            Text("Scan to Pay", color = PrimaryGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Box - Clickable
            Box(
                    modifier =
                            Modifier.size(200.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(2.dp, PrimaryGold, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .clickable { showQrModal = true },
                    contentAlignment = Alignment.Center
            ) {
                if (!profile?.upiQrPath.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.upiQrPath,
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.QrCode,
                        null,
                        modifier = Modifier.size(100.dp),
                        tint = Color.LightGray
                    )
                }
            }
            Text(
                    "Tap to Enlarge",
                    color = PrimaryGold.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                    Icons.Default.Payment,
                    null,
                    tint = PrimaryGold.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    "Complete Payment",
                    color = TextLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        // Bill Summary in Payment Step
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBrown2),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGold.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Payable Amount", color = TextGold, fontSize = 14.sp)
                    Text(
                            "₹${"%.2f".format(summary.total)}",
                            color = PrimaryGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
                "Select Payment Mode:",
                color = TextLight,
                modifier = Modifier.align(Alignment.Start),
                fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(56.dp)
                                .background(DarkBrown2, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderGold)
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedMode.displayLabel, color = PrimaryGold)
                Icon(Icons.Default.ArrowDropDown, null, tint = PrimaryGold)
            }
            DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DarkBrown2)
            ) {
                enabledModes.forEach { mode ->
                    DropdownMenuItem(
                            text = { Text(mode.displayLabel, color = TextLight) },
                            onClick = {
                                selectedMode = mode
                                expanded = false
                            }
                    )
                }
            }
        }

        if (isSplitMode) {
            Spacer(modifier = Modifier.height(24.dp))
            val labels =
                    when (selectedMode) {
                        PaymentMode.PART_CASH_UPI -> "Cash Amount" to "UPI Amount"
                        PaymentMode.PART_CASH_POS -> "Cash Amount" to "POS Amount"
                        PaymentMode.PART_UPI_POS -> "UPI Amount" to "POS Amount"
                        else -> "" to ""
                    }

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ParchmentTextField(
                            value = p1Text,
                            onValueChange = { p1Text = it },
                            label = labels.first,
                            isError = !isAmountValid,
                            keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType =
                                                    androidx.compose.ui.text.input.KeyboardType
                                                            .Decimal
                                    )
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ParchmentTextField(
                            value = p2Text,
                            onValueChange = { p2Text = it },
                            label = labels.second,
                            isError = !isAmountValid,
                            keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType =
                                                    androidx.compose.ui.text.input.KeyboardType
                                                            .Decimal
                                    )
                    )
                }
            }

            if (!isAmountValid) {
                Text(
                        "Sum must equal ₹${"%.2f".format(summary.total)} (Current: ₹${"%.2f".format(p1 + p2)})",
                        color = DangerRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                )
            }
        }

        val scope = rememberCoroutineScope()

        Spacer(modifier = Modifier.height(32.dp))
        Button(
                onClick = {
                    if (isAmountValid) {
                        scope.launch {
                            viewModel.setPaymentMode(selectedMode, p1, p2)
                            if (viewModel.completeOrder(PaymentStatus.SUCCESS)) {
                                onComplete()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = if (isAmountValid) SuccessGreen else Color.Gray
                        ),
                shape = RoundedCornerShape(12.dp),
                enabled = isAmountValid
        ) {
            Text(
                    "Payment Successful",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
                onClick = {
                    scope.launch {
                        viewModel.completeOrder(PaymentStatus.FAILED)
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Payment Failed / Cancelled", color = DangerRed, fontSize = 14.sp) }
    }

    if (showQrModal) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showQrModal = false }) {
            Card(
                    modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                "UPI Payment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                        )
                        IconButton(onClick = { showQrModal = false }) {
                            Icon(Icons.Default.Close, null, tint = Color.Black)
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(
                            color = Color.LightGray.copy(alpha = 0.5f)
                    )

                    Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                                modifier =
                                        Modifier.size(280.dp).background(Color.White).padding(8.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            if (!profile?.upiQrPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = profile?.upiQrPath,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.QrCode,
                                    null,
                                    modifier = Modifier.size(180.dp),
                                    tint = Color.LightGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                "₹${"%.2f".format(summary.total)}",
                                color = Color.Black,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                        )
                        Text(profile?.shopName ?: "", color = Color.Gray, fontSize = 14.sp)
                    }

                    Button(
                            onClick = { showQrModal = false },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBrown1)
                    ) { Text("CLOSE", color = PrimaryGold, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun SuccessStep(
        viewModel: BillingViewModel,
        settingsViewModel: SettingsViewModel,
        onDone: () -> Unit
) {
    val context = LocalContext.current
    val lastBill by viewModel.lastBill.collectAsState()
    val profile by settingsViewModel.profile.collectAsState()

    Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(100.dp))
        Text(
                "Payment Successful!",
                color = TextLight,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
        )

        val totalAmount = lastBill?.bill?.totalAmount ?: 0.0
        Text(
                "Payment of ₹${"%.2f".format(totalAmount)} received successfully.",
                color = TextGold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
                onClick = {
                    lastBill?.let { shareBillAsPdf(context, it, profile) }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = lastBill != null
        ) {
            Icon(Icons.Default.Share, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share PDF on WhatsApp", color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
                onClick = {
                    lastBill?.let { directPrint(context, it, profile, viewModel.printerManager) }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(12.dp),
                enabled = lastBill != null
        ) {
            Icon(Icons.Default.Print, null, tint = DarkBrown1)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Print Bill", color = DarkBrown1)
        }
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGold),
                shape = RoundedCornerShape(12.dp)
        ) { Text("Back", color = TextGold) }
    }
}

@Composable
fun VariantPickerDialog(
        itemName: String,
        variants: List<ItemVariantEntity>,
        onDismiss: () -> Unit,
        onSelect: (ItemVariantEntity) -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = DarkBrown2,
            title = {
                Column {
                    Text("Choose Variant", color = PrimaryGold, fontWeight = FontWeight.Bold)
                    Text(itemName, color = TextGold, fontSize = 13.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    variants.forEach { variant ->
                        Card(
                                modifier =
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                            onSelect(variant)
                                        },
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor = Color.Black.copy(alpha = 0.3f)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                        variant.variantName,
                                        color = TextLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                )
                                Text(
                                        "₹${"%.0f".format(variant.price)}",
                                        color = PrimaryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryGold) }
            }
    )
}

@Composable
fun FoodTypeIcon(type: String) {
    val color = if (type == "veg") VegGreen else NonVegRed
    Box(
            modifier = Modifier.size(14.dp).border(1.dp, color).padding(2.dp),
            contentAlignment = Alignment.Center
    ) { Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(100.dp))) }
}

@Composable
private fun menuTextFieldColors() =
        OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderGold.copy(alpha = 0.3f),
                focusedBorderColor = PrimaryGold,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight
        )

@Composable
fun BillStepper(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        StepItem(icon = Icons.Default.Person, label = "Customer", isActive = currentStep >= 1, isCompleted = currentStep > 1)
        StepConnector(isCompleted = currentStep > 1)
        StepItem(icon = Icons.AutoMirrored.Filled.List, label = "Menu", isActive = currentStep >= 2, isCompleted = currentStep > 2)
        StepConnector(isCompleted = currentStep > 2)
        StepItem(icon = Icons.Default.Payments, label = "Payment", isActive = currentStep >= 3, isCompleted = currentStep > 3)
    }
}

@Composable
fun StepItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, isCompleted: Boolean) {
    val color = if (isActive) PrimaryGold else Color.Gray
    val containerColor = if (isActive) PrimaryGold.copy(alpha = 0.1f) else Color.Transparent
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(containerColor, CircleShape)
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RowScope.StepConnector(isCompleted: Boolean) {
    val color = if (isCompleted) PrimaryGold else Color.Gray
    Box(
        modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .padding(horizontal = 4.dp)
            .padding(bottom = 12.dp)
            .background(color)
    )
}

