package com.khanabook.lite.pos.domain.manager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

object UpiQrManager {

    
    fun generateUpiQr(vpa: String, name: String, amount: Double): Bitmap? {
        return try {
            val uri = "upi://pay?pa=$vpa&pn=$name&am=${"%.2f".format(amount)}&cu=INR"
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(uri, BarcodeFormat.QR_CODE, 512, 512)
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


