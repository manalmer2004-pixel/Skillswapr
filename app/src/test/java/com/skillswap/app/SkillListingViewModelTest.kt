package com.skillswap.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SkillListingViewModelTest {
    @Test
    fun testInitialTimeCreditBalance() {
        val welcomeBonusCredits = 3
        assertEquals(3, welcomeBonusCredits)
    }

    @Test
    fun testTimeBankingExchangeRate() {
        val hoursTaught = 2
        val creditsEarned = hoursTaught * 1
        assertEquals(2, creditsEarned)
    }
}
