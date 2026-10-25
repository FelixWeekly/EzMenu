package com.ezmenu.app.domain

interface TimerClock {
    fun elapsedMillis(): Long
    fun wallMillis(): Long
    fun bootCount(): Int
}

object TimerMath {
    fun remaining(storedMillis: Long, deadline: Long?, now: Long, running: Boolean): Long =
        (if (running && deadline != null) deadline - now else storedMillis).coerceAtLeast(0)
}
