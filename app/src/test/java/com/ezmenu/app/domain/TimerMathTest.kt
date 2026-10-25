package com.ezmenu.app.domain
import org.junit.Assert.*
import org.junit.Test

class TimerMathTest {
    @Test fun deadlineSurvivesSkippedTicks() { assertEquals(1000,TimerMath.remaining(30000,35000,34000,true)) }
    @Test fun expiredNeverBecomesNegative() { assertEquals(0,TimerMath.remaining(30000,35000,40000,true)) }
    @Test fun pausedDoesNotAdvance() { assertEquals(42000,TimerMath.remaining(42000,null,999999,false)) }
}
