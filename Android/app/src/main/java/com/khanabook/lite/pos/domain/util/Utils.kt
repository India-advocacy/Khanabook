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
            .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
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

fun shareBillAsPdf(context: Context, billWithItems: BillWithItems, profile: RestaurantProfileEntity?) {
    try {
        val pdfGenerator = InvoicePDFGenerator(context)
        val pdfFile = pdfGenerator.generatePDF(billWithItems, profile)
        val pdfUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )

        val phone = billWithItems.bill.customerWhatsapp
        var formattedPhone = phone?.replace(Regex("[^0-9]"), "")
        if (formattedPhone != null && formattedPhone.length == 10) {
            formattedPhone = "91$formattedPhone"
        }

        if (formattedPhone != null) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Customer WhatsApp", formattedPhone)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Number copied! If chat doesn't open, paste it in WhatsApp search.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Ignore clipboard errors
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, "Invoice from ${profile?.shopName ?: "KhanaBook"}")
                putExtra("jid", "$formattedPhone@s.whatsapp.net")
                `package` = "com.whatsapp"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Share Invoice PDF"))
            }
        } else {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice PDF"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
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



