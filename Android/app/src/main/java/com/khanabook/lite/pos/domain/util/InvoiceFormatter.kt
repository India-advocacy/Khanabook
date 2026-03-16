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

    // —— ESC/POS Thermal Printer Commands ——————————————————————————————————————
    private val ESC: Byte = 0x1B
    private val GS: Byte  = 0x1D
    private val RESET      = byteArrayOf(ESC, 0x40) // Initialize
    private val BOLD_ON    = byteArrayOf(ESC, 0x45, 0x01)
    private val BOLD_OFF   = byteArrayOf(ESC, 0x45, 0x00)
    private val ALIGN_LEFT  = byteArrayOf(ESC, 0x61, 0x00)
    private val ALIGN_CENTER= byteArrayOf(ESC, 0x61, 0x01)
    private val LARGE_FONT = byteArrayOf(GS, 0x21, 0x11) // 2x height, 2x width
    private val NORMAL_FONT= byteArrayOf(GS, 0x21, 0x00)
    private val CUT_PAPER  = byteArrayOf(GS, 0x56, 0x42, 0x00) // Cut command

    private fun resolveCurrency(profile: RestaurantProfileEntity?): String {
        return if (profile?.currency == "INR" || profile?.currency == "Rupee") "Rs." else profile?.currency ?: ""
    }

    private fun formatMoney(amount: Double): String {
        return try {
            BigDecimal(amount.toString())
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
        } catch (e: Exception) {
            String.format("%.2f", amount)
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
            // ISO-8859-1 provides better coverage for common symbols than US_ASCII
            out.addAll(text.toByteArray(Charsets.ISO_8859_1).toList())
        }

        add(RESET)
        add(ALIGN_CENTER)
        
        // 0. Shop Logo
        if (profile?.includeLogoInPrint == true && !profile.logoPath.isNullOrBlank()) {
            try {
                val bitmap = BitmapFactory.decodeFile(profile.logoPath)
                if (bitmap != null) {
                    add(decodeBitmapToESC_POS(bitmap, 384))
                    add("\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing logo", e)
            }
        }

        // Shop Name Large Bold
        add(LARGE_FONT)
        add(BOLD_ON)
        add(profile?.shopName?.uppercase() ?: "RESTAURANT")
        add("\n")
        add(BOLD_OFF)
        add(NORMAL_FONT)
        
        // Address & Info
        profile?.shopAddress?.takeIf { it.isNotBlank() }?.let { add(it + "\n") }
        if (!profile?.whatsappNumber.isNullOrBlank()) add("Contact: ${profile?.whatsappNumber}\n")
        if (!profile?.fssaiNumber.isNullOrBlank()) add("FSSAI No: ${profile?.fssaiNumber}\n")
        if (isGst && !profile?.gstin.isNullOrBlank()) add("GSTIN: ${profile?.gstin}\n")
        
        add(ALIGN_LEFT)
        add("$doubleLine\n")
        add(centerText(if (isGst) "TAX INVOICE" else "INVOICE", width) + "\n")
        add("$line\n")
        
        add("Bill : ${bill.bill.dailyOrderId.toString().padStart(3, '0')}\n")
        val dateObj = DateUtils.parseDb(bill.bill.createdAt) ?: run {
            Log.w(TAG, "Malformed created_at timestamp: ${bill.bill.createdAt}")
            Date()
        }
        val formattedDate = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(dateObj)
        add("Date : $formattedDate\n")
        bill.bill.customerName?.takeIf { it.isNotBlank() }?.let { add("Cust : $it\n") }
        bill.bill.customerWhatsapp?.takeIf { it.isNotBlank() }?.let { add("WA   : $it\n") }
        add("$line\n")
        
        // Dynamic Column Widths
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
        
        // Summary
        add(formatRow("Subtotal:", "$currency ${formatMoney(bill.bill.subtotal)}", width))
        
        if (isGst && bill.bill.cgstAmount > 0) {
            // TODO: Handle IGST for inter-state supplies
            val halfGst = bill.bill.gstPercentage / 2
            add(formatRow("CGST (%.1f%%):".format(halfGst), "$currency ${formatMoney(bill.bill.cgstAmount)}", width))
            add(formatRow("SGST (%.1f%%):".format(halfGst), "$currency ${formatMoney(bill.bill.sgstAmount)}", width))
        }
        
        if (!isGst && bill.bill.customTaxAmount > 0) {
            add(formatRow("Tax:", "$currency ${formatMoney(bill.bill.customTaxAmount)}", width))
        }
        
        add("$line\n")
        add(BOLD_ON)
        add(formatRow("TOTAL AMOUNT:", "$currency ${formatMoney(bill.bill.totalAmount)}", width))
        add(BOLD_OFF)
        add("$line\n")
        
        add("Payment Mode : ${bill.bill.paymentMode.uppercase()}\n")
        
        add("$doubleLine\n")
        add(ALIGN_CENTER)
        add("Thank you for visiting us!\n")
        add("Have a great day!\n")
        add("$doubleLine\n")
        add("Powered by KhanaBook POS\n")
        
        // Feed & Cut
        add("\n\n\n\n")
        add(CUT_PAPER)

        return out.toByteArray()
    }

    fun formatForWhatsApp(bill: BillWithItems, profile: RestaurantProfileEntity?): String {
        val sb = StringBuilder()
        // Use proper Rupee symbol for digital WhatsApp message
        val currency = if (profile?.currency == "INR" || profile?.currency == "Rupee") "₹" else profile?.currency ?: ""
        
        // Header
        sb.append("*${profile?.shopName?.uppercase() ?: "RESTAURANT"}*\n")
        if (!profile?.shopAddress.isNullOrBlank()) sb.append("${profile?.shopAddress}\n")
        if (!profile?.whatsappNumber.isNullOrBlank()) sb.append("Contact: ${profile?.whatsappNumber}\n")
        if (!profile?.fssaiNumber.isNullOrBlank()) sb.append("FSSAI: ${profile?.fssaiNumber}\n")
        
        sb.append("\n*--- INVOICE ---*\n")
        sb.append("*Bill #:* ${bill.bill.dailyOrderId.toString().padStart(3, '0')}\n")
        
        val dateObj = DateUtils.parseDb(bill.bill.createdAt) ?: run {
            Log.w(TAG, "Malformed created_at timestamp: ${bill.bill.createdAt}")
            Date()
        }
        val formattedDate = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(dateObj)
        sb.append("*Date:* $formattedDate\n")
        
        if (!bill.bill.customerName.isNullOrBlank()) sb.append("*Customer:* ${bill.bill.customerName}\n")
        if (!bill.bill.customerWhatsapp.isNullOrBlank()) sb.append("*WA:* ${bill.bill.customerWhatsapp}\n")
        
        sb.append("\n*ITEMS*\n")
        for (item in bill.items) {
            val name = if (item.variantName != null) "${item.itemName} (${item.variantName})" else item.itemName
            sb.append("• ${item.quantity} x $name = $currency${formatMoney(item.itemTotal)}\n")
        }
        
        sb.append("----------------------------\n")
        sb.append("*Subtotal: $currency${formatMoney(bill.bill.subtotal)}*\n")
        
        if (profile?.gstEnabled == true) {
            val halfGst = bill.bill.gstPercentage / 2
            sb.append("CGST (${halfGst}%): $currency${formatMoney(bill.bill.cgstAmount)}\n")
            sb.append("SGST (${halfGst}%): $currency${formatMoney(bill.bill.sgstAmount)}\n")
        }
        
        sb.append("*TOTAL AMOUNT: $currency${formatMoney(bill.bill.totalAmount)}*\n")
        sb.append("----------------------------\n")
        sb.append("Payment: ${bill.bill.paymentMode.uppercase()}\n\n")
        sb.append("Thank you! Visit Again 🙏\n")
        sb.append("_Powered by KhanaBook POS_")
        
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

    /**
     * Converts a Bitmap into ESC/POS Raster bit-image commands (GS v 0).
     */
    private fun decodeBitmapToESC_POS(bitmap: Bitmap, maxWidth: Int): ByteArray {
        var scaledBitmap: Bitmap? = null
        try {
            // Resize to fit printer width while maintaining aspect ratio
            val scale = maxWidth.toFloat() / bitmap.width.toFloat()
            val targetHeight = (bitmap.height * scale).toInt()
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)

            val width = scaledBitmap!!.width
            val height = scaledBitmap.height
            val bytesWidth = (width + 7) / 8
            
            val data = mutableListOf<Byte>()
            
            // GS v 0 p wL wH hL hH (Raster bit image command)
            data.add(0x1D.toByte())
            data.add(0x76.toByte())
            data.add(0x30.toByte())
            data.add(0x00.toByte())
            
            data.add((bytesWidth % 256).toByte()) // wL
            data.add((bytesWidth / 256).toByte()) // wH
            data.add((height % 256).toByte())    // hL
            data.add((height / 256).toByte())    // hH

            for (y in 0 until height) {
                for (x in 0 until bytesWidth) {
                    var bite = 0
                    for (b in 0 until 8) {
                        val pixelX = x * 8 + b
                        if (pixelX < width) {
                            val pixel = scaledBitmap.getPixel(pixelX, y)
                            val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                            if (gray < 128) { // Black pixel threshold
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


