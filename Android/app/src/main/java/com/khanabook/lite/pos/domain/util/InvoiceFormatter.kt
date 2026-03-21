package com.khanabook.lite.pos.domain.util

import android.util.Log
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceFormatter {

    private const val TAG = "InvoiceFormatter"

    
    private val ESC: Byte = 0x1B
    private val GS: Byte  = 0x1D
    private val RESET      = byteArrayOf(ESC, 0x40) 
    private val BOLD_ON    = byteArrayOf(ESC, 0x45, 0x01)
    private val BOLD_OFF   = byteArrayOf(ESC, 0x45, 0x00)
    private val ALIGN_LEFT  = byteArrayOf(ESC, 0x61, 0x00)
    private val ALIGN_CENTER= byteArrayOf(ESC, 0x61, 0x01)
    private val LARGE_FONT = byteArrayOf(GS, 0x21, 0x11) 
    private val NORMAL_FONT= byteArrayOf(GS, 0x21, 0x00)
    private val CUT_PAPER  = byteArrayOf(GS, 0x56, 0x42, 0x00) 

    private fun resolveCurrency(profile: RestaurantProfileEntity?): String {
        return if (profile?.currency == "INR" || profile?.currency == "Rupee") "Rs." else profile?.currency ?: ""
    }

    private fun formatMoney(amount: String): String {
        return try {
            BigDecimal(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
        } catch (e: Exception) {
            "0.00"
        }
    }

    fun formatForThermalPrinter(bill: BillWithItems, profile: RestaurantProfileEntity?): ByteArray {
        val charsPerLine = if (profile?.paperSize == "80mm") 42 else 32
        val currency = resolveCurrency(profile)
        val isGst = profile?.gstEnabled == true
        
        val width = charsPerLine
        val line = "-".repeat(width)
        val doubleLine = "=".repeat(width)

        val out = mutableListOf<Byte>()

        fun add(bytes: ByteArray) { out.addAll(bytes.toList()) }
        fun add(text: String) { 
            
            out.addAll(text.toByteArray(Charsets.UTF_8).toList())
        }

        add(RESET)
        add(ALIGN_CENTER)
        
        
        if (profile?.includeLogoInPrint == true && !profile.logoPath.isNullOrBlank()) {
            try {
                val bitmap = BitmapFactory.decodeFile(profile.logoPath)
                if (bitmap != null) {
                    try {
                        add(decodeBitmapToESC_POS(bitmap, 384))
                        add("\n")
                    } finally {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing logo", e)
            }
        }

        
        add(LARGE_FONT)
        add(BOLD_ON)
        add(profile?.shopName?.uppercase() ?: "RESTAURANT")
        add("\n")
        add(BOLD_OFF)
        add(NORMAL_FONT)
        
        
        profile?.shopAddress?.takeIf { it.isNotBlank() }?.let { add(it + "\n") }
        if (!profile?.whatsappNumber.isNullOrBlank()) add("Contact: ${profile?.whatsappNumber}\n")
        if (!profile?.fssaiNumber.isNullOrBlank()) add("FSSAI No: ${profile?.fssaiNumber}\n")
        if (isGst && !profile?.gstin.isNullOrBlank()) add("GSTIN: ${profile?.gstin}\n")
        
        add(ALIGN_LEFT)
        add("$doubleLine\n")
        add(centerText(if (isGst) "TAX INVOICE" else "INVOICE", width) + "\n")
        add("$line\n")
        
        add("Bill : ${bill.bill.lifetimeOrderId}\n")
        val dateStr = com.khanabook.lite.pos.domain.util.DateUtils.formatDateOnly(bill.bill.createdAt)
        add("Date: $dateStr\n")
        bill.bill.customerName?.takeIf { it.isNotBlank() }?.let { add("Cust : $it\n") }
        bill.bill.customerWhatsapp?.takeIf { it.isNotBlank() }?.let { add("WA   : $it\n") }
        add("$line\n")
        
        
        val itemW = (width * 0.45).toInt()
        val qtyW = 4
        val rateW = (width * 0.2).toInt()
        val amtW = maxOf(4, width - itemW - qtyW - rateW - 3)
        
        val headerFormat = "%-${itemW}s %${qtyW}s %${rateW}s %${amtW}s\n"
        add(String.format(headerFormat, "ITEM", "QTY", "RATE", "AMT"))
        add("$line\n")
        
        for (item in bill.items) {
            val name = if (item.variantName != null) "${item.itemName} (${item.variantName})" else item.itemName
            add(String.format(headerFormat, name.take(itemW), item.quantity, formatMoney(item.price), formatMoney(item.itemTotal)))
        }
        
        add("$line\n")
        
        
        add(formatRow("Subtotal:", "$currency ${formatMoney(bill.bill.subtotal)}", width))
        
        if (isGst && (BigDecimal(bill.bill.cgstAmount).compareTo(BigDecimal.ZERO) > 0)) {
            
            val halfGst = BigDecimal(bill.bill.gstPercentage).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
            add(formatRow("CGST (${halfGst.stripTrailingZeros().toPlainString()}%):", "$currency ${formatMoney(bill.bill.cgstAmount)}", width))
            add(formatRow("SGST (${halfGst.stripTrailingZeros().toPlainString()}%):", "$currency ${formatMoney(bill.bill.sgstAmount)}", width))
        }
        
        if (!isGst && (BigDecimal(bill.bill.customTaxAmount).compareTo(BigDecimal.ZERO) > 0)) {
            val taxLabel = profile?.customTaxName?.takeIf { it.isNotBlank() } ?: "Tax"
            add(formatRow("$taxLabel:", "$currency ${formatMoney(bill.bill.customTaxAmount)}", width))
        }
        
        add("$line\n")
        add(BOLD_ON)
        add(formatRow("TOTAL AMOUNT:", "$currency ${formatMoney(bill.bill.totalAmount)}", width))
        add(BOLD_OFF)
        add("$line\n")
        
        add("Payment Mode : ${bill.bill.paymentMode.uppercase()}\n")
        
        add("$doubleLine\n")
        add(ALIGN_CENTER)
        add("Thank you! Visit again.\n")
        add("$doubleLine\n")
        if (profile?.showBranding != false) {
            add("Powered by KhanaBook\n")
        }
        
        
        add("\n\n\n\n")
        add(CUT_PAPER)

        return out.toByteArray()
    }

    fun formatForWhatsApp(bill: BillWithItems, profile: RestaurantProfileEntity?): String {
        val sb = StringBuilder()
        val width = if (profile?.paperSize == "80mm") 42 else 32
        val line = "-".repeat(width)
        
        val currency = if (profile?.currency == "INR" || profile?.currency == "Rupee") "₹" else profile?.currency ?: ""
        
        sb.append("🏛️ *${profile?.shopName?.uppercase() ?: "RESTAURANT"}*\n")
        if (!profile?.shopAddress.isNullOrBlank()) sb.append("📍 ${profile?.shopAddress}\n")
        if (!profile?.whatsappNumber.isNullOrBlank()) sb.append("📞 Contact: ${profile?.whatsappNumber}\n")
        
        val title = if (profile?.gstEnabled == true) "TAX INVOICE" else "INVOICE"
        sb.append("\n🧾 *--- $title ---*\n")
        
        sb.append("🔢 *Bill #:* ${bill.bill.lifetimeOrderId}\n")
        val formattedDate = com.khanabook.lite.pos.domain.util.DateUtils.formatDisplay(bill.bill.createdAt)
        sb.append("📅 *Date:* ${formattedDate}\n")
        
        sb.append("👤 *Customer:* ${bill.bill.customerName?.takeIf { it.isNotBlank() } ?: "Walking Customer"}\n")
        
        sb.append("\n📦 *ORDER SUMMARY*\n")
        sb.append("$line\n")
        for (item in bill.items) {
            val name = if (item.variantName != null) "${item.itemName} (${item.variantName})" else item.itemName
            sb.append("🔹 *${name.uppercase()}*\n")
            sb.append("   ${item.quantity} x $currency${formatMoney(item.price)} = $currency${formatMoney(item.itemTotal)}\n")
        }
        sb.append("$line\n")
        
        sb.append("💵 *Subtotal: $currency${formatMoney(bill.bill.subtotal)}*\n")
        
        if (profile?.gstEnabled == true) {
            val halfGst = try {
                BigDecimal(bill.bill.gstPercentage)
                    .divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString()
            } catch (e: Exception) { "0" }

            val cgst = try {
                BigDecimal(bill.bill.cgstAmount)
                    .setScale(2, RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }

            val sgst = try {
                BigDecimal(bill.bill.sgstAmount)
                    .setScale(2, RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }

            sb.append("   CGST ($halfGst%): $currency$cgst\n")
            sb.append("   SGST ($halfGst%): $currency$sgst\n")
        }

        if (profile?.gstEnabled == false) {
            val customAmt = try {
                BigDecimal(bill.bill.customTaxAmount)
            } catch (e: Exception) { BigDecimal.ZERO }

            if (customAmt.compareTo(BigDecimal.ZERO) > 0) {
                val taxLabel = profile.customTaxName?.takeIf { it.isNotBlank() } ?: "Tax"
                sb.append("   $taxLabel: $currency${formatMoney(bill.bill.customTaxAmount)}\n")
            }
        }
        
        sb.append("\n💰 *TOTAL AMOUNT: $currency${formatMoney(bill.bill.totalAmount)}*\n")
        sb.append("$line\n")
        sb.append("💳 *Payment:* ${
            com.khanabook.lite.pos.domain.model.PaymentMode
                .fromDbValue(bill.bill.paymentMode).displayLabel
        }\n")
        sb.append("\nThank you! Visit Again 🙏\n")
        if (profile?.showBranding != false) {
            sb.append("_Software by KhanaBook_")
        }
        
        return sb.toString()
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) {
            Log.w(TAG, "Text '$text' is longer than width $width and will be truncated.")
            return text.take(width)
        }
        val padding = maxOf(0, (width - text.length) / 2)
        return " ".repeat(padding) + text
    }

    private fun formatRow(label: String, value: String, width: Int): String {
        val spaceCount = width - label.length - value.length
        return if (spaceCount > 0) label + " ".repeat(spaceCount) + value + "\n"
        else "$label\n${" ".repeat(maxOf(0, width - value.length))}$value\n"
    }

    
    private fun decodeBitmapToESC_POS(bitmap: Bitmap, maxWidth: Int): ByteArray {
        var scaledBitmap: Bitmap? = null
        try {
            
            val scale = maxWidth.toFloat() / bitmap.width.toFloat()
            val targetHeight = (bitmap.height * scale).toInt()
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)

            val width = scaledBitmap!!.width
            val height = scaledBitmap.height
            val bytesWidth = (width + 7) / 8
            
            val data = mutableListOf<Byte>()
            
            
            data.add(0x1D.toByte())
            data.add(0x76.toByte())
            data.add(0x30.toByte())
            data.add(0x00.toByte())
            
            data.add((bytesWidth % 256).toByte()) 
            data.add((bytesWidth / 256).toByte()) 
            data.add((height % 256).toByte())    
            data.add((height / 256).toByte())    

            for (y in 0 until height) {
                for (x in 0 until bytesWidth) {
                    var bite = 0
                    for (b in 0 until 8) {
                        val pixelX = x * 8 + b
                        if (pixelX < width) {
                            val pixel = scaledBitmap.getPixel(pixelX, y)
                            val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                            if (gray < 128) { 
                                bite = bite or (0x80 shr b)
                            }
                        }
                    }
                    data.add(bite.toByte())
                }
            }
            return data.toByteArray()
        } finally {
            scaledBitmap?.recycle()
        }
    }
}


