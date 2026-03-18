package com.khanabook.lite.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    viewModel: MenuViewModel
) {
    val categories by viewModel.categories.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val disabledCount by viewModel.disabledItemsCount.collectAsState()
    val addOnsCount by viewModel.menuAddOnsCount.collectAsState()
    val ocrImportUiState by viewModel.ocrImportUiState.collectAsState()
    val scannedDrafts = ocrImportUiState.drafts
    val configMode = ocrImportUiState.configMode

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MenuItemEntity?>(null) }
    var showVariantsFor by remember { mutableStateOf<MenuWithVariants?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedCategoryName = remember(categories, selectedCategoryId) {
        categories.firstOrNull { it.id == selectedCategoryId }?.name
    }

    val pdfPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.extractTextFromPdf(context, it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBrown1, DarkBrown2)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (configMode != null) viewModel.setConfigMode(null) else onBack() }) {
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
                    IconButton(onClick = { viewModel.setConfigMode(null) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Change Mode", tint = PrimaryGold)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            if (configMode == null) {
                
                ModeSelectionView(
                    selectedCategoryName = selectedCategoryName,
                    onManualClick = { viewModel.setConfigMode("manual") },
                    onScanClick = { viewModel.setConfigMode("scan") },
                    onPdfClick = { pdfPickerLauncher.launch("application/pdf") }
                )
            } else {
                
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
                onConfirm = { name, price, foodType ->
                    selectedCategoryId?.let { viewModel.addItem(it, name, price, foodType) }
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
                onConfirm = { name, price, foodType ->
                    viewModel.updateItem(
                        item.copy(
                            name = name,
                            basePrice = price.toString(),
                            foodType = foodType
                        )
                    )
                    android.widget.Toast.makeText(context, "\"$name\" updated", android.widget.Toast.LENGTH_SHORT).show()
                    editingItem = null
                }
            )
        }

        
        val ocrState = ocrImportUiState
        AnimatedVisibility(
            visible = ocrState.isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OcrLoadingOverlay(label = ocrState.processingLabel)
        }

        LaunchedEffect(ocrState.error) {
            ocrState.error?.let {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                viewModel.setError(null)
            }
        }

        if (scannedDrafts.isNotEmpty()) {
            ReviewScannedItemsSheet(
                drafts = scannedDrafts,
                onDismiss = { viewModel.clearDrafts() },
                onConfirm = {
                    selectedCategoryId?.let { viewModel.saveDraftsToCategory(it) }
                },
                onUpdateDraft = { index, updated -> viewModel.updateDraft(index, updated) },
                onToggleSelection = { index -> viewModel.toggleDraftSelection(index) },
                onToggleFoodType = { index -> viewModel.toggleDraftFoodType(index) },
                onSelectAll = { viewModel.selectAllDrafts(true) },
                onDeselectAll = { viewModel.selectAllDrafts(false) }
            )
        }

        LaunchedEffect(ocrState.successMessage) {
            ocrState.successMessage?.let {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }
    }
}


@Composable
fun OcrLoadingOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkBrown2),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = PrimaryGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "AI Processing",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    label,
                    color = TextLight.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ReviewScannedItemsSheet(
    drafts: List<MenuViewModel.DraftMenuItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateDraft: (Int, MenuViewModel.DraftMenuItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onToggleFoodType: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    val selectedCount = drafts.count { it.isSelected }
    val allSelected = selectedCount == drafts.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(DarkBrown1)
            ) {
                
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(PrimaryGold.copy(alpha = 0.4f), CircleShape)
                    )
                }

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Review Detected Items",
                            color = PrimaryGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${drafts.size} items found · $selectedCount selected",
                            color = TextGold.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextGold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Text("Item Name", color = TextGold.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Price", color = TextGold.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
                }

                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(drafts) { index, draft ->
                        val bgColor by animateColorAsState(
                            targetValue = if (draft.isSelected) DarkBrown2 else Color.Transparent,
                            animationSpec = tween(200),
                            label = "item_bg"
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgColor)
                                .border(
                                    width = 0.5.dp,
                                    color = if (draft.isSelected) BorderGold else BorderGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onToggleSelection(index) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (draft.isSelected) PrimaryGold else Color.Transparent
                                        )
                                        .border(
                                            1.5.dp,
                                            if (draft.isSelected) PrimaryGold else TextGold.copy(alpha = 0.4f),
                                            RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (draft.isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DarkBrown1,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                
                                Column(modifier = Modifier.weight(1f)) {
                                    BasicTextField(
                                        value = draft.name,
                                        onValueChange = { onUpdateDraft(index, draft.copy(name = it)) },
                                        textStyle = TextStyle(
                                            color = if (draft.isSelected) TextLight else TextLight.copy(alpha = 0.4f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (!draft.isSelected) TextDecoration.LineThrough else null
                                        ),
                                        cursorBrush = SolidColor(PrimaryGold)
                                    )
                                    if (!draft.categoryName.isNullOrBlank()) {
                                        Text(
                                            draft.categoryName,
                                            color = PrimaryGold.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                
                                if (draft.variants.size <= 1) {
                                    Row(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .background(DarkBrown1.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("₹", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        BasicTextField(
                                            value = if (draft.price == 0.0) "" else draft.price.toInt().toString(),
                                            onValueChange = { raw ->
                                                val p = raw.toDoubleOrNull() ?: 0.0
                                                onUpdateDraft(index, draft.copy(price = p))
                                            },
                                            textStyle = TextStyle(
                                                color = if (draft.isSelected) TextLight else TextLight.copy(alpha = 0.4f),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.End
                                            ),
                                            cursorBrush = SolidColor(PrimaryGold),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = { onToggleFoodType(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = if (draft.foodType == "veg") Color(0xFF4CAF50) else Color(0xFFE53935),
                                        modifier = Modifier.size(12.dp).border(1.dp, Color.White, CircleShape)
                                    )
                                }
                            }

                            
                            if (draft.variants.size > 1) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 36.dp, top = 8.dp, end = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    draft.variants.forEachIndexed { vIndex, variant ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BasicTextField(
                                                value = variant.name,
                                                onValueChange = { newName ->
                                                    val newVariants = draft.variants.toMutableList()
                                                    newVariants[vIndex] = variant.copy(name = newName)
                                                    onUpdateDraft(index, draft.copy(variants = newVariants))
                                                },
                                                textStyle = TextStyle(
                                                    color = TextGold.copy(alpha = 0.8f),
                                                    fontSize = 12.sp
                                                ),
                                                cursorBrush = SolidColor(PrimaryGold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            Row(
                                                modifier = Modifier
                                                    .width(64.dp)
                                                    .background(DarkBrown1.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("₹", color = PrimaryGold, fontSize = 11.sp)
                                                BasicTextField(
                                                    value = if (variant.price == 0.0) "" else variant.price.toInt().toString(),
                                                    onValueChange = { raw ->
                                                        val p = raw.toDoubleOrNull() ?: 0.0
                                                        val newVariants = draft.variants.toMutableList()
                                                        newVariants[vIndex] = variant.copy(price = p)
                                                        onUpdateDraft(index, draft.copy(variants = newVariants))
                                                    },
                                                    textStyle = TextStyle(
                                                        color = TextLight.copy(alpha = 0.9f),
                                                        fontSize = 11.sp,
                                                        textAlign = TextAlign.End
                                                    ),
                                                    cursorBrush = SolidColor(PrimaryGold),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                
                Surface(
                    color = DarkBrown2,
                    border = BorderStroke(0.5.dp, BorderGold.copy(alpha = 0.3f)),
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, NonVegRed.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NonVegRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Discard")
                        }
                        Button(
                            onClick = onConfirm,
                            enabled = selectedCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGold,
                                contentColor = DarkBrown1,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                                disabledContentColor = Color.Gray
                            ),
                            modifier = Modifier.weight(2f)
                        ) {
                            Icon(Icons.Default.PlaylistAddCheck, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Add $selectedCount Item${if (selectedCount == 1) "" else "s"} to Menu",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectionView(
    selectedCategoryName: String?,
    onManualClick: () -> Unit,
    onScanClick: () -> Unit,
    onPdfClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        
        if (!selectedCategoryName.isNullOrBlank()) {
            Surface(
                color = PrimaryGold.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Adding to: \"$selectedCategoryName\"",
                        color = PrimaryGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Text(
            "How would you like to set up your menu?",
            color = TextLight.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        
        ModeCard(
            title = "Manual Setup",
            subtitle = "Type in each item individually",
            description = "Best for small menus or fine-grained control.",
            icon = Icons.Default.EditNote,
            iconBg = Color(0xFF1565C0),
            onClick = onManualClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        
        ModeCard(
            title = "Scan Menu Photo",
            subtitle = "AI reads your printed/digital menu",
            description = "Point camera at a menu card, English works best.",
            icon = Icons.Default.CameraAlt,
            iconBg = Color(0xFF6A1B9A),
            badge = "AI",
            onClick = onScanClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        
        ModeCard(
            title = "Upload PDF Menu",
            subtitle = "Import directly from a PDF file",
            description = "Works with text-based PDFs (not scanned images).",
            icon = Icons.Default.PictureAsPdf,
            iconBg = Color(0xFFB71C1C),
            onClick = onPdfClick
        )
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String = "",
    description: String,
    icon: ImageVector,
    iconBg: Color = Color(0xFF5D4037),
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = DarkBrown2,
        border = BorderStroke(1.dp, BorderGold.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(iconBg.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, iconBg.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = LightGold, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = PrimaryGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                badge,
                                color = DarkBrown1,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = TextGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, color = TextLight.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 16.sp)
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PrimaryGold.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MenuConfigurationContent(
    categories: List<CategoryEntity>,
    menuItems: List<MenuWithVariants>,
    selectedCategoryId: Long?,
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
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var price by remember { mutableStateOf(initialItem?.basePrice?.toString() ?: "") }
    var foodType by remember { mutableStateOf(initialItem?.foodType ?: "veg") }

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
                    onConfirm(name, price.toDoubleOrNull() ?: 0.0, foodType)
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
