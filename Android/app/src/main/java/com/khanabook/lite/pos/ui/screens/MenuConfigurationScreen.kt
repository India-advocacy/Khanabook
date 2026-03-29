@file:OptIn(ExperimentalMaterial3Api::class)

package com.khanabook.lite.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khanabook.lite.pos.data.local.entity.CategoryEntity
import com.khanabook.lite.pos.data.local.entity.MenuItemEntity
import com.khanabook.lite.pos.ui.theme.*
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel

object ReviewSheetLayout {
    val PRICE_WIDTH = 90.dp
    val CHECKBOX_WIDTH = 26.dp
    val CHECKBOX_GAP = 14.dp
    val FOOD_ICON_WIDTH = 40.dp
    val HORIZONTAL_PADDING = 16.dp
    val CARD_PADDING = 16.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuConfigurationScreen(
    navController: NavController,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val ocrUiState by viewModel.ocrImportUiState.collectAsState()
    val context = LocalContext.current
    
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.extractTextFromPdf(context, it) }
    }

    val onBack: () -> Unit = {
        if (ocrUiState.configMode != null) {
            viewModel.setConfigMode(null)
        } else {
            navController.popBackStack()
        }
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(ocrUiState.successMessage) {
        ocrUiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Configuration", color = PrimaryGold) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBrown1)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBrown1
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (ocrUiState.configMode == null) {
                ModeSelectionView(
                    selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name,
                    onManualClick = { viewModel.setConfigMode("manual") },
                    onSmartImportClick = {
                        val catName = categories.find { it.id == selectedCategoryId }?.name ?: ""
                        navController.navigate("ocr_scanner/$catName")
                    },
                    onPdfClick = { pdfLauncher.launch("application/pdf") }
                )
            } else {
                ManualMenuView(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    menuItems = menuItems,
                    onCategorySelect = { viewModel.selectCategory(it) },
                    onAddCategory = { viewModel.addCategory(it, true) },
                    onAddItem = { name, price, type -> 
                        selectedCategoryId?.let { viewModel.addItem(it, name, price, type) }
                    }
                )
            }

            if (ocrUiState.drafts.isNotEmpty()) {
                ReviewDetectedItemsSheet(
                    drafts = ocrUiState.drafts,
                    onDismiss = { viewModel.clearDrafts() },
                    onConfirm = { 
                        selectedCategoryId?.let { viewModel.saveDraftsToCategory(it) }
                    },
                    onToggleSelection = { viewModel.toggleDraftSelection(it) },
                    onUpdateDraft = { index, draft -> viewModel.updateDraft(index, draft) },
                    onToggleFoodType = { viewModel.toggleDraftFoodType(it) }
                )
            }
        }
    }
}

@Composable
fun ReviewDetectedItemsSheet(
    drafts: List<MenuViewModel.DraftMenuItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onUpdateDraft: (Int, MenuViewModel.DraftMenuItem) -> Unit,
    onToggleFoodType: (Int) -> Unit
) {
    val selectedCount = drafts.count { it.isSelected }
    
    val headerPadding = ReviewSheetLayout.HORIZONTAL_PADDING + ReviewSheetLayout.CARD_PADDING
    val checkboxPlaceholder = ReviewSheetLayout.CHECKBOX_WIDTH + ReviewSheetLayout.CHECKBOX_GAP

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1.0f) // FULL SCREEN
                .statusBarsPadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DarkBrown1)
                .clickable(enabled = false) { }
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
                        fontSize = 24.sp, // Increased
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${drafts.size} items found · $selectedCount selected",
                        color = TextGold.copy(alpha = 0.7f),
                        fontSize = 15.sp // Increased
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
                    .padding(horizontal = headerPadding, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(checkboxPlaceholder))
                Text("Item Name", color = TextGold.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Price", color = TextGold.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(ReviewSheetLayout.PRICE_WIDTH))
                Spacer(modifier = Modifier.width(ReviewSheetLayout.FOOD_ICON_WIDTH))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ReviewSheetLayout.HORIZONTAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(12.dp), // Increased spacing
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val groupedDrafts = drafts.withIndex().groupBy { it.value.categoryName ?: "Uncategorized" }
                
                groupedDrafts.forEach { (categoryName, indexedItems) ->
                    val allInCategorySelected = indexedItems.all { it.value.isSelected }
                    
                    item(key = "header_$categoryName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (allInCategorySelected) PrimaryGold else Color.Transparent
                                    )
                                    .border(
                                        1.5.dp,
                                        if (allInCategorySelected) PrimaryGold else TextGold.copy(alpha = 0.5f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { 
                                        val targetSelection = !allInCategorySelected
                                        indexedItems.forEach { indexed ->
                                            if (indexed.value.isSelected != targetSelection) {
                                                onToggleSelection(indexed.index)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (allInCategorySelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = DarkBrown1,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                categoryName.uppercase(),
                                color = PrimaryGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    items(indexedItems.size) { i ->
                        val index = indexedItems[i].index
                        val draft = indexedItems[i].value
                        val bgColor by animateColorAsState(
                            targetValue = if (draft.isSelected) DarkBrown2 else Color.Transparent,
                            animationSpec = tween(200),
                            label = "item_bg"
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(
                                    width = 0.5.dp,
                                    color = if (draft.isSelected) BorderGold else BorderGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onToggleSelection(index) }
                                .padding(horizontal = ReviewSheetLayout.CARD_PADDING, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(ReviewSheetLayout.CHECKBOX_WIDTH)
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(ReviewSheetLayout.CHECKBOX_GAP))

                                Column(modifier = Modifier.weight(1f)) {
                                    BasicTextField(
                                        value = draft.name,
                                        onValueChange = { onUpdateDraft(index, draft.copy(name = it)) },
                                        textStyle = TextStyle(
                                            color = if (draft.isSelected) TextLight else TextLight.copy(alpha = 0.4f),
                                            fontSize = 18.sp, // INCREASED
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (!draft.isSelected) TextDecoration.LineThrough else null
                                        ),
                                        cursorBrush = SolidColor(PrimaryGold)
                                    )
                                    
                                    BasicTextField(
                                        value = draft.categoryName ?: "",
                                        onValueChange = { onUpdateDraft(index, draft.copy(categoryName = it.ifBlank { null })) },
                                        textStyle = TextStyle(
                                            color = PrimaryGold.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (draft.categoryName.isNullOrBlank()) {
                                                Text("Tap to set category", color = PrimaryGold.copy(alpha = 0.3f), fontSize = 12.sp)
                                            }
                                            innerTextField()
                                        },
                                        cursorBrush = SolidColor(PrimaryGold)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                if (draft.variants.size <= 1) {
                                    Row(
                                        modifier = Modifier
                                            .width(ReviewSheetLayout.PRICE_WIDTH)
                                            .background(DarkBrown1.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("₹", color = PrimaryGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        BasicTextField(
                                            value = if (draft.price == 0.0) "" else draft.price.toInt().toString(),
                                            onValueChange = { raw ->
                                                val p = raw.toDoubleOrNull() ?: 0.0
                                                onUpdateDraft(index, draft.copy(price = p))
                                            },
                                            textStyle = TextStyle(
                                                color = if (draft.isSelected) TextLight else TextLight.copy(alpha = 0.4f),
                                                fontSize = 16.sp, // INCREASED
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
                                    modifier = Modifier.size(ReviewSheetLayout.FOOD_ICON_WIDTH)
                                ) {
                                    Icon(
                                        Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = if (draft.foodType == "veg") VegGreen else NonVegRed,
                                        modifier = Modifier.size(16.dp).border(1.dp, Color.White, CircleShape)
                                    )
                                }
                            }

                            if (draft.variants.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 44.dp, top = 12.dp, end = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    draft.variants.forEachIndexed { vIndex, variant ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(
                                                        if (variant.isSelected) PrimaryGold.copy(alpha = 0.8f) else Color.Transparent
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (variant.isSelected) PrimaryGold else TextGold.copy(alpha = 0.4f),
                                                        RoundedCornerShape(5.dp)
                                                    )
                                                    .clickable { 
                                                        val newVariants = draft.variants.toMutableList()
                                                        newVariants[vIndex] = variant.copy(isSelected = !variant.isSelected)
                                                        onUpdateDraft(index, draft.copy(variants = newVariants))
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (variant.isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = DarkBrown1,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            BasicTextField(
                                                value = variant.name,
                                                onValueChange = { newName ->
                                                    val newVariants = draft.variants.toMutableList()
                                                    newVariants[vIndex] = variant.copy(name = newName)
                                                    onUpdateDraft(index, draft.copy(variants = newVariants))
                                                },
                                                textStyle = TextStyle(
                                                    color = if (variant.isSelected) TextGold.copy(alpha = 0.8f) else TextGold.copy(alpha = 0.3f),
                                                    fontSize = 15.sp,
                                                    textDecoration = if (!variant.isSelected) TextDecoration.LineThrough else null
                                                ),
                                                cursorBrush = SolidColor(PrimaryGold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            Row(
                                                modifier = Modifier
                                                    .width(80.dp)
                                                    .background(DarkBrown1.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("₹", color = if (variant.isSelected) PrimaryGold else PrimaryGold.copy(alpha = 0.3f), fontSize = 14.sp)
                                                BasicTextField(
                                                    value = if (variant.price == 0.0) "" else variant.price.toInt().toString(),
                                                    onValueChange = { raw ->
                                                        val p = raw.toDoubleOrNull() ?: 0.0
                                                        val newVariants = draft.variants.toMutableList()
                                                        newVariants[vIndex] = variant.copy(price = p)
                                                        onUpdateDraft(index, draft.copy(variants = newVariants))
                                                    },
                                                    textStyle = TextStyle(
                                                        color = if (variant.isSelected) TextLight.copy(alpha = 0.9f) else TextLight.copy(alpha = 0.3f),
                                                        fontSize = 14.sp,
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
                }
            }

            Surface(
                color = DarkBrown2,
                border = BorderStroke(0.5.dp, BorderGold.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.5.dp, NonVegRed.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NonVegRed),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Discard", maxLines = 1, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = DarkBrown1
                        ),
                        modifier = Modifier.weight(2f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Add $selectedCount Items",
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
fun ModeSelectionView(
    selectedCategoryName: String?,
    onManualClick: () -> Unit,
    onSmartImportClick: () -> Unit,
    onPdfClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (!selectedCategoryName.isNullOrBlank()) {
            Surface(
                color = PrimaryGold.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, null, tint = PrimaryGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configuring: $selectedCategoryName", color = PrimaryGold, fontSize = 14.sp)
                }
            }
        }

        Text("Add Menu Items", color = TextGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

        Card(onClick = onManualClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkBrown2)) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PrimaryGold.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Edit, null, tint = PrimaryGold, modifier = Modifier.padding(14.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Manual Entry", color = TextLight, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Add items one by one", color = TextGold.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(onClick = onSmartImportClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkBrown2)) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PrimaryGold.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.CameraAlt, null, tint = PrimaryGold, modifier = Modifier.padding(14.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Camera Scan", color = TextLight, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = PrimaryGold, shape = RoundedCornerShape(4.dp)) {
                            Text("AI", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = DarkBrown1)
                        }
                    }
                    Text("Scan a menu photo", color = TextGold.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(onClick = onPdfClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkBrown2), border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PrimaryGold.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = PrimaryGold, modifier = Modifier.padding(14.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Upload PDF", color = TextLight, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Import from digital menu PDF", color = TextGold.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ManualMenuView(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    menuItems: List<com.khanabook.lite.pos.data.local.relation.MenuWithVariants>,
    onCategorySelect: (Long) -> Unit,
    onAddCategory: (String) -> Unit,
    onAddItem: (String, Double, String) -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategorySelect(category.id) },
                    label = { Text(category.name) }
                )
            }
            item {
                IconButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.Add, null, tint = PrimaryGold)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(menuItems) { itemWithVariants ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkBrown2)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(itemWithVariants.menuItem.name, color = TextLight, fontWeight = FontWeight.Bold)
                            Text("₹${itemWithVariants.menuItem.basePrice}", color = TextGold.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Circle, null, tint = if (itemWithVariants.menuItem.foodType == "veg") VegGreen else NonVegRed, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        Button(onClick = { showAddItemDialog = true }, modifier = Modifier.fillMaxWidth().padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = DarkBrown1), enabled = selectedCategoryId != null) {
            Text("Add New Item")
        }
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Category") },
            text = { OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Category Name") }) },
            confirmButton = { TextButton(onClick = { if (newCatName.isNotBlank()) onAddCategory(newCatName); showAddCategoryDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddItemDialog) {
        var name by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var isVeg by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Item") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isVeg, onCheckedChange = { isVeg = it })
                        Text("Vegetarian")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onAddItem(name, price.toDoubleOrNull() ?: 0.0, if (isVeg) "veg" else "non-veg"); showAddItemDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddItemDialog = false }) { Text("Cancel") } }
        )
    }
}
