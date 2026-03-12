package com.khanabook.lite.pos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.khanabook.lite.pos.data.local.entity.CategoryEntity
import com.khanabook.lite.pos.data.local.entity.ItemVariantEntity
import com.khanabook.lite.pos.data.local.entity.MenuItemEntity
import com.khanabook.lite.pos.data.local.relation.MenuWithVariants
import com.khanabook.lite.pos.ui.theme.*
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuConfigurationScreen(
    onBack: () -> Unit,
    onScanClick: (String?) -> Unit = {},
    scannedText: String? = null,
    onScannedTextConsumed: () -> Unit = {},
    viewModel: MenuViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val disabledCount by viewModel.disabledItemsCount.collectAsState()
    val addOnsCount by viewModel.menuAddOnsCount.collectAsState()
    val scannedDrafts by viewModel.scannedDrafts.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MenuItemEntity?>(null) }
    var showVariantsFor by remember { mutableStateOf<MenuWithVariants?>(null) }
    
    // Mode Selection State: null = No selection, "manual" = Manual mode, "scan" = Scan mode
    var configMode by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedCategoryName = remember(categories, selectedCategoryId) {
        categories.firstOrNull { it.id == selectedCategoryId }?.name
    }

    val pdfPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.extractTextFromPdf(context, it) }
    }

    // Process scanned text
    LaunchedEffect(scannedText) {
        if (scannedText != null && selectedCategoryId != null) {
            viewModel.parseScannedTextToDrafts(scannedText)
            onScannedTextConsumed()
        } else if (scannedText != null && selectedCategoryId == null) {
            android.widget.Toast.makeText(context, "Please select a category before scanning.", android.widget.Toast.LENGTH_LONG).show()
            onScannedTextConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBrown1, DarkBrown2)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (configMode != null) configMode = null else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryGold)
                }
                Text(
                    if (configMode == null) "Menu Configuration" else if (configMode == "manual") "Manual Configuration" else "Scan/Upload Menu",
                    modifier = Modifier.weight(1f),
                    color = PrimaryGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (configMode != null) {
                    IconButton(onClick = { configMode = null }) {
                        Icon(Icons.Default.Settings, contentDescription = "Change Mode", tint = PrimaryGold)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            if (configMode == null) {
                // Mode Selection UI
                ModeSelectionView(
                    selectedCategoryName = selectedCategoryName,
                    onManualClick = { configMode = "manual" },
                    onScanClick = { configMode = "scan" },
                    onPdfClick = { pdfPickerLauncher.launch("application/pdf") }
                )
            } else {
                // Actual Content
                MenuConfigurationContent(
                    categories = categories,
                    menuItems = menuItems,
                    selectedCategoryId = selectedCategoryId,
                    searchQuery = searchQuery,
                    disabledCount = disabledCount,
                    addOnsCount = addOnsCount,
                    viewModel = viewModel,
                    onScanClick = { onScanClick(selectedCategoryName) },
                    onPdfClick = { pdfPickerLauncher.launch("application/pdf") },
                    showScanOption = configMode == "scan",
                    onAddCategory = { showAddCategoryDialog = true },
                    onAddItem = { showAddItemDialog = true },
                    onEditItem = { editingItem = it },
                    onShowVariants = { showVariantsFor = it },
                    onDeleteCategory = { cat ->
                        viewModel.deleteCategory(cat)
                        android.widget.Toast.makeText(context, "\"${cat.name}\" deleted", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onDeleteItem = { item ->
                        viewModel.deleteItem(item)
                        android.widget.Toast.makeText(context, "\"${item.name}\" deleted", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { name, isVeg ->
                    viewModel.addCategory(name, isVeg)
                    android.widget.Toast.makeText(context, "\"$name\" category added", android.widget.Toast.LENGTH_SHORT).show()
                    showAddCategoryDialog = false
                }
            )
        }

        if (showAddItemDialog) {
            ItemDialog(
                onDismiss = { showAddItemDialog = false },
                onConfirm = { name, price, foodType, stock, threshold ->
                    selectedCategoryId?.let { viewModel.addItem(it, name, price, foodType, stock, threshold) }
                    android.widget.Toast.makeText(context, "\"$name\" added to menu", android.widget.Toast.LENGTH_SHORT).show()
                    showAddItemDialog = false
                }
            )
        }

        showVariantsFor?.let { itemWithVariants ->
            ManageVariantsDialog(
                itemWithVariants = itemWithVariants,
                onDismiss = { showVariantsFor = null },
                onAddVariant = { name, price ->
                    viewModel.addVariant(itemWithVariants.menuItem.id, name, price)
                    android.widget.Toast.makeText(context, "\"$name\" variant added", android.widget.Toast.LENGTH_SHORT).show()
                },
                onUpdateVariant = { variant ->
                    viewModel.updateVariant(variant)
                    android.widget.Toast.makeText(context, "\"${variant.variantName}\" updated", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDeleteVariant = { variant ->
                    viewModel.deleteVariant(variant)
                    android.widget.Toast.makeText(context, "\"${variant.variantName}\" variant removed", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }

        editingItem?.let { item ->
            ItemDialog(
                initialItem = item,
                onDismiss = { editingItem = null },
                onConfirm = { name, price, foodType, stock, threshold ->
                    viewModel.updateItem(
                        item.copy(
                            name = name,
                            basePrice = price,
                            foodType = foodType,
                            currentStock = stock,
                            lowStockThreshold = threshold
                        )
                    )
                    android.widget.Toast.makeText(context, "\"$name\" updated", android.widget.Toast.LENGTH_SHORT).show()
                    editingItem = null
                }
            )
        }

        if (scannedDrafts.isNotEmpty()) {
            ReviewScannedItemsDialog(
                drafts = scannedDrafts,
                onDismiss = { viewModel.clearDrafts() },
                onConfirm = { 
                    selectedCategoryId?.let { 
                        viewModel.saveDraftsToCategory(it)
                        android.widget.Toast.makeText(context, "Items added successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onUpdateDraft = { index, updated -> viewModel.updateDraft(index, updated) },
                onToggleSelection = { index -> viewModel.toggleDraftSelection(index) }
            )
        }
    }
}

@Composable
fun ReviewScannedItemsDialog(
    drafts: List<MenuViewModel.DraftMenuItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateDraft: (Int, MenuViewModel.DraftMenuItem) -> Unit,
    onToggleSelection: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Scanned Items", color = PrimaryGold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Verify and edit detected items before adding them to your menu.", color = TextLight, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(drafts) { index, draft ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = draft.isSelected,
                                onCheckedChange = { onToggleSelection(index) },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryGold)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = draft.name,
                                    onValueChange = { onUpdateDraft(index, draft.copy(name = it)) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₹", color = PrimaryGold, fontSize = 12.sp)
                                    BasicTextField(
                                        value = draft.price.toString(),
                                        onValueChange = { 
                                            val price = it.toDoubleOrNull() ?: 0.0
                                            onUpdateDraft(index, draft.copy(price = price))
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = TextGold, fontSize = 12.sp),
                                        modifier = Modifier.width(60.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { onToggleSelection(index) }) {
                                Icon(
                                    if (draft.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (draft.isSelected) PrimaryGold else Color.Gray
                                )
                            }
                        }
                        HorizontalDivider(color = BorderGold.copy(alpha = 0.2f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = DarkBrown1)
            ) { Text("Add Selected (${drafts.count { it.isSelected }})") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Discard All", color = Color.Red.copy(alpha = 0.7f)) }
        },
        containerColor = DarkBrown2
    )
}

@Composable
fun ModeSelectionView(
    selectedCategoryName: String?,
    onManualClick: () -> Unit,
    onScanClick: () -> Unit,
    onPdfClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModeCard(
                title = "Manually Setup",
                description = "Add categories and items one by one using a form.",
                icon = Icons.Default.EditNote,
                onClick = onManualClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            ModeCard(
                title = "Scan Menu",
                description = "Capture a photo of the menu and process it for the selected category.",
                icon = Icons.Default.QrCodeScanner,
                onClick = onScanClick
            )
        }

        ExtendedFloatingActionButton(
            onClick = onPdfClick,
            modifier = Modifier.align(Alignment.BottomEnd),
            containerColor = PrimaryGold,
            contentColor = DarkBrown1,
            text = {
                Text(
                    if (!selectedCategoryName.isNullOrBlank()) {
                        "Upload PDF for \"$selectedCategoryName\""
                    } else {
                        "Upload PDF"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            icon = {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            }
        )
    }
}

@Composable
fun ModeCard(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ParchmentBG),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(PrimaryGold.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(32.dp))
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DarkBrown1, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
            }
            
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PrimaryGold)
        }
    }
}

@Composable
fun MenuConfigurationContent(
    categories: List<CategoryEntity>,
    menuItems: List<MenuWithVariants>,
    selectedCategoryId: Int?,
    searchQuery: String,
    disabledCount: Int,
    addOnsCount: Int,
    viewModel: MenuViewModel,
    onScanClick: () -> Unit,
    onPdfClick: () -> Unit,
    showScanOption: Boolean,
    onAddCategory: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (MenuItemEntity) -> Unit,
    onShowVariants: (MenuWithVariants) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDeleteItem: (MenuItemEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentBG),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search for an item or category", fontSize = 12.sp, color = Color.Gray) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.LightGray,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                FilterBadge(
                    label = "MENU ADD ONS",
                    count = addOnsCount,
                    backgroundColor = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )

                FilterBadge(
                    label = "DISABLED",
                    count = disabledCount,
                    backgroundColor = Color(0xFF757575),
                    icon = Icons.Default.VisibilityOff,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))

            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CATEGORY (${categories.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("ADD NEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), modifier = Modifier.clickable { onAddCategory() })
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(categories) { category ->
                            CategoryItemRow(
                                category = category,
                                isSelected = selectedCategoryId == category.id,
                                onClick = { viewModel.selectCategory(category.id) },
                                onToggle = { viewModel.toggleCategory(category.id, it) },
                                onDelete = { onDeleteCategory(category) }
                            )
                        }
                    }
                }

                VerticalDivider(color = Color.Black.copy(alpha = 0.1f))

                Column(modifier = Modifier.weight(1.5f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ITEM (${menuItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(menuItems) { itemWithVariants ->
                                MenuItemRow(
                                    itemWithVariants = itemWithVariants,
                                    onClick = { onEditItem(itemWithVariants.menuItem) },
                                    onToggle = { viewModel.toggleItem(itemWithVariants.menuItem.id, it) },
                                    onManageVariants = { onShowVariants(itemWithVariants) },
                                    onDelete = { onDeleteItem(itemWithVariants.menuItem) }
                                )
                            }
                        }
                        
                        val canAddItem = categories.isNotEmpty() && selectedCategoryId != null
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Conditional Scan Button
                            if (showScanOption) {
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable(enabled = canAddItem) { onScanClick() },
                                    color = if (canAddItem) ParchmentBG else Color.LightGray.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (canAddItem) Color(0xFF5D4037) else Color.Gray),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan Menu",
                                            tint = if (canAddItem) Color(0xFF5D4037) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable(enabled = canAddItem) { onPdfClick() },
                                    color = if (canAddItem) ParchmentBG else Color.LightGray.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (canAddItem) Color(0xFF5D4037) else Color.Gray),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = "Upload PDF",
                                            tint = if (canAddItem) Color(0xFF5D4037) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Add New Button
                            if (!showScanOption) {
                                Surface(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clickable(enabled = canAddItem) { onAddItem() },
                                    color = if (canAddItem) ParchmentBG else Color.LightGray.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (canAddItem) Color(0xFF5D4037) else Color.Gray),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "ADD NEW", 
                                            color = if (canAddItem) Color(0xFF5D4037) else Color.Gray, 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isVeg by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category", color = PrimaryGold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = PrimaryGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedLabelColor = PrimaryGold, unfocusedLabelColor = TextGold)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isVeg, onClick = { isVeg = true }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold))
                    Text("Veg", color = TextLight)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !isVeg, onClick = { isVeg = false }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold))
                    Text("Non-Veg", color = TextLight)
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, isVeg) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = DarkBrown1)) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryGold) }
        },
        containerColor = DarkBrown2
    )
}

@Composable
fun ItemDialog(
    initialItem: MenuItemEntity? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, Double, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var price by remember { mutableStateOf(initialItem?.basePrice?.toString() ?: "") }
    var foodType by remember { mutableStateOf(initialItem?.foodType ?: "veg") }
    var initialStock by remember { mutableStateOf(initialItem?.currentStock?.toString() ?: "0") }
    var threshold by remember { mutableStateOf(initialItem?.lowStockThreshold?.toString() ?: "10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "Add New Menu Item" else "Edit Menu Item", color = PrimaryGold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = PrimaryGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedLabelColor = PrimaryGold, unfocusedLabelColor = TextGold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Base Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = PrimaryGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedLabelColor = PrimaryGold, unfocusedLabelColor = TextGold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialStock,
                        onValueChange = { initialStock = it },
                        label = { Text("Stock") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = PrimaryGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedLabelColor = PrimaryGold, unfocusedLabelColor = TextGold)
                    )
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        label = { Text("Low Alert") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = PrimaryGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedLabelColor = PrimaryGold, unfocusedLabelColor = TextGold)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = foodType == "veg", onClick = { foodType = "veg" }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold))
                    Text("Veg", color = TextLight)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = foodType == "non-veg", onClick = { foodType = "non-veg" }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold))
                    Text("Non-Veg", color = TextLight)
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (name.isNotBlank()) {
                    onConfirm(name, price.toDoubleOrNull() ?: 0.0, foodType, initialStock.toDoubleOrNull() ?: 0.0, threshold.toDoubleOrNull() ?: 10.0)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = DarkBrown1)) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryGold) }
        },
        containerColor = DarkBrown2
    )
}

@Composable
fun ManageVariantsDialog(
    itemWithVariants: MenuWithVariants,
    onDismiss: () -> Unit,
    onAddVariant: (String, Double) -> Unit,
    onUpdateVariant: (ItemVariantEntity) -> Unit,
    onDeleteVariant: (ItemVariantEntity) -> Unit
) {
    var newVariantName by remember { mutableStateOf("") }
    var newVariantPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Variants: ${itemWithVariants.menuItem.name}", color = PrimaryGold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Add New Variant", fontWeight = FontWeight.Bold, color = PrimaryGold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newVariantName,
                        onValueChange = { newVariantName = it },
                        label = { Text("Name (e.g. Full)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = PrimaryGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedLabelColor = PrimaryGold, unfocusedLabelColor = TextGold)
                    )
                    OutlinedTextField(
                        value = newVariantPrice,
                        onValueChange = { newVariantPrice = it },
                        label = { Text("Price") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = PrimaryGold, unfocusedBorderColor = BorderGold.copy(alpha = 0.5f), focusedLabelColor = PrimaryGold, unfocusedLabelColor = TextGold)
                    )
                }
                Button(
                    onClick = {
                        if (newVariantName.isNotBlank() && newVariantPrice.isNotBlank()) {
                            onAddVariant(newVariantName, newVariantPrice.toDoubleOrNull() ?: 0.0)
                            newVariantName = ""
                            newVariantPrice = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = DarkBrown1)
                ) { Text("Add Variant") }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderGold.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Existing Variants", fontWeight = FontWeight.Bold, color = PrimaryGold)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(itemWithVariants.variants) { variant ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(variant.variantName, color = TextLight, modifier = Modifier.weight(1f))
                            Text("₹${variant.price}", color = PrimaryGold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { onDeleteVariant(variant) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = DarkBrown1)) { Text("Done") }
        },
        containerColor = DarkBrown2
    )
}

@Composable
fun CategoryItemRow(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) PrimaryGold.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = category.isActive,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.6f),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4CAF50))
        )
        Text(
            category.name,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF1976D2) else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun MenuItemRow(
    itemWithVariants: MenuWithVariants,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onManageVariants: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = itemWithVariants.menuItem.isAvailable,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.6f),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4CAF50))
        )
        
        Column(modifier = Modifier.weight(1f).clickable { onClick() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodTypeIconSmall(itemWithVariants.menuItem.foodType == "veg")
                Spacer(modifier = Modifier.width(4.dp))
                Text(itemWithVariants.menuItem.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            if (itemWithVariants.variants.isNotEmpty()) {
                Text("${itemWithVariants.variants.size} variants", fontSize = 10.sp, color = Color.Gray)
            } else {
                Text("₹${itemWithVariants.menuItem.basePrice}", fontSize = 10.sp, color = Color.Gray)
            }
        }
        
        IconButton(onClick = onManageVariants, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Layers, contentDescription = "Variants", tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
        }
        
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun FilterBadge(label: String, count: Int, backgroundColor: Color, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.size(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(count.toString(), color = backgroundColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FoodTypeIconSmall(isVeg: Boolean) {
    val color = if (isVeg) Color(0xFF4CAF50) else Color(0xFFF44336)
    Box(
        modifier = Modifier
            .size(10.dp)
            .border(1.dp, color, RoundedCornerShape(1.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color, RoundedCornerShape(100.dp))
        )
    }
}
