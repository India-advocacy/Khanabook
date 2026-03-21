package com.khanabook.lite.pos.domain.util

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import com.khanabook.lite.pos.domain.manager.InvoicePDFGenerator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private const val DISPLAY_FORMAT = "dd MMM yyyy, hh:mm a"

    fun formatDisplay(timestamp: Long): String {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern(DISPLAY_FORMAT))
    }

    fun formatDateOnly(timestamp: Long): String {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }

    fun formatDisplayDate(timestamp: Long): String {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("MMMM\ndd, yyyy,\nHH:mm a"))
    }

    fun formatDisplayWithZone(timestamp: Long, zoneId: String): String {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.of(zoneId))
            .format(java.time.format.DateTimeFormatter.ofPattern(DISPLAY_FORMAT))
    }

    fun parseDb(timestamp: Long): Date {
        return Date(timestamp)
    }

    fun getStartOfDay(date: String, timezone: String = "Asia/Kolkata"): Long {
        return java.time.LocalDate.parse(date)
            .atStartOfDay(java.time.ZoneId.of(timezone))
            .toInstant()
            .toEpochMilli()
    }

    fun getEndOfDay(date: String, timezone: String = "Asia/Kolkata"): Long {
        return java.time.LocalDate.parse(date)
            .atTime(java.time.LocalTime.MAX)
            .atZone(java.time.ZoneId.of(timezone))
            .toInstant()
            .toEpochMilli()
    }
}

object CurrencyUtils {
    fun formatPrice(amount: Double, currency: String = "\u20b9"): String {
        return "$currency ${String.format("%.2f", amount)}"
    }
    
    fun formatPrice(amount: String?, currency: String = "\u20b9"): String {
        val d = amount?.toDoubleOrNull() ?: 0.0
        return formatPrice(d, currency)
    }
}


fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun normalizeWhatsAppNumber(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    // strip everything except digits
    val digits = raw.replace(Regex("[^0-9]"), "")
    return when {
        digits.length == 10 -> "91$digits"           // bare Indian number
        digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}" // 0XXXXXXXXXX
        digits.length == 12 && digits.startsWith("91") -> digits               // 91XXXXXXXXXX
        digits.length == 13 && digits.startsWith("091") -> digits.drop(1)      // 091XXXXXXXXXX
        digits.length >= 10 -> digits                 // international, trust as-is
        else -> null                                  // too short, unusable
    }
}

fun shareBillOnWhatsApp(
    context: Context,
    billWithItems: BillWithItems,
    profile: RestaurantProfileEntity?
) {
    try {
        // 1. Generate PDF
        val pdfGenerator = InvoicePDFGenerator(context)
        val pdfFile = pdfGenerator.generatePDF(billWithItems, profile)
        val pdfUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )

        // 2. Build WhatsApp text body using the formatter
        val textBody = com.khanabook.lite.pos.domain.util.InvoiceFormatter
            .formatForWhatsApp(billWithItems, profile)

        // 3. Normalize customer phone
        val formattedPhone = normalizeWhatsAppNumber(
            billWithItems.bill.customerWhatsapp
        )

        if (formattedPhone != null) {
            // --- SAVED OR KNOWN NUMBER PATH ---
            // Try WhatsApp consumer first, then Business, then wa.me deep link
            val sent = tryDirectWhatsApp(context, formattedPhone, textBody, pdfUri)
            if (!sent) {
                tryWaMeDeepLink(context, formattedPhone, textBody, pdfUri)
            }
        } else {
            // --- NO NUMBER PATH ---
            // Open generic share chooser with text + PDF
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, textBody)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(fallbackIntent, "Share Invoice")
            )
        }

    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Error sharing invoice: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun tryDirectWhatsApp(
    context: Context,
    phone: String,
    textBody: String,
    pdfUri: android.net.Uri
): Boolean {
    // WhatsApp ignores EXTRA_TEXT when type = application/pdf.
    // So we send two intents in sequence:
    //   1. Text message to the jid (opens chat)
    //   2. PDF file to the same jid (attaches file)
    // This is the most reliable approach without the Business API.

    val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

    for (pkg in packages) {
        try {
            // Step A: send text first to open the correct chat
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textBody)
                putExtra("jid", "$phone@s.whatsapp.net")
                `package` = pkg
            }
            context.startActivity(textIntent)

            // Step B: after 800ms, send the PDF to the same chat
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val pdfIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        putExtra("jid", "$phone@s.whatsapp.net")
                        `package` = pkg
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(pdfIntent)
                } catch (_: Exception) {
                    // PDF step failed silently — text already sent
                }
            }, 800)

            return true // at least text intent succeeded
        } catch (_: Exception) {
            // try next package
        }
    }
    return false
}

private fun tryWaMeDeepLink(
    context: Context,
    phone: String,
    textBody: String,
    pdfUri: android.net.Uri
) {
    // wa.me is WhatsApp's officially documented unsaved contact deep link.
    // It opens a new chat pre-filled with the text. PDF must be shared separately.
    try {
        val encodedText = java.net.URLEncoder.encode(textBody, "UTF-8")
        val waUri = android.net.Uri.parse("https://wa.me/$phone?text=$encodedText")
        val waIntent = Intent(Intent.ACTION_VIEW, waUri)
        context.startActivity(waIntent)

        // After WA opens, copy PDF path to clipboard so user can attach manually
        // Only show this toast in the wa.me fallback path
        val clipboard = context.getSystemService(
            android.content.Context.CLIPBOARD_SERVICE
        ) as android.content.ClipboardManager
        val clip = android.content.ClipData.newUri(
            context.contentResolver,
            "Invoice PDF",
            pdfUri
        )
        clipboard.setPrimaryClip(clip)

        android.widget.Toast.makeText(
            context,
            "Text sent. To attach the PDF, tap the attachment icon in WhatsApp.",
            android.widget.Toast.LENGTH_LONG
        ).show()

    } catch (_: Exception) {
        // wa.me failed too — final fallback: generic chooser with both
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, textBody)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(fallbackIntent, "Share Invoice")
        )
    }
}

fun openBillToPrint(context: Context, billWithItems: BillWithItems, profile: RestaurantProfileEntity?) {
    
    val app = context.applicationContext as? com.khanabook.lite.pos.KhanaBookApplication
    
    
    
    
    try {
        val pdfGenerator = InvoicePDFGenerator(context)
        val pdfFile = pdfGenerator.generatePDF(billWithItems, profile)
        val pdfUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF to Print"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening printer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun directPrint(context: Context, billWithItems: BillWithItems, profile: RestaurantProfileEntity?, printerManager: com.khanabook.lite.pos.domain.manager.BluetoothPrinterManager) {
    if (profile?.printerEnabled != true) {
        openBillToPrint(context, billWithItems, profile)
        return
    }

    val scope = (context.findActivity() as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope ?: kotlinx.coroutines.GlobalScope
    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        if (!printerManager.isConnected() && !profile.printerMac.isNullOrBlank()) {
            printerManager.connect(profile.printerMac)
        }
        
        if (printerManager.isConnected()) {
            val bytes = InvoiceFormatter.formatForThermalPrinter(billWithItems, profile)
            printerManager.printBytes(bytes)
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Bluetooth Printer not connected. Opening PDF...", Toast.LENGTH_SHORT).show()
                openBillToPrint(context, billWithItems, profile)
            }
        }
    }
}
