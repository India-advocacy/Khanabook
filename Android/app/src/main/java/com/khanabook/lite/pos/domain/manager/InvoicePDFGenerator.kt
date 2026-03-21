package com.khanabook.lite.pos.domain.manager

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

        // 1. Setup Page Dimensions
        val is80mm = profile?.paperSize == "80mm"
        val pageWidth = if (is80mm) 226 else 164

        // 2. Load Logo if exists
        var logoBitmap: Bitmap? = null
        try {
            logoBitmap = profile?.logoPath?.let { path ->
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }

            // 3. Configuration
            val includeLogo = profile?.includeLogoInPrint == true
            val includeCustomerWhatsapp = profile?.printCustomerWhatsapp == true

            // 4. Calculate Heights
            val logoHeight = if (logoBitmap != null && includeLogo) 50 else 0
            val whatsappHeight =
                if (includeCustomerWhatsapp && !bill.bill.customerWhatsapp.isNullOrBlank()) 12
                else 0
            val fssaiHeight = if (!profile?.fssaiNumber.isNullOrBlank()) 12 else 0
            val gstinHeight = if (profile?.gstEnabled == true && !profile.gstin.isNullOrBlank()) 12 else 0
            val shopWaHeight = if (!profile?.whatsappNumber.isNullOrBlank()) 12 else 0
            
            val itemHeight = bill.items.size * 18
            val headerHeight = 180 + logoHeight + whatsappHeight + fssaiHeight + gstinHeight + shopWaHeight
            val summaryHeight = 140
            val taxHeight = if (profile?.gstEnabled == true) 50 else 0
            val footerHeight = 126
            val pageHeight = headerHeight + itemHeight + summaryHeight + taxHeight + footerHeight

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val normalTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val boldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val monoTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

            var y = 15f

            // 5. Draw Logo
            if (logoBitmap != null && includeLogo) {
                val scaledWidth = 35f
                val scaledHeight =
                    (logoBitmap.height.toFloat() / logoBitmap.width.toFloat()) * scaledWidth
                val left = (pageWidth - scaledWidth) / 2
                val rect = RectF(left, y, left + scaledWidth, y + scaledHeight)
                canvas.drawBitmap(logoBitmap, null, rect, paint)
                y += scaledHeight + 12f
            }

        // 6. Colors & Sizes
        val colorPrimary = if (isDigital) Color.parseColor("#2E150B") else Color.BLACK
        val colorText = Color.BLACK
        val mainTitleSize = if (is80mm) 12f else 10f
        val subTitleSize = if (is80mm) 7f else 6f
        val bodySize = if (is80mm) 8f else 6.5f
        val headerLabelSize = if (is80mm) 7f else 6f

        // 7. Shop Info
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

        paint.color = Color.parseColor("#757575") 
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

        paint.color = colorText
        paint.textAlign = Paint.Align.LEFT
        
        if (!profile?.fssaiNumber.isNullOrBlank()) {
            val label = "FSSAI No: "
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
            y += 9f 
        }

        if (!profile?.whatsappNumber.isNullOrBlank()) {
            val label = "Contact: "
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
            y += 9f 
        }

        // 8. Title with Header Bar
        y += 4f 
        paint.color = colorText
        paint.strokeWidth = 0.5f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
        
        y += 12f 
        if (isDigital) {
            paint.color = Color.argb(25, Color.red(colorPrimary), Color.green(colorPrimary), Color.blue(colorPrimary))
            canvas.drawRect(5f, y - 9f, (pageWidth - 5).toFloat(), y + 3f, paint)
        }
        paint.color = colorPrimary
        paint.typeface = boldTypeface
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
                if (profile?.gstEnabled == true) "TAX INVOICE" else "INVOICE",
                (pageWidth / 2).toFloat(),
                y,
                paint
        )
        y += 6f 
        paint.color = colorText
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)

        // 9. Bill Details
        y += 14f
        paint.textSize = headerLabelSize
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = boldTypeface
        canvas.drawText("BILL: ${bill.bill.lifetimeOrderId}", 5f, y, paint)
        
        val dateStr = com.khanabook.lite.pos.domain.util.DateUtils.formatDateOnly(bill.bill.createdAt)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("DATE: $dateStr", (pageWidth - 5).toFloat(), y, paint)

        y += 10f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("CUST: ${bill.bill.customerName ?: "Walking Customer"}", 5f, y, paint)
        if (!bill.bill.customerWhatsapp.isNullOrBlank()) {
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("WA: ${bill.bill.customerWhatsapp}", (pageWidth - 5).toFloat(), y, paint)
        }

        // 10. Table Header
        y += 12f
        paint.strokeWidth = 0.3f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
        
        y += 10f
        paint.typeface = boldTypeface
        paint.textSize = headerLabelSize
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("ITEM", 5f, y, paint)

        val rateX = if (is80mm) 130f else 90f
        val qtyX  = if (is80mm) 160f else 115f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("RATE", rateX, y, paint)
        canvas.drawText("QTY",  qtyX,  y, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMT", (pageWidth - 5).toFloat(), y, paint)
        
        y += 4f
        paint.strokeWidth = 0.3f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)

        // 11. Items with Dividers
        paint.typeface = normalTypeface
        paint.textSize = bodySize
        y += 12f
        bill.items.forEachIndexed { index, item ->
            paint.color = colorText
            paint.textAlign = Paint.Align.LEFT
            val displayName = item.itemName.uppercase()
            val maxChars = if (is80mm) 22 else 15
            canvas.drawText(
                    if (displayName.length > maxChars) displayName.take(maxChars - 2) + ".." else displayName,
                    5f,
                    y,
                    paint
            )

            paint.textAlign = Paint.Align.CENTER
            val priceStr = try {
                java.math.BigDecimal(item.price)
                    .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }
            canvas.drawText(priceStr, rateX, y, paint)
            canvas.drawText("${item.quantity}", qtyX, y, paint)

            paint.textAlign = Paint.Align.RIGHT
            val itemTotalStr = try {
                java.math.BigDecimal(item.itemTotal)
                    .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }
            canvas.drawText(itemTotalStr, (pageWidth - 5).toFloat(), y, paint)
            
            y += 4f
            if (index < bill.items.size - 1) {
                paint.color = Color.parseColor("#EEEEEE")
                paint.strokeWidth = 0.1f
                canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
            }
            y += 10f 
        }

        // 12. Summary
        y += 2f
        paint.strokeWidth = 0.3f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
        
        y += 12f
        paint.typeface = normalTypeface
        val summaryLabelX = pageWidth * 0.55f 
        
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Sub-Total", summaryLabelX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        val subtotalStr = try {
            java.math.BigDecimal(bill.bill.subtotal)
                .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        } catch (e: Exception) { "0.00" }
        canvas.drawText(subtotalStr, (pageWidth - 5).toFloat(), y, paint)

        if (profile?.gstEnabled == true) {
            val halfGst = try {
                java.math.BigDecimal(bill.bill.gstPercentage)
                    .divide(java.math.BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString()
            } catch (e: Exception) { "0" }

            val cgstAmt = try {
                java.math.BigDecimal(bill.bill.cgstAmount)
                    .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }

            val sgstAmt = try {
                java.math.BigDecimal(bill.bill.sgstAmount)
                    .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }

            y += 10f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("CGST ($halfGst%)", summaryLabelX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(cgstAmt, (pageWidth - 5).toFloat(), y, paint)

            y += 10f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("SGST ($halfGst%)", summaryLabelX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(sgstAmt, (pageWidth - 5).toFloat(), y, paint)
        }

        if (profile?.gstEnabled == false &&
            try { java.math.BigDecimal(bill.bill.customTaxAmount) > java.math.BigDecimal.ZERO } catch (e: Exception) { false }
        ) {
            val taxLabel = profile.customTaxName?.takeIf { it.isNotBlank() } ?: "Tax"
            val taxAmt = try {
                java.math.BigDecimal(bill.bill.customTaxAmount)
                    .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }

            y += 10f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("$taxLabel:", summaryLabelX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(taxAmt, (pageWidth - 5).toFloat(), y, paint)
        }

        y += 8f
        paint.strokeWidth = 0.3f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)

        // 13. Highlighted Total Box
        y += 12f
        if (isDigital) {
            paint.color = colorPrimary
            canvas.drawRoundRect(5f, y, (pageWidth - 5).toFloat(), y + 24f, 4f, 4f, paint)
            paint.color = Color.WHITE
        } else {
            paint.color = colorText
            paint.strokeWidth = 0.5f
            canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)
            y += 4f
        }
        
        y += 16f
        paint.typeface = boldTypeface
        paint.textSize = bodySize + 1.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("NET AMOUNT", 10f, y, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        val currency = if (profile?.currency == "INR" || profile?.currency == "Rupee") "₹" else profile?.currency ?: ""
        val totalAmountStr = try {
            java.math.BigDecimal(bill.bill.totalAmount)
                .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        } catch (e: Exception) { "0.00" }
        canvas.drawText(
                "${if (currency == "₹") "" else currency} $totalAmountStr",
                (pageWidth - 10).toFloat(),
                y,
                paint
        )

        // Rule 4: Payment Mode
        y += 24f
        paint.color = colorText
        paint.typeface = normalTypeface
        paint.textSize = bodySize
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "Payment: ${
                com.khanabook.lite.pos.domain.model.PaymentMode
                    .fromDbValue(bill.bill.paymentMode).displayLabel
            }",
            5f, y, paint
        )

        // 14. Footer
        y += 16f 
        paint.typeface = boldTypeface
        paint.textSize = 7f
        paint.textAlign = Paint.Align.CENTER
        if (isDigital) {
            paint.color = colorPrimary
            canvas.drawRect(5f, y - 9f, (pageWidth - 5).toFloat(), y + 4f, paint)
            paint.color = Color.WHITE
        }
        canvas.drawText("Thank you! Visit again.", (pageWidth / 2).toFloat(), y, paint)

        paint.color = colorText
        paint.typeface = normalTypeface
        y += 14f
        if (profile?.showBranding != false) {
            canvas.drawText("Powered by KhanaBook", (pageWidth / 2).toFloat(), y, paint)
        }
        
        y += 10f
        paint.strokeWidth = 0.5f
        canvas.drawLine(5f, y, (pageWidth - 5).toFloat(), y, paint)

        pdfDocument.finishPage(page)

        val invoiceDir = File(context.cacheDir, "invoices")
        invoiceDir.mkdirs()
        val file = File(invoiceDir, "invoice_${bill.bill.lifetimeOrderId}.pdf")
        try {
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
        } finally {
            pdfDocument.close()
        }
        return file
    } finally {
        logoBitmap?.recycle()
    }
}
}
