package com.khanabook.lite.pos.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khanabook.lite.pos.data.local.entity.*
import com.khanabook.lite.pos.data.local.relation.MenuWithVariants
import com.khanabook.lite.pos.domain.manager.BluetoothPrinterManager
import com.khanabook.lite.pos.domain.util.*
import com.khanabook.lite.pos.ui.theme.*
import com.khanabook.lite.pos.ui.viewmodel.AuthViewModel
import com.khanabook.lite.pos.ui.viewmodel.SettingsViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onScanClick: (String?) -> Unit = {},
    scannedText: String? = null,
    onScannedTextConsumed: () -> Unit = {},
    returnToMenuRoot: Boolean = false,
    onReturnToMenuRootConsumed: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf("menu") }

    LaunchedEffect(returnToMenuRoot) {
        if (returnToMenuRoot) {
            section = "menu"
            onReturnToMenuRootConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBrown1, DarkBrown2)))
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Header
            if (section != "menu_config") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (section == "menu") onBack() else section = "menu" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PrimaryGold)
                    }
                    Text(
                        text = when (section) {
                            "shop" -> "Shop Configuration"
                            "payment" -> "Payment Configuration"
                            "printer" -> "Printer Configuration"
                            "tax" -> "Tax Configuration"
                            "menu" -> "Settings"
                            else -> "Settings"
                        },
                        modifier = Modifier.weight(1f),
                        color = PrimaryGold,
                        fontSize = if (section == "menu") 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (section) {
                    "menu" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ProfileCard(currentUser)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            SettingsItem(icon = Icons.Filled.Store, text = "Shop/Restaurant Configuration") { section = "shop" }
                            SettingsItem(icon = Icons.AutoMirrored.Filled.ReceiptLong, text = "Menu Configuration") { section = "menu_config" }
                            SettingsItem(icon = Icons.Filled.CreditCard, text = "Payment Configuration") { section = "payment" }
                            SettingsItem(icon = Icons.Filled.Print, text = "Printer Configuration") { section = "printer" }
                            SettingsItem(icon = Icons.Filled.Settings, text = "Tax Configuration") { section = "tax" }
                            SettingsItem(icon = Icons.Default.Inventory2, text = "Inventory Configuration") { section = "inventory" }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Logout Card
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBG),
                                border = BorderStroke(1.dp, BorderGold.copy(alpha = 0.3f))
                            ) {
                                val ctx = LocalContext.current
                                val logoutViewModel: com.khanabook.lite.pos.ui.viewmodel.LogoutViewModel = hiltViewModel()
                                val logoutState by logoutViewModel.logoutState.collectAsStateWithLifecycle()

                                LaunchedEffect(logoutState) {
                                    if (logoutState is com.khanabook.lite.pos.ui.viewmodel.LogoutState.LoggedOut) {
                                        Toast.makeText(ctx, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                if (logoutState is com.khanabook.lite.pos.ui.viewmodel.LogoutState.WarningOfflineData) {
                                    val count = (logoutState as com.khanabook.lite.pos.ui.viewmodel.LogoutState.WarningOfflineData).count
                                    AlertDialog(
                                        onDismissRequest = { logoutViewModel.cancelLogout() },
                                        title = { Text("Offline Data Warning", color = Color.Red) },
                                        text = { Text("You have $count unsynced bills. Logging out will delete them. Proceed?") },
                                        confirmButton = {
                                            TextButton(onClick = { logoutViewModel.forceLogoutDespiteWarning() }) {
                                                Text("Logout Anyway", color = Color.Red, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { logoutViewModel.cancelLogout() }) { Text("Cancel") }
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Account Session", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Button(
                                        onClick = { logoutViewModel.initiateLogout() },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Logout", fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                    "shop" -> {
                        ShopConfigView(profile, viewModel, authViewModel) { section = "menu" }
                    }
                    "menu_config" -> {
                        MenuConfigurationScreen(
                            onBack = { section = "menu" },
                            onScanClick = onScanClick,
                            scannedText = scannedText,
                            onScannedTextConsumed = onScannedTextConsumed
                        )
                    }
                    "payment" -> {
                        PaymentConfigView(profile, onSave = { viewModel.saveProfile(it); section = "menu" }, onBack = { section = "menu" })
                    }
                    "printer" -> {
                        PrinterConfigView(profile, onSave = { viewModel.saveProfile(it); section = "menu" }, onBack = { section = "menu" }, viewModel = viewModel)
                    }
                    "tax" -> {
                        TaxConfigView(profile, onSave = { viewModel.saveProfile(it); section = "menu" }, onBack = { section = "menu" })
                    }
                    "inventory" -> {
                        InventoryConfigView(viewModel = viewModel, onBack = { section = "menu" })
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(user: UserEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBrown2.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(PrimaryGold, CircleShape), contentAlignment = Alignment.Center) {
                Text(text = user?.name?.take(1)?.uppercase() ?: "?", color = DarkBrown1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user?.name ?: "Guest User", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = user?.whatsappNumber ?: "", color = TextGold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBG),
        border = BorderStroke(1.dp, BorderGold.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text, color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = PrimaryGold)
        }
    }
}

@Composable
fun ConfigCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBG),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderGold.copy(alpha = 0.3f))
    ) { Column(modifier = Modifier.padding(20.dp)) { content() } }
}

@Composable
private fun ShopConfigView(profile: RestaurantProfileEntity?, viewModel: SettingsViewModel, authViewModel: AuthViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile?.shopName ?: "") }
    var address by remember { mutableStateOf(profile?.shopAddress ?: "") }
    var whatsapp by remember { mutableStateOf(profile?.whatsappNumber ?: "") }
    var email by remember { mutableStateOf(profile?.email ?: "") }
    var logoPath by remember { mutableStateOf(profile?.logoPath) }
    var consent by remember { mutableStateOf(profile?.emailInvoiceConsent ?: false) }

    // --- OTP States ---
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpValue by remember { mutableStateOf("") }
    val otpStatus by authViewModel.otpVerificationStatus.collectAsState()

    LaunchedEffect(otpStatus) {
        when (otpStatus) {
            is AuthViewModel.OtpVerificationResult.OtpSent -> {
                showOtpDialog = true
            }
            is AuthViewModel.OtpVerificationResult.Error -> {
                Toast.makeText(context, (otpStatus as AuthViewModel.OtpVerificationResult.Error).message, Toast.LENGTH_SHORT).show()
                authViewModel.clearOtpStatus()
            }
            else -> {}
        }
    }

    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { 
                showOtpDialog = false
                authViewModel.clearOtpStatus()
                otpValue = ""
            },
            title = { Text("Verify WhatsApp", color = PrimaryGold) },
            text = {
                Column {
                    Text("OTP sent to $whatsapp via WhatsApp", color = TextGold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    ParchmentTextField(
                        value = otpValue,
                        onValueChange = { if (it.length <= 6) otpValue = it },
                        label = "6-Digit OTP"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (authViewModel.verifyOtp(otpValue)) {
                            profile?.copy(
                                shopName = name,
                                shopAddress = address,
                                whatsappNumber = whatsapp,
                                email = email,
                                logoPath = logoPath,
                                emailInvoiceConsent = consent
                            )?.let { 
                                viewModel.saveProfile(it) 
                            }
                            showOtpDialog = false
                            authViewModel.clearOtpStatus()
                            otpValue = ""
                            onBack()
                        } else {
                            Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Verify & Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showOtpDialog = false
                    authViewModel.clearOtpStatus()
                    otpValue = ""
                }) {
                    Text("Cancel", color = DangerRed)
                }
            },
            containerColor = DarkBrown1
        )
    }

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { logoPath = copyUriToInternalStorage(context, it, "shop_logo.png") }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ConfigCard {
            Text("Shop Profile", color = PrimaryGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(100.dp).background(Color.White).border(1.dp, Color.LightGray), contentAlignment = Alignment.Center) {
                    logoPath?.let { loadBitmap(it)?.let { bmp -> Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp)) } } ?: Icon(Icons.Default.Storefront, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                }
                OutlinedButton(onClick = { logoLauncher.launch("image/*") }, border = BorderStroke(1.dp, PrimaryGold), shape = RoundedCornerShape(20.dp)) { Text("Change Logo", color = PrimaryGold) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            ParchmentTextField(value = name, onValueChange = { name = it }, label = "Shop Name")
            Spacer(modifier = Modifier.height(12.dp))
            ParchmentTextField(value = address, onValueChange = { address = it }, label = "Shop Address")
            Spacer(modifier = Modifier.height(12.dp))
            ParchmentTextField(value = whatsapp, onValueChange = { if (it.length <= 10) whatsapp = it }, label = "Whatsapp Number")
            Spacer(modifier = Modifier.height(12.dp))
            ParchmentTextField(value = email, onValueChange = { email = it }, label = "Email")
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = { consent = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryGold))
                Text("Receive invoice copies on Email", color = TextGold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    if (whatsapp != (profile?.whatsappNumber ?: "")) {
                        if (isValidPhone(whatsapp)) {
                            authViewModel.sendOtp(whatsapp, "update_whatsapp")
                        } else {
                            Toast.makeText(context, "Invalid WhatsApp Number", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        profile?.copy(shopName = name, shopAddress = address, whatsappNumber = whatsapp, email = email, logoPath = logoPath, emailInvoiceConsent = consent)?.let { 
                            viewModel.saveProfile(it) 
                        }
                        onBack() 
                    }
                }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(24.dp)) { Text("Save", color = Color.White) }
                OutlinedButton(onClick = { viewModel.resetDailyCounter(); Toast.makeText(context, "Daily order counter reset", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, DangerRed), colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed), shape = RoundedCornerShape(24.dp)) { Text("Reset Counter", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun PaymentConfigView(profile: RestaurantProfileEntity?, onSave: (RestaurantProfileEntity) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var currency by remember { mutableStateOf(profile?.currency ?: "INR") }
    var upiSupported by remember { mutableStateOf(profile?.upiEnabled ?: false) }
    var upiHandle by remember { mutableStateOf(profile?.upiHandle ?: "") }
    var upiMobile by remember { mutableStateOf(profile?.upiMobile ?: "") }
    var qrPath by remember { mutableStateOf(profile?.upiQrPath) }
    var cashEnabled by remember { mutableStateOf(profile?.cashEnabled ?: true) }
    var posEnabled by remember { mutableStateOf(profile?.posEnabled ?: false) }
    var zomatoEnabled by remember { mutableStateOf(profile?.zomatoEnabled ?: false) }
    var swiggyEnabled by remember { mutableStateOf(profile?.swiggyEnabled ?: false) }
    var ownWebsiteEnabled by remember { mutableStateOf(profile?.ownWebsiteEnabled ?: false) }

    val qrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { qrPath = copyUriToInternalStorage(context, it, "upi_qr.png") }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ConfigCard {
            ParchmentTextField(value = currency, onValueChange = { currency = it }, label = "Currency *")
            Spacer(modifier = Modifier.height(24.dp))
            Text("Enable Payment Methods", color = PrimaryGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            PaymentToggle("Cash Payment", cashEnabled) { cashEnabled = it }
            PaymentToggle("POS Machine", posEnabled) { posEnabled = it }
            PaymentToggle("Zomato Orders", zomatoEnabled) { zomatoEnabled = it }
            PaymentToggle("Swiggy Orders", swiggyEnabled) { swiggyEnabled = it }
            PaymentToggle("Own Website", ownWebsiteEnabled) { ownWebsiteEnabled = it }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = upiSupported, onCheckedChange = { upiSupported = it }, colors = CheckboxDefaults.colors(checkedColor = SuccessGreen))
                Text("Enable UPI QR Payments", color = TextGold, fontSize = 14.sp)
            }
            if (upiSupported) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(100.dp).background(Color.White).border(1.dp, Color.LightGray).padding(4.dp), contentAlignment = Alignment.Center) {
                        qrPath?.let { loadBitmap(it)?.let { bmp -> Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) } } ?: Icon(Icons.Default.QrCode, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    }
                    OutlinedButton(onClick = { qrLauncher.launch("image/*") }, border = BorderStroke(1.dp, PrimaryGold), shape = RoundedCornerShape(20.dp)) { Text("Upload QR Code", color = PrimaryGold) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                ParchmentTextField(value = upiHandle, onValueChange = { upiHandle = it }, label = "UPI Handle")
                Spacer(modifier = Modifier.height(12.dp))
                ParchmentTextField(value = upiMobile, onValueChange = { upiMobile = it }, label = "UPI Mobile Number")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { profile?.copy(currency = currency, upiEnabled = upiSupported, upiHandle = upiHandle, upiMobile = upiMobile, upiQrPath = qrPath, cashEnabled = cashEnabled, posEnabled = posEnabled, zomatoEnabled = zomatoEnabled, swiggyEnabled = swiggyEnabled, ownWebsiteEnabled = ownWebsiteEnabled)?.let { onSave(it) } }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(24.dp)) { Text("Save", color = Color.White) }
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, TextGold), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGold)) { Text("Back") }
            }
        }
    }
}

@Composable
fun PaymentToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextGold, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterConfigView(profile: RestaurantProfileEntity?, onSave: (RestaurantProfileEntity) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel) {
    var enabled by remember { mutableStateOf(profile?.printerEnabled ?: false) }
    var paper58 by remember { mutableStateOf((profile?.paperSize ?: "58mm") == "58mm") }
    var autoPrint by remember { mutableStateOf(profile?.autoPrintOnSuccess ?: false) }
    var includeLogo by remember { mutableStateOf(profile?.includeLogoInPrint ?: true) }
    var printWhatsapp by remember { mutableStateOf(profile?.printCustomerWhatsapp ?: true) }
    val context = LocalContext.current
    var isBtActive by remember { mutableStateOf(viewModel.isBluetoothEnabled(context)) }
    
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    isBtActive = (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON)
                }
            }
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { try { context.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    val btDevices by viewModel.btDevices.collectAsStateWithLifecycle()
    val btIsScanning by viewModel.btIsScanning.collectAsStateWithLifecycle()
    val btIsConnecting by viewModel.btIsConnecting.collectAsStateWithLifecycle()
    val btConnectResult by viewModel.btConnectResult.collectAsStateWithLifecycle()
    var showBtSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bluetoothLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            isBtActive = true
            if (enabled) {
                viewModel.startBluetoothScan(context)
                showBtSheet = true
            }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_CONNECT] == true && perms[Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            perms[Manifest.permission.BLUETOOTH] == true && perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }
        if (ok) {
            if (!viewModel.isBluetoothEnabled(context)) {
                bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                viewModel.startBluetoothScan(context)
                showBtSheet = true
            }
        } else {
            Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
            enabled = false
        }
    }

    LaunchedEffect(btConnectResult) {
        btConnectResult?.let { Toast.makeText(context, if (it) "Printer Connected!" else "Connection Failed", Toast.LENGTH_SHORT).show(); if (it) showBtSheet = false; viewModel.clearBtConnectResult() }
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ConfigCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Bluetooth Printer", color = TextGold, fontWeight = FontWeight.Medium)
                Switch(
                    checked = enabled,
                    onCheckedChange = { 
                        enabled = it
                        if (it) {
                            if (!viewModel.hasBluetoothPermissions(context)) {
                                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                                } else {
                                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                                permissionLauncher.launch(perms)
                            } else if (!viewModel.isBluetoothEnabled(context)) {
                                bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            } else {
                                viewModel.startBluetoothScan(context)
                                showBtSheet = true 
                            }
                        }
                    }, 
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
                )
            }
            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().border(1.dp, BorderGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(12.dp)) {
                    Column {
                        Text("Connected: ${profile?.printerName ?: "None"}", color = TextLight, fontWeight = FontWeight.Bold)
                        Text("MAC: ${profile?.printerMac ?: "---"}", color = TextGold, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { 
                        if (!viewModel.hasBluetoothPermissions(context)) {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                            } else {
                                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            permissionLauncher.launch(perms)
                        } else if (!viewModel.isBluetoothEnabled(context)) {
                            bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        } else {
                            viewModel.startBluetoothScan(context)
                            showBtSheet = true 
                        }
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))) {
                        Text("Scan", color = Color.White)
                    }
                    Button(onClick = { viewModel.testPrint() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)) {
                        Text("Test Print", color = DarkBrown1)
                    }
                }
            }
        }
        ConfigCard {
            Text("Paper Size", color = PrimaryGold, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = paper58, onClick = { paper58 = true }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold))
                Text("58mm", color = TextGold)
                Spacer(modifier = Modifier.width(32.dp))
                RadioButton(selected = !paper58, onClick = { paper58 = false }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold))
                Text("80mm", color = TextGold)
            }
        }
        ConfigCard {
            Text("Print Options", color = PrimaryGold, fontWeight = FontWeight.Bold)
            PrinterOptionRow("Auto Print Success", autoPrint) { autoPrint = it }
            PrinterOptionRow("Include Logo", includeLogo) { includeLogo = it }
            PrinterOptionRow("Print WhatsApp", printWhatsapp) { printWhatsapp = it }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { profile?.copy(printerEnabled = enabled, paperSize = if (paper58) "58mm" else "80mm", autoPrintOnSuccess = autoPrint, includeLogoInPrint = includeLogo, printCustomerWhatsapp = printWhatsapp)?.let { onSave(it) } }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("SAVE SETTINGS", color = Color.White) }
    }

    if (showBtSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.stopBluetoothScan(); showBtSheet = false }, sheetState = sheetState, containerColor = Color(0xFF1C1008)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp)) {
                Text("Select Printer", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (btIsScanning) CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(btDevices, key = { it.address }) { device ->
                        DeviceRow(device, isConnecting = btIsConnecting) { viewModel.connectToPrinter(context, device) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterOptionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = PrimaryGold))
        Text(label, color = TextGold, fontSize = 14.sp)
    }
}

@Composable
fun DeviceRow(device: BluetoothDevice, isConnecting: Boolean, onClick: () -> Unit) {
    @Suppress("MissingPermission")
    val name = device.name ?: "Unknown"
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !isConnecting) { onClick() }, colors = CardDefaults.cardColors(containerColor = DarkBrown1.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bluetooth, null, tint = PrimaryGold)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = TextLight, fontWeight = FontWeight.Medium)
                Text(device.address, color = TextGold, fontSize = 11.sp)
            }
            if (isConnecting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryGold)
        }
    }
}

@Composable
private fun TaxConfigView(profile: RestaurantProfileEntity?, onSave: (RestaurantProfileEntity) -> Unit, onBack: () -> Unit) {
    var country by remember { mutableStateOf(profile?.country ?: "India") }
    var gstEnabled by remember { mutableStateOf(profile?.gstEnabled ?: false) }
    var gstNumber by remember { mutableStateOf(profile?.gstin ?: "") }
    var gstPct by remember { mutableStateOf((profile?.gstPercentage ?: 0.0).toString()) }
    var fssaiNumber by remember { mutableStateOf(profile?.fssaiNumber ?: "") }
    
    val isFssaiValid = fssaiNumber.isNotBlank() && fssaiNumber.length >= 10
    val isGstValid = !gstEnabled || (gstNumber.isNotBlank() && isValidTaxPercentage(gstPct))
    val isSaveEnabled = isFssaiValid && isGstValid

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ConfigCard {
            ParchmentTextField(value = country, onValueChange = { country = it }, label = "Country")
            Spacer(modifier = Modifier.height(16.dp))
            ParchmentTextField(
                value = fssaiNumber, 
                onValueChange = { fssaiNumber = it }, 
                label = "FSSAI Number (Mandatory)",
                isError = fssaiNumber.isNotEmpty() && !isFssaiValid,
                supportingText = if (fssaiNumber.isNotEmpty() && !isFssaiValid) "Invalid FSSAI Number" else null
            )
            if (country.equals("India", true)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("GST Registered", color = TextGold)
                    Switch(checked = gstEnabled, onCheckedChange = { gstEnabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50)))
                }
                if (gstEnabled) {
                    ParchmentTextField(
                        value = gstNumber, 
                        onValueChange = { gstNumber = it.uppercase() }, 
                        label = "GSTIN (Mandatory)",
                        isError = gstEnabled && gstNumber.isEmpty()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ParchmentTextField(
                        value = gstPct, 
                        onValueChange = { gstPct = it }, 
                        label = "GST % (Mandatory)",
                        isError = gstEnabled && !isValidTaxPercentage(gstPct),
                        supportingText = if (gstEnabled && !isValidTaxPercentage(gstPct)) "Invalid GST %" else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { profile?.copy(country = country, gstEnabled = gstEnabled, gstin = gstNumber, gstPercentage = gstPct.toDoubleOrNull() ?: 0.0, fssaiNumber = fssaiNumber)?.let { onSave(it) } }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), enabled = isSaveEnabled) { Text("Save") }
        }
    }
}

@Composable
fun ParchmentTextField(value: String, onValueChange: (String) -> Unit, label: String, trailingIcon: @Composable (() -> Unit)? = null, isError: Boolean = false, supportingText: String? = null) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label, fontSize = 12.sp, color = TextGold) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), trailingIcon = trailingIcon, isError = isError, supportingText = supportingText?.let { { Text(it, color = DangerRed, fontSize = 11.sp) } }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BorderGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = TextGold, unfocusedLabelColor = TextGold.copy(alpha = 0.7f), cursorColor = PrimaryGold, errorBorderColor = DangerRed, errorLabelColor = DangerRed, errorCursorColor = DangerRed), singleLine = true)
}

private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try { context.contentResolver.openInputStream(uri)?.use { input -> File(context.filesDir, fileName).let { file -> FileOutputStream(file).use { output -> input.copyTo(output) }; file.absolutePath } } } catch (_: Exception) { null }
}

private fun loadBitmap(path: String): Bitmap? {
    return try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
}

@Composable
fun InventoryConfigView(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No categories found", color = TextGold)
            }
        } else {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBrown1,
                contentColor = PrimaryGold,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryGold
                        )
                    }
                }
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(category.name, fontSize = 12.sp) }
                    )
                }
            }

            val categoryId = categories[selectedTab].id
            val items by viewModel.getMenuWithVariantsByCategory(categoryId).collectAsState(emptyList())

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(items) { menuWithVariants ->
                    InventoryItemCard(menuWithVariants, viewModel)
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(menuWithVariants: MenuWithVariants, viewModel: SettingsViewModel) {
    val item = menuWithVariants.menuItem
    var stock by remember { mutableStateOf(item.currentStock.toString()) }
    var threshold by remember { mutableStateOf(item.lowStockThreshold.toString()) }

    ConfigCard {
        Text(item.name, color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Available Stock", color = TextGold, fontSize = 12.sp)
                ParchmentTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = "Current Qty"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.updateItemStock(item.id, stock.toDoubleOrNull() ?: 0.0) },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Update Stock", fontSize = 10.sp, color = Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Low Stock Alert", color = TextGold, fontSize = 12.sp)
                ParchmentTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = "Threshold"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.updateItemThreshold(item.id, threshold.toDoubleOrNull() ?: 0.0) },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Update Threshold", fontSize = 10.sp, color = DarkBrown1)
                }
            }
        }
    }
}
