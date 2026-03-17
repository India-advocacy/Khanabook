package com.khanabook.lite.pos.domain.manager

import org.junit.Test
import org.junit.Assert.*

class BillCalculatorTest {

    @Test
    fun testSubtotalCalculation_BigDecimalPrecision() {
        
        val items = listOf(
            10.555 to 1, 
            20.123 to 2  
            
        )
        
        val subtotal = BillCalculator.calculateSubtotal(items)
        assertEquals(50.80, subtotal, 0.0)
    }

    @Test
    fun testGSTSplit_CGST_SGST_Equality() {
        val subtotal = 100.0
        val gstPct = 18.0
        
        val breakdown = BillCalculator.calculateGST(subtotal, gstPct)
        
        
        assertEquals(9.0, breakdown.cgst, 0.0)
        assertEquals(9.0, breakdown.sgst, 0.0)
        assertEquals(18.0, breakdown.totalGst, 0.0)
    }

    @Test
    fun testGSTSplit_OddTotal() {
        
        
        val subtotal = 58.61
        val gstPct = 18.0
        
        val breakdown = BillCalculator.calculateGST(subtotal, gstPct)
        
        
        
        
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
