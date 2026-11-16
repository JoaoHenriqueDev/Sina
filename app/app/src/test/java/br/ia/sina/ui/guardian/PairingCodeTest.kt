package br.ia.sina.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingCodeTest {
    @Test
    fun extractsSixDigits() {
        assertEquals("483920", extractPairingCode("483920"))
        assertEquals("483920", extractPairingCode("483 920"))
        assertEquals("483920", extractPairingCode("sina:483920"))
    }

    @Test
    fun rejectsOtherLengths() {
        assertNull(extractPairingCode("12345"))
        assertNull(extractPairingCode("1234567"))
        assertNull(extractPairingCode(null))
    }
}
