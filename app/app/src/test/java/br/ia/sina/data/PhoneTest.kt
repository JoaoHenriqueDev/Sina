package br.ia.sina.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneTest {
    @Test
    fun aceitaNumeroComDddEMascara() {
        assertEquals("11987654321", normalizePhone("(11) 98765-4321"))
        assertEquals("5511987654321", normalizePhone("+55 11 98765-4321"))
        assertEquals("1133334444", normalizePhone("11 3333-4444"))
    }

    @Test
    fun recusaSemDddOuComprido() {
        assertNull(normalizePhone("98765-4321"))
        assertNull(normalizePhone("55 11 98765-43210"))
        assertNull(normalizePhone(""))
    }
}
