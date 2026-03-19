package com.khanabook.lite.pos.domain.util

import com.google.mlkit.vision.text.Text
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel.DraftMenuItem
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel.DraftVariant
import java.util.regex.Pattern

object OcrSpatialParser {

    private val priceRegex = Pattern.compile("""(?:[\u20B9\u20A8]|rs\.?|inr)?\s*((?:\d{1,3}(?:,\d{3})*|\d+)(?:\.\d{1,2})?)\b""", Pattern.CASE_INSENSITIVE)
    private const val Y_THRESHOLD_RATIO = 0.5 // Threshold for grouping lines on the same row

    fun parse(visionText: Text): List<DraftMenuItem> {
        val lines = visionText.textBlocks.flatMap { it.lines }
        if (lines.isEmpty()) return emptyList()

        // 1. Sort lines by Y-coordinate
        val sortedLines = lines.sortedBy { it.boundingBox?.top ?: 0 }

        val rows = mutableListOf<MutableList<Text.Line>>()
        
        // 2. Group lines into rows based on Y-coordinate alignment
        for (line in sortedLines) {
            val bounds = line.boundingBox ?: continue
            val lineHeight = bounds.height()
            val yCenter = bounds.centerY()

            val existingRow = rows.find { row ->
                val rowCenter = row.first().boundingBox?.centerY() ?: 0
                val rowHeight = row.first().boundingBox?.height() ?: 0
                Math.abs(yCenter - rowCenter) < (Math.max(lineHeight, rowHeight) * Y_THRESHOLD_RATIO)
            }

            if (existingRow != null) {
                existingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        val drafts = mutableListOf<DraftMenuItem>()
        var currentCategory: String? = null
        var currentVariantHeaders = listOf<String>()

        val noiseKeywords = listOf("authentic", "experience", "since", "halal", "fine dining", "multi cuisine", "restaurant", "order online", "phone", "website", "www.", "special item", "menu")

        // 3. Process each row to find Category, Item Name, and Price
        for (row in rows) {
            try {
                // Sort line elements within the row by X-coordinate
                val sortedRow = row.sortedBy { it.boundingBox?.left ?: 0 }
                val rowText = sortedRow.joinToString(" ") { it.text }.trim()
                val lower = rowText.lowercase()

                val prices = findPrices(rowText)
                
                if (prices.isEmpty()) {
                    // Category/Header detection
                    val words = rowText.split(Regex("\\s+"))
                    
                    // A category should be short, not a sentence, and not marketing noise
                    if (words.size in 1..5 && noiseKeywords.none { lower.contains(it) }) {
                        
                        // Variant header detection: Contains keywords like Full, Half, Qty, Price
                        if (lower.contains("full") || lower.contains("half") || lower.contains("qty") || lower.contains("price")) {
                            val potentialHeaders = sortedRow.map { it.text.trim() }
                                .filter { it.length > 2 && it.lowercase() !in listOf("item", "description") }
                            
                            if (potentialHeaders.any { it.lowercase() in listOf("full", "half", "qty", "price") }) {
                                currentVariantHeaders = potentialHeaders.map { toTitleCase(it) }
                                continue 
                            }
                        }

                        // If it's all caps or has no digits, it's likely a category
                        val upper = rowText.uppercase()
                        if (rowText.length > 2 && (rowText == upper || !rowText.any { it.isDigit() })) {
                            currentCategory = toTitleCase(rowText)
                        }
                    }
                    continue
                }

                // The item name is usually everything before the first price
                val firstPriceMatch = priceRegex.matcher(rowText)
                val firstPriceIndex = if (firstPriceMatch.find()) firstPriceMatch.start() else rowText.length
                val firstPriceStr = rowText.substring(0, firstPriceIndex).trim()
                
                // Refine name: remove leading/trailing noise
                val cleanName = firstPriceStr
                    .replace(Regex("""^\s*(?:[-*•]+|\d+[.):])\s*"""), "") // Remove bullets/numbers
                    .replace(Regex("""[\s\-:|.…]+$"""), "") // Remove trailing separators
                    .trim()
                
                if (cleanName.length < 2 || cleanName.lowercase() in listOf("item", "description", "price")) continue

                // Automatic Non-Veg detection
                val nonVegKeywords = listOf("chicken", "murg", "mutton", "egg", "anda", "fish", "prawn", "meat", "non-veg", "non veg", "seekh", "kebab", "tikka", "biriyani")
                val isNonVeg = nonVegKeywords.any { lower.contains(it) }

                val variants = prices.mapIndexed { index, price ->
                    var vName = currentVariantHeaders.getOrNull(index) ?: if (prices.size > 1) "Variant ${index + 1}" else "Base"
                    if (vName.lowercase() == "price" && prices.size > 1) {
                        vName = "Variant ${index + 1}"
                    }
                    DraftVariant(vName, price)
                }

                drafts.add(
                    DraftMenuItem(
                        name = toTitleCase(cleanName),
                        price = prices.first(),
                        variants = if (variants.size > 1) variants else emptyList(),
                        categoryName = currentCategory,
                        foodType = if (isNonVeg) "non-veg" else "veg"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Skip problematic rows instead of crashing
            }
        }

        return drafts
    }

    private fun findPrices(text: String): List<Double> {
        val matcher = priceRegex.matcher(text)
        val prices = mutableListOf<Double>()
        while (matcher.find()) {
            val priceStr = matcher.group(1)?.replace(",", "")
            priceStr?.toDoubleOrNull()?.let { 
                if (it < 100000) prices.add(it) 
            }
        }
        return prices
    }

    private fun toTitleCase(s: String): String {
        return s.lowercase().split(Regex("""\s+""")).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
    }
}
