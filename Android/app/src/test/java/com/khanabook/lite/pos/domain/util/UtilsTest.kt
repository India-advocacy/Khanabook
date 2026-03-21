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
        mockkStatic(Uri::class) // Added Uri static mock
        mockkConstructor(InvoicePDFGenerator::class)

        every { context.packageName } returns "com.khanabook.lite.pos"
        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboardManager
        every { Toast.makeText(any<Context>(), any<CharSequence>(), any<Int>()) } returns toast
        every { FileProvider.getUriForFile(any(), any(), any()) } returns mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } answers {
            val uri = mockk<Uri>(relaxed = true)
            every { uri.toString() } returns it.invocation.args[0] as String
            uri
        } // Mock Uri.parse properly
        every { anyConstructed<InvoicePDFGenerator>().generatePDF(any(), any(), any()) } returns mockk<File>()
    }

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
        unmockkStatic(Toast::class)
        unmockkStatic(Uri::class)
        unmockkConstructor(InvoicePDFGenerator::class)
    }

    @Test
    fun `shareBillOnWhatsApp formats phone correctly and targets WhatsApp`() {
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

        // Mock PackageInfo to simulate WhatsApp being installed
        every { context.packageManager.getPackageInfo("com.whatsapp", 0) } returns mockk()

        // Act
        shareBillOnWhatsApp(context, billWithItems, profile)

        // Assert
        val intentSlot = slot<Intent>()
        verify { context.startActivity(capture(intentSlot)) }

        val capturedIntent = intentSlot.captured
        assertEquals(Intent.ACTION_SEND, capturedIntent.action)
        assertEquals("com.whatsapp", capturedIntent.`package`)
        assertEquals("919876543210@s.whatsapp.net", capturedIntent.getStringExtra("jid"))
    }

    @Test
    fun `shareBillOnWhatsApp uses fallback for unsaved numbers`() {
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

        // Simulate WhatsApp NOT installed or direct share failing
        every { context.packageManager.getPackageInfo(any<String>(), any<Int>()) } throws Exception("Not found")
        // Also handle the newer signature if needed, but since we are mocking context.packageManager, 
        // MockK will try to match based on the provided types.
        // If it still fails, we'll try a more specific match.

        // Act
        shareBillOnWhatsApp(context, billWithItems, profile)

        // Assert
        // Should trigger ACTION_VIEW for the deep link
        verify { context.startActivity(match { it.action == Intent.ACTION_VIEW && it.data?.toString()?.contains("api.whatsapp.com") == true }) }
    }
}
