package com.khanabook.lite.pos.domain.manager

import org.junit.Test
import org.junit.Assert.*

class BillCalculatorTest {

    @Test
    fun testSubtotalCalculation_BigDecimalPrecision() {
        // Test items with fractional prices
        val items = listOf(
            10.555 to 1, // Should round to 10.56
            20.123 to 2  // 40.246 -> Should round to 40.25 (BUT in current impl, it adds then rounds)
            // Current impl: (10.555 * 1) + (20.123 * 2) = 10.555 + 40.246 = 50.801 -> 50.80
        )
        
        val subtotal = BillCalculator.calculateSubtotal(items)
        assertEquals(50.80, subtotal, 0.0)
    }

    @Test
    fun testGSTSplit_CGST_SGST_Equality() {
        val subtotal = 100.0
        val gstPct = 18.0
        
        val breakdown = BillCalculator.calculateGST(subtotal, gstPct)
        
        // 18% of 100 = 18.0. Split: 9.0 and 9.0
        assertEquals(9.0, breakdown.cgst, 0.0)
        assertEquals(9.0, breakdown.sgst, 0.0)
        assertEquals(18.0, breakdown.totalGst, 0.0)
    }

    @Test
    fun testGSTSplit_OddTotal() {
        // If GST is 10.55, Split should be 5.28 and 5.27 (RoundingMode.HALF_UP check)
        // subtotal = 58.61, gst = 18% -> 10.5498 -> 10.55
        val subtotal = 58.61
        val gstPct = 18.0
        
        val breakdown = BillCalculator.calculateGST(subtotal, gstPct)
        
        // totalGst = 10.55
        // cgst = 10.55 / 2 = 5.275 -> 5.28
        // sgst = 10.55 - 5.28 = 5.27
        assertEquals(10.55, breakdown.totalGst, 0.0)
        assertEquals(5.28, breakdown.cgst, 0.0)
        assertEquals(5.27, breakdown.sgst, 0.0)
    }

    @Test
    fun testTotalCalculation() {
        val subtotal = 100.0
        val cgst = 9.0
        val sgst = 9.0
        val customTax = 0.0
        
        val total = BillCalculator.calculateTotal(subtotal, cgst, sgst, customTax)
        assertEquals(118.0, total, 0.0)
    }
}
