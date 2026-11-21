package br.ia.sina.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingCodeTest {
    @Test
    fun extractsEightDigits() {
        assertEquals("48392017", extractPairingCode("48392017"))
        assertEquals("48392017", extractPairingCode("4839 2017"))
        assertEquals("48392017", extractPairingCode("sina:48392017"))
    }

    @Test
    fun rejectsOtherLengths() {
        assertNull(extractPairingCode("483920"))
        assertNull(extractPairingCode("1234567"))
        assertNull(extractPairingCode("123456789"))
        assertNull(extractPairingCode(null))
    }
}
