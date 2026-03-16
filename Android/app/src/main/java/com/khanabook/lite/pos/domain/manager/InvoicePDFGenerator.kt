package com.khanabook.lite.pos.domain.manager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import java.io.File
import java.io.FileOutputStream

class InvoicePDFGenerator(private val context: Context) {

    fun generatePDF(
            bill: BillWithItems,
            profile: RestaurantProfileEntity?,
            isDigital: Boolean = true
    ): File {
        val pdfDocument = PdfDocument()

        // 58mm = ~164 pts, 80mm = ~226 pts
        val is80mm = profile?.paperSize == "80mm"
        val pageWidth = if (is80mm) 226 else 164

        // Load Logo if exists
        val logoBitmap =
                profile?.logoPath?.let { path ->
                    try {
                        BitmapFactory.decodeFile(path)
                    } catch (e: Exception) {
                        null
                    }
                }

        // 0. Build visual flags from profile
        val includeLogo = profile?.includeLogoInPrint == true
        val includeCustomerWhatsapp = profile?.printCustomerWhatsapp == true

        // Calculate height dynamically
        val logoHeight = if (logoBitmap != null && includeLogo) 50 else 0
        val whatsappHeight =
                if (includeCustomerWhatsapp && !bill.bill.customerWhatsapp.isNullOrBlank()) 12
                else 0
        val fssaiHeight = if (!profile?.fssaiNumber.isNullOrBlank()) 12 else 0
        val gstinHeight = if (profile?.gstEnabled == true && !profile.gstin.isNullOrBlank()) 12 else 0
        val shopWaHeight = if (!profile?.whatsappNumber.isNullOrBlank()) 12 else 0
        
        val itemHeight = bill.items.size * 14
        val headerHeight = 180 + logoHeight + whatsappHeight + fssaiHeight + gstinHeight + shopWaHeight
        val summaryHeight = 130
        val taxHeight = if (profile?.gstEnabled == true) 30 else 0
        val footerHeight = 100
        val pageHeight = headerHeight + itemHeight + summaryHeight + taxHeight + footerHeight

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val normalTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val boldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var y = 15f

        // 0. Draw Logo (if enabled in settings)
        if (logoBitmap != null && includeLogo) {
            val scaledWidth = 35f
            val scaledHeight =
                    (logoBitmap.height.toFloat() / logoBitmap.width.toFloat()) * scaledWidth
            val left = (pageWidth - scaledWidth) / 2
            val rect = RectF(left, y, left + scaledWidth, y + scaledHeight)
            canvas.drawBitmap(logoBitmap, null, rect, paint)
            y += scaledHeight + 12f
        }

        // Colors
        val colorPrimary = if (isDigital) Color.parseColor("#2E150B") else Color.BLACK
        val colorVeg = if (isDigital) Color.parseColor("#2E7D32") else Color.BLACK
        val colorNonVeg = if (isDigital) Color.parseColor("#C62828") else Color.BLACK
        val colorText = Color.BLACK

        // Font sizes (Polished & Shrunk)
        val mainTitleSize = if (is80mm) 12f else 10f
        val subTitleSize = if (is80mm) 7f else 6f
        val bodySize = if (is80mm) 8f else 6.5f
        val headerLabelSize = if (is80mm) 7f else 6f

        // 1. Header (Centered)
        paint.color = colorPrimary
        paint.typeface = boldTypeface
        paint.textSize = mainTitleSize
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
                profile?.shopName?.uppercase() ?: "RESTAURANT",
                (pageWidth / 2).toFloat(),
                y,
                paint
        )

        paint.color = Color.parseColor("#757575") // Lighter grey like standard Android 'Secondary' text
        paint.typeface = normalTypeface
        paint.textSize = subTitleSize
        y += 10f
        
        val fullAddress = profile?.shopAddress ?: ""
        if (fullAddress.isNotBlank()) {
            val lines = if (fullAddress.length > 35) {
                val mid = fullAddress.lastIndexOf(",", 35).takeIf { it != -1 } 
                           ?: fullAddress.lastIndexOf(" ", 35).takeIf { it != -1 }
                           ?: 35
                listOf(fullAddress.substring(0, mid + 1).trim(), fullAddress.substring(mid + 1).trim())
            } else {
                listOf(fullAddress)
            }
            
            lines.take(2).forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line, (pageWidth / 2).toFloat(), y, paint)
                    y += 9f
                }
            }
        }

        // Draw FSSAI and GST with mixed styles
        paint.color = colorText
        paint.textAlign = Paint.Align.LEFT
        
        if (!profile?.fssaiNumber.isNullOrBlank()) {
            val label = "FSSAI LIC NO: "
            val value = profile?.fssaiNumber ?: ""
            paint.typeface = boldTypeface
            val lw = paint.measureText(label)
            paint.typeface = normalTypeface
            val vw = paint.measureText(value)
            val sx = (pageWidth - (lw + vw)) / 2
            paint.typeface = boldTypeface
            canvas.drawText(label, sx, y, paint)
            paint.typeface = normalTypeface
            canvas.drawText(value, sx + lw, y, paint)
            y += 8f
        }

        if (profile?.gstEnabled == true && !profile.gstin.isNullOrBlank()) {
            val label = "GST NO: "
            val value = profile.gstin
            paint.typeface = boldTypeface
            val lw = paint.measureText(label)
            paint.typeface = normalTypeface
            val vw = paint.measureText(value)
            val sx = (pageWidth - (lw + vw)) / 2
            paint.typeface = boldTypeface
            canvas.drawText(label, sx, y, paint)
            paint.typeface = normalTypeface
            canvas.drawText(value, sx + lw, y, paint)
            y += 9f // Increased from 4f to prevent overlap
        }

        if (!profile?.whatsappNumber.isNullOrBlank()) {
            val label = "SHOP WA: "
            val value = profile?.whatsappNumber ?: ""
            paint.typeface = boldTypeface
            val lw = paint.measureText(label)
            paint.typeface = normalTypeface
            val vw = paint.measureText(value)
            val sx = (pageWidth - (lw + vw)) / 2
            paint.typeface = boldTypeface
            canvas.drawText(label, sx, y, paint)
            paint.typeface = normalTypeface
            canvas.drawText(value, sx + lw, y, paint)
            y += 9f // Increased from 4f to prevent overlap
        }

        // 2. Divider & Title
        y += 1f // Minimal gap
        paint.strokeWidth = 1f
        paint.color = colorText
        paint.textAlign = Paint.Align.LEFT
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
        y += 12f 
        paint.typeface = boldTypeface
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
                if (profile?.gstEnabled == true) "TAX INVOICE" else "INVOICE",
                (pageWidth / 2).toFloat(),
                y,
                paint
        )
        y += 6f // Increased from 5f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)

        // 3. Bill Info
        y += 10f
        paint.textSize = headerLabelSize
        paint.textAlign = Paint.Align.LEFT
        
        // BILL (Mixed style)
        val billLabel = "BILL: "
        val billValue = "${bill.bill.lifetimeOrderId}"
        paint.typeface = boldTypeface
        val blw = paint.measureText(billLabel)
        canvas.drawText(billLabel, 5f, y, paint)
        paint.typeface = normalTypeface
        canvas.drawText(billValue, 5f + blw, y, paint)
        
        // DATE (Mixed style, Right aligned)
        val dateLabel = "DATE: "
        val dateValue = com.khanabook.lite.pos.domain.util.DateUtils.formatDateOnly(bill.bill.createdAt)
        paint.typeface = boldTypeface
        val dlw = paint.measureText(dateLabel)
        paint.typeface = normalTypeface
        val dvw = paint.measureText(dateValue)
        val dateStartX = (pageWidth - 5) - (dlw + dvw)
        paint.typeface = boldTypeface
        canvas.drawText(dateLabel, dateStartX, y, paint)
        paint.typeface = normalTypeface
        canvas.drawText(dateValue, dateStartX + dlw, y, paint)

        // Customer Info (Mixed style)
        if (includeCustomerWhatsapp && !bill.bill.customerWhatsapp.isNullOrBlank()) {
            y += 8f
            paint.textAlign = Paint.Align.LEFT
            
            // CUST
            val custLabel = "CUST: "
            val custValue = bill.bill.customerName ?: "GUEST"
            paint.typeface = boldTypeface
            val clw = paint.measureText(custLabel)
            canvas.drawText(custLabel, 5f, y, paint)
            paint.typeface = normalTypeface
            canvas.drawText(custValue, 5f + clw, y, paint)
            
            // WA (Right aligned)
            val waLabel = "WA: "
            val waValue = bill.bill.customerWhatsapp ?: ""
            paint.typeface = boldTypeface
            val wlw = paint.measureText(waLabel)
            paint.typeface = normalTypeface
            val wvw = paint.measureText(waValue)
            val waStartX = (pageWidth - 5) - (wlw + wvw)
            paint.typeface = boldTypeface
            canvas.drawText(waLabel, waStartX, y, paint)
            paint.typeface = normalTypeface
            canvas.drawText(waValue, waStartX + wlw, y, paint)
        } else if (!bill.bill.customerName.isNullOrBlank()) {
            // Only name if WhatsApp is disabled/missing
            y += 8f
            paint.textAlign = Paint.Align.LEFT
            val custLabel = "CUST: "
            val custValue = bill.bill.customerName ?: "GUEST"
            paint.typeface = boldTypeface
            val clw = paint.measureText(custLabel)
            canvas.drawText(custLabel, 5f, y, paint)
            paint.typeface = normalTypeface
            canvas.drawText(custValue, 5f + clw, y, paint)
        }

        // 4. Table Header
        y += 10f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
        y += 8f
        paint.typeface = boldTypeface
        paint.textSize = headerLabelSize
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("ITEM", 5f, y, paint)

        val qtyX = if (is80mm) 160f else 110f

        canvas.drawText("QTY", qtyX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMT", (pageWidth - 5).toFloat(), y, paint)
        y += 4f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)

        // 5. Items
        paint.typeface = normalTypeface
        paint.textSize = bodySize
        paint.letterSpacing = 0.03f // Improve item name legibility
        y += 10f
        bill.items.forEachIndexed { _, item ->
            paint.textAlign = Paint.Align.LEFT
            paint.color = colorText
            val displayName = item.itemName.uppercase()
            // Even more space now without the dot
            val maxChars = if (is80mm) 38 else 28
            canvas.drawText(
                    if (displayName.length > maxChars) displayName.take(maxChars - 2) + ".." else displayName,
                    5f,
                    y,
                    paint
            )

            paint.textAlign = Paint.Align.CENTER
            paint.letterSpacing = 0f // Reset for numbers
            canvas.drawText("${item.quantity}", qtyX + 8f, y, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                    String.format("%.2f", item.itemTotal),
                    (pageWidth - 5).toFloat(),
                    y,
                    paint
            )
            y += 12f // Increased spacing between items
            paint.letterSpacing = 0.03f // Re-enable for next item name
        }
        paint.letterSpacing = 0f // Reset globally

        // 6. Summary
        y += 4f
        paint.color = colorText
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
        y += 10f
        paint.typeface = normalTypeface
        paint.textSize = bodySize
        
        // Match preview alignment: labels start from mid-page
        val summaryLabelX = pageWidth * 0.55f 
        
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Sub-Total", summaryLabelX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
                String.format("%.2f", bill.bill.subtotal),
                (pageWidth - 5).toFloat(),
                y,
                paint
        )

        // GST Row - ONLY if enabled
        if (profile?.gstEnabled == true) {
            y += 9f
            paint.textAlign = Paint.Align.LEFT
            paint.letterSpacing = 0.05f // Add spacing for GST label
            canvas.drawText("GST (${bill.bill.gstPercentage}%)", summaryLabelX, y, paint)
            paint.letterSpacing = 0f // Reset
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                    String.format("%.2f", bill.bill.cgstAmount + bill.bill.sgstAmount),
                    (pageWidth - 5).toFloat(),
                    y,
                    paint
            )
        }

        y += 8f
        paint.color = colorText
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)

        y += 15f
        paint.typeface = boldTypeface
        paint.textSize = bodySize + 1.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("NET AMOUNT", 5f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        val currency =
                if (profile?.currency == "INR" || profile?.currency == "Rupee") "₹"
                else profile?.currency ?: ""
        canvas.drawText(
                "$currency ${String.format("%.2f", bill.bill.totalAmount)}",
                (pageWidth - 15).toFloat(),
                y,
                paint
        )

        // 8. Footer
        paint.typeface = boldTypeface
        paint.textSize = 7f
        paint.textAlign = Paint.Align.CENTER
        y += 25f

        if (isDigital) {
            paint.color = colorPrimary
            canvas.drawRect(5f, y - 8f, (pageWidth - 5).toFloat(), y + 4f, paint)
            paint.color = Color.WHITE
        }

        canvas.drawText("THANK YOU! VISIT AGAIN", (pageWidth / 2).toFloat(), y, paint)

        paint.color = colorText
        paint.typeface = normalTypeface
        y += 12f
        canvas.drawText("Software by KhanaBook", (pageWidth / 2).toFloat(), y, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "invoice_${bill.bill.lifetimeOrderId}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }
}


