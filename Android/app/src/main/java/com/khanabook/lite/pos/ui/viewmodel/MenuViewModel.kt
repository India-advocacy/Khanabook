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
            Regex("""(?i)(?:\u20B9|rs\.?|inr)?\s*(\d+(?:\.\d{1,2})?)\s*$""")
        private val leadingBulletRegex = Regex("""^\s*(?:[-*]+|\d+[.)])\s*""")
        private val trailingSeparatorRegex = Regex("""[\s\-:|]+$""")
        private val trailingCurrencyRegex = Regex("""(?i)(?:\u20B9|rs\.?|inr)\s*$""")

        internal fun parseDraftsFromText(text: String): List<DraftMenuItem> {
            return text
                .lineSequence()
                .map { normalizeImportLine(it) }
                .filter { it.length > 2 && it.any(Char::isLetter) }
                .mapNotNull { parseDraftLine(it) }
                .toList()
        }

        private fun normalizeImportLine(raw: String): String {
            return raw
                .replace("\u20B9", " Rs ")
                .replace("â‚¹", " Rs ")
                .replace('\u00A0', ' ')
                .replace(Regex("""\s+"""), " ")
                .trim()
        }

        private fun parseDraftLine(line: String): DraftMenuItem? {
            val cleanedLine = line.replace(leadingBulletRegex, "")
            val priceMatch = trailingPriceRegex.find(cleanedLine)
            val name = if (priceMatch != null) {
                cleanedLine
                    .substring(0, priceMatch.range.first)
                    .replace(trailingCurrencyRegex, "")
                    .replace(trailingSeparatorRegex, "")
                    .trim()
            } else {
                cleanedLine
            }

            if (name.isBlank()) {
                return null
            }

            val normalizedName = name
                .lowercase()
                .split(Regex("""\s+"""))
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            val price = priceMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            return DraftMenuItem(normalizedName, price)
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

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId

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

    fun selectCategory(id: Int) {
        _selectedCategoryId.value = id
    }

    fun toggleCategory(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            categoryRepository.toggleActive(id, isActive)
        }
    }

    fun toggleItem(id: Int, isAvailable: Boolean) {
        viewModelScope.launch {
            menuRepository.toggleItemAvailability(id, isAvailable)
        }
    }

    fun addCategory(name: String, isVeg: Boolean) {
        viewModelScope.launch {
            val isDuplicate = categories.value.any { it.name.equals(name, ignoreCase = true) }
            if (isDuplicate) return@launch
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            categoryRepository.insertCategory(
                CategoryEntity(
                    name = name,
                    isVeg = isVeg,
                    createdAt = sdf.format(Date())
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

    fun addItem(categoryId: Int, name: String, price: Double, foodType: String, stock: Double = 0.0, threshold: Double = 10.0) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            menuRepository.insertItem(
                MenuItemEntity(
                    categoryId = categoryId,
                    name = name,
                    basePrice = price,
                    foodType = foodType,
                    currentStock = stock,
                    lowStockThreshold = threshold,
                    createdAt = sdf.format(Date())
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

    fun addVariant(menuItemId: Int, name: String, price: Double) {
        viewModelScope.launch {
            menuRepository.insertVariant(
                ItemVariantEntity(
                    menuItemId = menuItemId,
                    variantName = name,
                    price = price,
                    sortOrder = 0,
                    currentStock = 0.0,
                    lowStockThreshold = 10.0
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
        val isSelected: Boolean = true
    )

    private val _scannedDrafts = MutableStateFlow<List<DraftMenuItem>>(emptyList())
    val scannedDrafts: StateFlow<List<DraftMenuItem>> = _scannedDrafts

    fun clearDrafts() {
        _scannedDrafts.value = emptyList()
    }

    fun updateDraft(index: Int, updated: DraftMenuItem) {
        val current = _scannedDrafts.value.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _scannedDrafts.value = current
        }
    }

    fun toggleDraftSelection(index: Int) {
        val current = _scannedDrafts.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isSelected = !current[index].isSelected)
            _scannedDrafts.value = current
        }
    }

    /**
     * Extracts text from a PDF URI and parses it into drafts.
     */
    fun extractTextFromPdf(context: Context, uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.applicationContext)
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use { input ->
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(input)
                    val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                    val text = stripper.getText(document)
                    document.close()
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        parseScannedTextToDrafts(text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed to read PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Parses raw OCR text into Draft items for review.
     */
    fun parseScannedTextToDrafts(text: String) {
        _scannedDrafts.value = parseDraftsFromText(text)
    }

    fun saveDraftsToCategory(categoryId: Int) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val selectedDrafts = _scannedDrafts.value.filter { it.isSelected }
            
            for (draft in selectedDrafts) {
                val existing = menuRepository.getItemsByCategoryFlow(categoryId).first().any { 
                    it.name.equals(draft.name, ignoreCase = true) 
                }
                
                if (!existing) {
                    menuRepository.insertItem(
                        MenuItemEntity(
                            categoryId = categoryId,
                            name = draft.name,
                            basePrice = draft.price,
                            foodType = "veg",
                            currentStock = 0.0,
                            lowStockThreshold = 10.0,
                            createdAt = sdf.format(Date())
                        )
                    )
                }
            }
            clearDrafts()
        }
    }
}
