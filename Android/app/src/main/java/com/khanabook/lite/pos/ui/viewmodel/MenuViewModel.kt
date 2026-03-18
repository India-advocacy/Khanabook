package com.khanabook.lite.pos.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.khanabook.lite.pos.BuildConfig
import com.khanabook.lite.pos.data.local.entity.CategoryEntity
import com.khanabook.lite.pos.data.local.entity.ItemVariantEntity
import com.khanabook.lite.pos.data.local.entity.MenuItemEntity
import com.khanabook.lite.pos.data.local.relation.MenuWithVariants
import com.khanabook.lite.pos.data.remote.api.NvidiaChatRequest
import com.khanabook.lite.pos.data.remote.api.NvidiaMessage
import com.khanabook.lite.pos.data.remote.api.NvidiaNimApiService
import com.khanabook.lite.pos.data.repository.CategoryRepository
import com.khanabook.lite.pos.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val menuRepository: MenuRepository,
    private val nvidiaNimApiService: NvidiaNimApiService
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedCategoryId = MutableStateFlow<Long?>(null)

    val menuItems: StateFlow<List<MenuWithVariants>> = selectedCategoryId
        .flatMapLatest { id ->
            if (id != null) menuRepository.getMenuWithVariantsByCategoryFlow(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val searchQuery = MutableStateFlow("")
    val disabledItemsCount = MutableStateFlow(0)
    val menuAddOnsCount = MutableStateFlow(0)

    fun selectCategory(id: Long?) {
        selectedCategoryId.value = id
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun addCategory(name: String, isVeg: Boolean) {
        viewModelScope.launch {
            categoryRepository.insertCategory(CategoryEntity(name = name, isVeg = isVeg))
        }
    }

    fun toggleCategory(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            categoryRepository.toggleActive(id, enabled)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun addItem(categoryId: Long, name: String, price: Double, foodType: String) {
        viewModelScope.launch {
            menuRepository.insertItem(
                MenuItemEntity(
                    categoryId = categoryId,
                    name = name,
                    basePrice = price.toString(),
                    foodType = foodType,
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

    fun toggleItem(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            menuRepository.toggleItemAvailability(id, enabled)
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
                    price = price.toString()
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

    data class DraftVariant(
        val name: String,
        val price: Double
    )

    data class DraftMenuItem(
        val name: String,
        val price: Double,
        val variants: List<DraftVariant> = emptyList(),
        val isSelected: Boolean = true,
        val foodType: String = "veg",
        val categoryName: String? = null
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
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _ocrImportUiState.update { it.copy(
                                processingLabel = "Reading page $page of $pageCount..."
                            )}
                        }
                    }
                    document.close()
                    val fullText = sb.toString()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
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
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _ocrImportUiState.update { it.copy(
                        isProcessing = false,
                        error = "Failed to read PDF: ${e.message?.take(80)}"
                    )}
                }
            }
        }
    }

    fun submitOcrText(text: String) {
        viewModelScope.launch {
            _ocrImportUiState.update { it.copy(isProcessing = true, processingLabel = "Extracting menu items...", rawText = text) }
            
            val drafts = try {
                if (BuildConfig.NVIDIA_API_KEY.isNotBlank()) {
                    extractDraftsWithNvidia(text) ?: parseDraftsFromText(text)
                } else {
                    parseDraftsFromText(text)
                }
            } catch (e: Exception) {
                parseDraftsFromText(text)
            }
            
            _ocrImportUiState.update { 
                it.copy(
                    drafts = drafts,
                    isProcessing = false,
                    error = if (drafts.isEmpty()) "No items extracted. Please try again." else null
                )
            }
        }
    }

    private suspend fun extractDraftsWithNvidia(text: String): List<DraftMenuItem>? {
        return try {
            val prompt = """
                Extract menu items from the following OCR text with 100% accuracy on NAMES and PRICES.
                Return ONLY a valid JSON array of objects.
                If an item has multiple prices (e.g., Full/Half), use the "variants" array.
                Ignore headers, page numbers, and descriptions.
                
                Each object must have:
                - "name": String
                - "price": Double (The primary or first price found)
                - "variants": Array of { "name": String, "price": Double } (Optional)
                
                Text to process:
                ${text.take(3000)}
            """.trimIndent()

            val request = NvidiaChatRequest(
                messages = listOf(
                    NvidiaMessage(role = "system", content = "You are a precise menu data extractor. You output ONLY valid JSON. No conversational text."),
                    NvidiaMessage(role = "user", content = prompt)
                )
            )

            val response = nvidiaNimApiService.getChatCompletions(
                authorization = "Bearer ${BuildConfig.NVIDIA_API_KEY}",
                request = request
            )

            val jsonContent = response.choices?.firstOrNull()?.message?.content?.let { content ->
                val start = content.indexOf("[")
                val end = content.lastIndexOf("]")
                if (start != -1 && end != -1) content.substring(start, end + 1) else null
            }

            if (jsonContent != null) {
                val itemType = object : TypeToken<List<DraftMenuItem>>() {}.type
                Gson().fromJson<List<DraftMenuItem>>(jsonContent, itemType)
            } else null
        } catch (e: Exception) {
            Log.e("MenuViewModel", "NVIDIA NIM Extraction failed", e)
            null
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
            .addOnFailureListener {
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

    fun saveDraftsToCategory(categoryId: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val selectedDrafts = _ocrImportUiState.value.drafts.filter { it.isSelected }

            val existingItems = menuRepository.getItemsByCategoryFlow(categoryId).first()
            val existingNames = existingItems.map { it.name.lowercase() }.toHashSet()

            var addedCount = 0
            for (draft in selectedDrafts) {
                if (draft.name.lowercase() !in existingNames) {
                    val itemId = menuRepository.insertItem(
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
                    
                    if (draft.variants.isNotEmpty()) {
                        draft.variants.forEach { variant ->
                            menuRepository.insertVariant(
                                ItemVariantEntity(
                                    menuItemId = itemId,
                                    variantName = variant.name,
                                    price = variant.price.toString(),
                                    sortOrder = 0,
                                    currentStock = "0.0",
                                    lowStockThreshold = "10.0"
                                )
                            )
                        }
                    }
                    
                    addedCount++
                }
            }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
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

    companion object {
        private val genericPriceRegex =
            Regex("""(?:[\u20B9\u20A8]|rs\.?|inr)?\s*((?:\d{1,3}(?:,\d{3})*|\d+)(?:\.\d{1,2})?)\b""", RegexOption.IGNORE_CASE)
            
        private val headerLineRegex = Regex("""(?i)^(full|half|qty|price|s\.\s*no|veg|non.?veg).*$""")
        private val allCapsHeaderRegex = Regex("""^[A-Z\s\-&]{3,}$""")
        
        private val leadingBulletRegex = Regex("""^\s*(?:[-*•]+|\d+[.):]?)\s*""")
        private val trailingSeparatorRegex = Regex("""[\s\-:|.…]+$""")
        private val trailingCurrencyRegex = Regex("""(?i)(?:[\u20B9\u20A8]|rs\.?|inr)\s*$""")
        
        private val skipLineRegex = Regex("""(?i)^(menu|category|item|price|qty|total|subtotal|s\.no|veg|non.?veg|page\s+\d+)\.?\s*$""")
        
        private const val MAX_PRICE = 99999.0

        internal fun parseDraftsFromText(text: String): List<DraftMenuItem> {
            val seen = mutableSetOf<String>()
            val drafts = mutableListOf<DraftMenuItem>()
            var currentCategory: String? = null
            var currentVariantHeaders = listOf<String>()

            text.lineSequence()
                .map { normalizeImportLine(it) }
                .filter { it.isNotBlank() }
                .forEach { line ->
                    val lower = line.lowercase()
                    if (lower.contains("full") || lower.contains("half") || lower.contains("qty") || lower.contains("price")) {
                        val headers = line.split(Regex("""[\s\-:|.]+""")).filter { it.length > 2 }
                        val hasKeywords = headers.any { h -> h.lowercase() in listOf("full", "half", "qty", "price") }
                        if (hasKeywords) {
                            currentVariantHeaders = headers.map { h -> toTitleCase(h) }
                            return@forEach
                        }
                    }

                    if (allCapsHeaderRegex.matches(line) && !line.any { it.isDigit() }) {
                        if (!skipLineRegex.matches(line)) {
                            currentCategory = toTitleCase(line)
                        }
                        return@forEach
                    }

                    val draft = parseDraftLine(line, currentVariantHeaders)
                    if (draft != null) {
                        val finalDraft = draft.copy(categoryName = currentCategory)
                        if (seen.add(finalDraft.name.lowercase())) {
                            drafts.add(finalDraft)
                        }
                    }
                }
            return drafts
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

        private fun parseDraftLine(line: String, variantHeaders: List<String>): DraftMenuItem? {
            val noBullet = line.replace(leadingBulletRegex, "").trim()
            if (noBullet.isBlank()) return null

            val priceMatches = genericPriceRegex.findAll(noBullet).toList()
            if (priceMatches.isEmpty()) return null // Require a price for local extraction

            val firstNamePos = priceMatches.first().range.first
            val lastNamePos = priceMatches.last().range.last
            
            val priceValues = priceMatches.mapNotNull { 
                it.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() 
            }.filter { it <= MAX_PRICE }
            
            if (priceValues.isEmpty()) return null

            val rawName = if (firstNamePos > noBullet.length / 3) {
                noBullet.substring(0, firstNamePos)
            } else {
                noBullet.substring(lastNamePos + 1)
            }
            .replace(trailingCurrencyRegex, "")
            .replace(trailingSeparatorRegex, "")
            .replace(Regex("""^\s*[-:|.…]+"""), "") 
            .trim()

            val name = toTitleCase(rawName)
            if (name.isBlank() || name.length < 2) return null

            val variants = priceValues.mapIndexed { index, price ->
                val vName = variantHeaders.getOrNull(index) ?: if (priceValues.size > 1) "Variant ${index + 1}" else "Base"
                DraftVariant(vName, price)
            }

            return DraftMenuItem(
                name = name,
                price = priceValues.first(),
                variants = variants
            )
        }

        private fun toTitleCase(s: String): String {
            return s.lowercase()
                .split(Regex("""\s+"""))
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        }
    }
}
