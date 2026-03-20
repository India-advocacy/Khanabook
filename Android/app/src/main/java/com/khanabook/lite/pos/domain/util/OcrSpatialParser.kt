package com.khanabook.lite.pos.domain.util

import android.util.Log
import com.google.mlkit.vision.text.Text
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel.DraftMenuItem
import com.khanabook.lite.pos.ui.viewmodel.MenuViewModel.DraftVariant
import java.util.regex.Pattern

object OcrSpatialParser {
    private const val TAG = "OCR_PARSER_DEBUG"

    private val priceRegex = Pattern.compile("""(?:[\u20B9\u20A8]|rs\.?|inr)?\s*((?:\d{1,3}(?:,\d{3})*|\d+)(?:\.\d{1,2})?)(?:\s*/-)?""", Pattern.CASE_INSENSITIVE)
    private val weightRegex = Regex("""(?i)\d+\s*(g|kg|ml|ltr|pcs|lb|oz)\b""")
    private const val Y_THRESHOLD_RATIO = 0.35 

    data class VariantHeader(val name: String, val xCenter: Int)
    data class PriceInfo(val value: Double, val xCenter: Int)

    fun parse(visionText: Text): List<DraftMenuItem> {
        val lines = visionText.textBlocks.flatMap { it.lines }
        if (lines.isEmpty()) return emptyList()

        Log.d(TAG, "parse start: lines=${lines.size} blocks=${visionText.textBlocks.size}")

        // 1. Sort lines by Y-coordinate
        val sortedLines = lines.sortedBy { it.boundingBox?.top ?: 0 }
        val rows = mutableListOf<MutableList<Text.Line>>()
        
        // 2. Group lines into rows based on vertical overlap
        for (line in sortedLines) {
            val bounds = line.boundingBox ?: continue
            val yCenter = bounds.centerY()
            val lineHeight = bounds.height()

            val existingRow = rows.find { row ->
                val rowBounds = row.first().boundingBox ?: return@find false
                
                // New logic: Check if boxes vertically overlap significantly
                val overlapTop = maxOf(bounds.top, rowBounds.top)
                val overlapBottom = minOf(bounds.bottom, rowBounds.bottom)
                val overlapHeight = overlapBottom - overlapTop
                
                if (overlapHeight > 0) {
                    val minHeight = minOf(bounds.height(), rowBounds.height())
                    // If they overlap by more than 50% of the smaller line's height, they are on the same row
                    overlapHeight > minHeight * 0.5
                } else {
                    // Fallback to center point if no direct overlap (less likely but possible)
                    val rowCenter = rowBounds.centerY()
                    Math.abs(yCenter - rowCenter) < (Math.max(lineHeight, rowBounds.height()) * Y_THRESHOLD_RATIO)
                }
            }

            if (existingRow != null) existingRow.add(line) else rows.add(mutableListOf(line))
        }

        val drafts = mutableListOf<DraftMenuItem>()
        var currentCategory: String? = null
        var currentHeaders = mutableListOf<VariantHeader>()

        val noiseKeywords = listOf("authentic", "experience", "since", "halal", "fine dining", "multi cuisine", "restaurant", "order online", "phone", "website", "www.", "special item", "menu", "thiruv", "address", "contact")
        val variantHeaderKeywords = listOf("full", "half", "qty", "price", "size", "large", "medium", "small", "regular", "portion", "plate")
        val itemKeywords = listOf("biriyani", "chicken", "murg", "mutton", "egg", "anda", "fish", "prawn", "meat", "seekh", "kebab", "tikka", "paneer", "mashroom", "veg", "aloo", "gobi", "dal", "roti", "naan", "paratha")

        val maxRowsToLog = 25
        var rowIdx = 0
        for (row in rows) {
            try {
                val sortedRow = row.sortedBy { it.boundingBox?.left ?: 0 }
                val rowText = sortedRow.joinToString(" ") { it.text }.trim()
                val lower = rowText.lowercase()

                val priceInfos = findPricesWithCoordinates(row)

                if (rowIdx < maxRowsToLog) {
                    Log.d(
                        TAG,
                        "row[$rowIdx] text='${rowText.take(180)}' currentCategory=$currentCategory priceInfos=${priceInfos.size} headers=${currentHeaders.size}"
                    )
                }
                
                if (priceInfos.isEmpty()) {
                    // Category/Header detection
                    if (variantHeaderKeywords.any { lower.contains(it) }) {
                        val newHeaders = mutableListOf<VariantHeader>()
                        sortedRow.forEach { line ->
                            val txt = line.text.trim().lowercase()
                            if (variantHeaderKeywords.any { txt.contains(it) } && txt.length >= 2) {
                                val cleanLabel = line.text.replace(Regex("""\s*\(.*?\)\s*"""), " ").trim()
                                val finalLabel = weightRegex.replace(cleanLabel, "").trim()
                                if (finalLabel.length >= 2) {
                                    newHeaders.add(VariantHeader(toTitleCase(finalLabel), line.boundingBox?.centerX() ?: 0))
                                }
                            }
                        }

                        if (newHeaders.isNotEmpty()) {
                            currentHeaders = newHeaders
                            // Also check for category on same line
                            val catPart = sortedRow.filter { line ->
                                val txt = line.text.lowercase()
                                variantHeaderKeywords.none { txt.contains(it) } && noiseKeywords.none { txt.contains(it) } && txt.length > 2
                            }.joinToString(" ") { it.text }.trim()
                            
                            if (catPart.isNotEmpty() && (catPart.uppercase() == catPart || !catPart.any { it.isDigit() })) {
                                currentCategory = toTitleCase(catPart)
                            }
                            if (rowIdx < maxRowsToLog) {
                                Log.d(
                                    TAG,
                                    "row[$rowIdx] header detected: headers=${currentHeaders.map { it.name }} categoryNow=$currentCategory"
                                )
                            }
                            continue
                        }
                    }

                    // Strict Category detection
                    val words = rowText.split(Regex("\\s+"))
                    if (words.size in 1..5 && noiseKeywords.none { lower.contains(it) }) {
                        val isItemLike = itemKeywords.any { lower.contains(it) }
                        if (rowText.length > 2 && (rowText == rowText.uppercase() || !rowText.any { it.isDigit() })) {
                            if (!isItemLike || currentCategory == null || rowText == rowText.uppercase()) {
                                currentCategory = toTitleCase(rowText)
                                val clearing = variantHeaderKeywords.none { lower.contains(it) }
                                if (clearing) currentHeaders.clear()
                                if (rowIdx < maxRowsToLog) {
                                    Log.d(
                                        TAG,
                                        "row[$rowIdx] category detected: categoryNow=$currentCategory clearedHeaders=$clearing"
                                    )
                                }
                            }
                        }
                    }
                    continue
                }

                // Item Name: Everything to the left of the FIRST price column
                val firstPriceX = priceInfos.minOf { it.xCenter }
                val nameElements = sortedRow.filter { (it.boundingBox?.centerX() ?: 0) < firstPriceX }
                var rawName = nameElements.joinToString(" ") { it.text }.trim()
                
                if (rawName.isEmpty()) {
                    // Fallback to text before first price in joined string
                    val firstPriceIdx = priceRegex.matcher(rowText).let { if (it.find()) it.start() else rowText.length }
                    rawName = rowText.substring(0, firstPriceIdx).trim()
                }

                val cleanName = rawName
                    .replace(Regex("""^\s*(?:[-*•]+|\d+[.):])\s*"""), "")
                    .replace(Regex("""[\s\-:|.…]+$"""), "")
                    .trim()
                
                if (cleanName.length < 2 || cleanName.lowercase() in listOf("item", "description", "price", "total")) continue

                // Correct common typos
                val correctedName = fixTypos(toTitleCase(cleanName))

                // Map prices to headers spatially
                val variants = priceInfos.map { price ->
                    val closestHeader = currentHeaders.minByOrNull { Math.abs(it.xCenter - price.xCenter) }
                    val vLabel = closestHeader?.name ?: if (priceInfos.size > 1) "Variant" else "Base"
                    DraftVariant(vLabel, price.value)
                }

                if (rowIdx < maxRowsToLog) {
                    Log.d(
                        TAG,
                        "row[$rowIdx] item: name='${correctedName.take(120)}' categoryNow=$currentCategory priceInfos=${priceInfos.size} headers=${currentHeaders.size} variantLabels=${variants.map { it.name }}"
                    )
                }

                drafts.add(
                    DraftMenuItem(
                        name = correctedName,
                        price = priceInfos.first().value,
                        variants = if (variants.size > 1) variants else emptyList(),
                        categoryName = currentCategory,
                        foodType = if (itemKeywords.filter { it != "veg" }.any { lower.contains(it) }) "non-veg" else "veg"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rowIdx++
        }

        Log.d(TAG, "parse end: drafts=${drafts.size} categories=${drafts.mapNotNull { it.categoryName }.distinct()}")
        return drafts
    }

    private fun findPricesWithCoordinates(row: List<Text.Line>): List<PriceInfo> {
        val prices = mutableListOf<PriceInfo>()
        for (line in row) {
            val txt = line.text.trim()
            // Skip weights/quantities like 500g
            if (weightRegex.containsMatchIn(txt.lowercase())) continue

            val matcher = priceRegex.matcher(txt)
            while (matcher.find()) {
                val valStr = matcher.group(1)?.replace(",", "")
                valStr?.toDoubleOrNull()?.let { 
                    if (it in 1.0..100000.0) {
                        prices.add(PriceInfo(it, line.boundingBox?.centerX() ?: 0))
                    }
                }
            }
        }
        return prices.sortedBy { it.xCenter }
    }

    private fun fixTypos(name: String): String {
        val corrections = mapOf(
            "Tildka" to "Tikka",
            "Tikla" to "Tikka",
            "Mashroom" to "Mushroom",
            "Paneer" to "Paneer",
            "Biryani" to "Biryani",
            "Biriyani" to "Biriyani"
        )
        var result = name
        corrections.forEach { (bad, good) ->
            result = result.replace(bad, good, ignoreCase = true)
        }
        return result
    }

    private fun toTitleCase(s: String): String {
        return s.lowercase().split(Regex("""\s+""")).joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}
