package com.khanabook.lite.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.khanabook.lite.pos.data.local.entity.CategoryEntity
import com.khanabook.lite.pos.data.local.entity.ItemVariantEntity
import com.khanabook.lite.pos.data.local.entity.MenuItemEntity
import com.khanabook.lite.pos.data.local.relation.MenuWithVariants
import com.khanabook.lite.pos.data.repository.CategoryRepository
import com.khanabook.lite.pos.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val menuRepository: MenuRepository
) : ViewModel() {

    companion object {
        
        private val trailingPriceRegex =
            Regex("""(?i)(?:[\u20B9\u20A8]|rs\.?|inr)?\s*(\d{1,6}(?:\.\d{1,2})?)\s*(?:[\u20B9\u20A8]|rs\.?|inr)?\s*$""")
        
        private val leadingPriceRegex =
            Regex("""^(?:[\u20B9\u20A8]|rs\.?|inr)?\s*(\d{1,6}(?:\.\d{1,2})?)\s+""")
        
        private val leadingBulletRegex = Regex("""^\s*(?:[\-\*•]+|\d+[.):]?)\s*""")
        private val trailingSeparatorRegex = Regex("""[\s\-:\|.…]+$""")
        private val trailingCurrencyRegex = Regex("""(?i)(?:[\u20B9\u20A8]|rs\.?|inr)\s*$""")
        
        private val skipLineRegex = Regex("""(?i)^(menu|category|item|price|qty|total|subtotal|s\.no|veg|non.?veg|page\s+\d+)\.?\s*$""")
        
        private const val MAX_PRICE = 99999.0

        internal fun parseDraftsFromText(text: String): List<DraftMenuItem> {
            val seen = mutableSetOf<String>()
            return text
                .lineSequence()
                .map { normalizeImportLine(it) }
                .filter { line ->
                    line.length > 2 &&
                    line.any(Char::isLetter) &&
                    !skipLineRegex.matches(line)
                }
                .mapNotNull { parseDraftLine(it) }
                
                .filter { seen.add(it.name.lowercase()) }
                .toList()
        }

        private fun normalizeImportLine(raw: String): String {
            return raw
                .replace('\u20B9', ' ')  
                .replace('\u20A8', ' ')  
                .replace("Rs.", " Rs ")
                .replace("rs.", " Rs ")
                .replace('\u00A0', ' ')  
                .replace('\u2019', '\'')  
                .replace(Regex("""\s{2,}"""), " ")
                .trim()
        }

        private fun parseDraftLine(line: String): DraftMenuItem? {
            
            val noBullet = line.replace(leadingBulletRegex, "").trim()
            if (noBullet.isBlank()) return null

            
            val trailingMatch = trailingPriceRegex.find(noBullet)
            if (trailingMatch != null) {
                val priceVal = trailingMatch.groupValues.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                if (priceVal > MAX_PRICE) return null
                val rawName = noBullet
                    .substring(0, trailingMatch.range.first)
                    .replace(trailingCurrencyRegex, "")
                    .replace(trailingSeparatorRegex, "")
                    .trim()
                val name = toTitleCase(rawName)
                if (name.isBlank() || name.length < 2) return null
                return DraftMenuItem(name, priceVal)
            }

            
            val leadingMatch = leadingPriceRegex.find(noBullet)
            if (leadingMatch != null) {
                val priceVal = leadingMatch.groupValues.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                if (priceVal > MAX_PRICE) return null
                val rawName = noBullet.substring(leadingMatch.range.last + 1).trim()
                val name = toTitleCase(rawName)
                if (name.isBlank() || name.length < 2) return null
                return DraftMenuItem(name, priceVal)
            }

            
            val name = toTitleCase(noBullet.replace(trailingSeparatorRegex, "").trim())
            if (name.isBlank() || name.length < 2) return null
            return DraftMenuItem(name, 0.0)
        }

        private fun toTitleCase(s: String): String {
            return s.lowercase()
                .split(Regex("""\s+"""))
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategoriesFlow()
        .onEach { list ->
            if (_selectedCategoryId.value == null && list.isNotEmpty()) {
                _selectedCategoryId.value = list.first().id
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId

    private val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    val menuItems: StateFlow<List<MenuWithVariants>> = combine(_selectedCategoryId, debouncedSearchQuery) { id, query ->
        id to query
    }.flatMapLatest { (id, query) ->
        if (id != null) {
            menuRepository.getMenuWithVariantsByCategoryFlow(id).map { items ->
                if (query.isBlank()) items
                else items.filter { it.menuItem.name.contains(query, ignoreCase = true) }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disabledItemsCount: StateFlow<Int> = menuRepository.getAllItemsFlow()
        .map { items -> items.count { !it.isAvailable } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val menuAddOnsCount: StateFlow<Int> = categories
        .map { list ->
            list.count { cat -> 
                val name = cat.name.lowercase()
                name.contains("add-on") || name.contains("extra") || name.contains("side") || name.contains("combo")
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
    }

    fun toggleCategory(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            categoryRepository.toggleActive(id, isActive)
        }
    }

    fun toggleItem(id: Long, isAvailable: Boolean) {
        viewModelScope.launch {
            menuRepository.toggleItemAvailability(id, isAvailable)
        }
    }

    fun addCategory(name: String, isVeg: Boolean) {
        viewModelScope.launch {
            val isDuplicate = categories.value.any { it.name.equals(name, ignoreCase = true) }
            if (isDuplicate) return@launch
            
            categoryRepository.insertCategory(
                CategoryEntity(
                    name = name,
                    isVeg = isVeg,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun addItem(categoryId: Long, name: String, price: Double, foodType: String, stock: Double = 0.0, threshold: Double = 10.0) {
        viewModelScope.launch {
            menuRepository.insertItem(
                MenuItemEntity(
                    categoryId = categoryId,
                    name = name,
                    basePrice = price.toString(),
                    foodType = foodType,
                    currentStock = stock.toString(),
                    lowStockThreshold = threshold.toString(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateItem(item: MenuItemEntity) {
        viewModelScope.launch {
            menuRepository.updateItem(item)
        }
    }

    fun deleteItem(item: MenuItemEntity) {
        viewModelScope.launch {
            menuRepository.deleteItem(item)
        }
    }

    fun addVariant(menuItemId: Long, name: String, price: Double) {
        viewModelScope.launch {
            menuRepository.insertVariant(
                ItemVariantEntity(
                    menuItemId = menuItemId,
                    variantName = name,
                    price = price.toString(),
                    sortOrder = 0,
                    currentStock = "0.0",
                    lowStockThreshold = "10.0"
                )
            )
        }
    }

    fun updateVariant(variant: ItemVariantEntity) {
        viewModelScope.launch {
            menuRepository.updateVariant(variant)
        }
    }

    fun deleteVariant(variant: ItemVariantEntity) {
        viewModelScope.launch {
            menuRepository.deleteVariant(variant)
        }
    }

    data class DraftMenuItem(
        val name: String,
        val price: Double,
        val isSelected: Boolean = true,
        val foodType: String = "veg"   
    )

    data class OcrImportUiState(
        val configMode: String? = null, 
        val isProcessing: Boolean = false,
        val processingLabel: String = "Processing...",  
        val rawText: String = "",
        val drafts: List<DraftMenuItem> = emptyList(),
        val error: String? = null,
        val successMessage: String? = null  
    )

    private val _ocrImportUiState = MutableStateFlow(OcrImportUiState())
    val ocrImportUiState: StateFlow<OcrImportUiState> = _ocrImportUiState.asStateFlow()

    fun clearDrafts() {
        _ocrImportUiState.update { 
            it.copy(rawText = "", drafts = emptyList(), isProcessing = false, error = null) 
        }
    }

    fun setConfigMode(mode: String?) {
        _ocrImportUiState.update { it.copy(configMode = mode) }
    }

    fun setProcessing(isProcessing: Boolean) {
        _ocrImportUiState.update { it.copy(isProcessing = isProcessing, error = null) }
    }

    fun setError(error: String?) {
        _ocrImportUiState.update { it.copy(error = error, isProcessing = false) }
    }

    fun updateDraft(index: Int, updated: DraftMenuItem) {
        val current = _ocrImportUiState.value.drafts.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _ocrImportUiState.update { it.copy(drafts = current) }
        }
    }

    fun toggleDraftSelection(index: Int) {
        val current = _ocrImportUiState.value.drafts.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isSelected = !current[index].isSelected)
            _ocrImportUiState.update { it.copy(drafts = current) }
        }
    }

    
    fun extractTextFromPdf(context: Context, uri: Uri) {
        _ocrImportUiState.update { it.copy(isProcessing = true, processingLabel = "Reading PDF...", error = null) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.applicationContext)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open file. Check permissions.")
                inputStream.use { input ->
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(input)
                    val pageCount = document.numberOfPages
                    val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                    val sb = StringBuilder()
                    for (page in 1..pageCount) {
                        stripper.startPage = page
                        stripper.endPage = page
                        sb.append(stripper.getText(document))
                        sb.append("\n")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _ocrImportUiState.update { it.copy(
                                processingLabel = "Reading page $page of $pageCount..."
                            )}
                        }
                    }
                    document.close()
                    val fullText = sb.toString()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (fullText.isBlank()) {
                            _ocrImportUiState.update { it.copy(
                                isProcessing = false,
                                error = "PDF appears to be image-based. Try the camera scan instead."
                            )}
                        } else {
                            submitOcrText(fullText)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _ocrImportUiState.update { it.copy(
                        isProcessing = false,
                        error = "Failed to read PDF: ${e.message?.take(80)}"
                    )}
                }
            }
        }
    }

    
    fun submitOcrText(text: String) {
        _ocrImportUiState.update { 
            it.copy(
                rawText = text,
                drafts = parseDraftsFromText(text),
                isProcessing = false,
                error = null
            )
        }
    }

    
    fun processMenuImage(context: Context, bitmap: android.graphics.Bitmap) {
        _ocrImportUiState.update { it.copy(isProcessing = true, processingLabel = "Analysing image...", error = null) }
        
        val scaledBitmap = scaleBitmapIfNeeded(bitmap)
        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(scaledBitmap, 0)
        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
        )

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.isBlank()) {
                    _ocrImportUiState.update { it.copy(
                        isProcessing = false,
                        error = "No text found. Ensure the menu is well-lit and in focus."
                    )}
                } else {
                    submitOcrText(text)
                }
            }
            .addOnFailureListener { e ->
                _ocrImportUiState.update { it.copy(
                    isProcessing = false,
                    error = "Recognition failed. Try with better lighting or a clearer photo."
                )}
            }
            .addOnCompleteListener {
                recognizer.close()
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
            }
    }

    private fun scaleBitmapIfNeeded(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        val maxDim = 2048
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxOf(w, h)
        return android.graphics.Bitmap.createScaledBitmap(
            bitmap, (w * scale).toInt(), (h * scale).toInt(), true
        )
    }

    fun toggleDraftFoodType(index: Int) {
        val current = _ocrImportUiState.value.drafts.toMutableList()
        if (index in current.indices) {
            val item = current[index]
            current[index] = item.copy(foodType = if (item.foodType == "veg") "non-veg" else "veg")
            _ocrImportUiState.update { it.copy(drafts = current) }
        }
    }

    fun selectAllDrafts(select: Boolean) {
        _ocrImportUiState.update { state ->
            state.copy(drafts = state.drafts.map { it.copy(isSelected = select) })
        }
    }

    fun saveDraftsToCategory(categoryId: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val selectedDrafts = _ocrImportUiState.value.drafts.filter { it.isSelected }

            
            val existingItems = menuRepository.getItemsByCategoryFlow(categoryId).first()
            val existingNames = existingItems.map { it.name.lowercase() }.toHashSet()

            var addedCount = 0
            for (draft in selectedDrafts) {
                if (draft.name.lowercase() !in existingNames) {
                    menuRepository.insertItem(
                        MenuItemEntity(
                            categoryId = categoryId,
                            name = draft.name,
                            basePrice = draft.price.toString(),
                            foodType = draft.foodType,
                            currentStock = "0.0",
                            lowStockThreshold = "10.0",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    addedCount++
                }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _ocrImportUiState.update { it.copy(
                    drafts = emptyList(),
                    rawText = "",
                    isProcessing = false,
                    successMessage = "$addedCount item${if (addedCount == 1) "" else "s"} added to menu!"
                )}
            }
        }
    }

    fun clearSuccessMessage() {
        _ocrImportUiState.update { it.copy(successMessage = null) }
    }
}
