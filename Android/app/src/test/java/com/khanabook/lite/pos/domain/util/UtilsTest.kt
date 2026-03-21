package com.khanabook.lite.pos.domain.util

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.khanabook.lite.pos.data.local.entity.BillEntity
import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import com.khanabook.lite.pos.domain.manager.InvoicePDFGenerator
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class UtilsTest {

    private lateinit var context: Context
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var toast: Toast

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        clipboardManager = mockk(relaxed = true)
        toast = mockk(relaxed = true)

        mockkStatic(FileProvider::class)
        mockkStatic(Toast::class)
        mockkConstructor(InvoicePDFGenerator::class)

        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboardManager
        every { Toast.makeText(any<Context>(), any<CharSequence>(), any<Int>()) } returns toast
        every { FileProvider.getUriForFile(any(), any(), any()) } returns mockk<Uri>()
        every { anyConstructed<InvoicePDFGenerator>().generatePDF(any(), any(), any()) } returns mockk<File>()
    }

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
        unmockkStatic(Toast::class)
        unmockkConstructor(InvoicePDFGenerator::class)
    }

    @Test
    fun `shareBillAsPdf formats phone correctly and targets WhatsApp`() {
        // Arrange
        val bill = BillEntity(
            id = 1,
            dailyOrderId = 1,
            dailyOrderDisplay = "001",
            lifetimeOrderId = 1001,
            totalAmount = "100.0",
            subtotal = "100.0",
            paymentMode = "cash",
            paymentStatus = "paid",
            orderStatus = "completed",
            customerWhatsapp = "9876543210"
        )
        val billWithItems = BillWithItems(bill = bill, items = emptyList(), payments = emptyList())
        val profile = RestaurantProfileEntity(
            id = 1,
            shopName = "Test Shop",
            whatsappNumber = "1234567890"
        )

        // Act
        shareBillAsPdf(context, billWithItems, profile)

        // Assert
        val intentSlot = slot<Intent>()
        verify { context.startActivity(capture(intentSlot)) }

        val capturedIntent = intentSlot.captured
        assertEquals(Intent.ACTION_SEND, capturedIntent.action)
        assertEquals("com.whatsapp", capturedIntent.`package`)
        assertEquals("919876543210@s.whatsapp.net", capturedIntent.getStringExtra("jid"))
        assertEquals("Invoice from Test Shop", capturedIntent.getStringExtra(Intent.EXTRA_TEXT))
    }
}
