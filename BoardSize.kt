package com.example.finalproject.initials

enum class BoardSize(val numCards: Int) {
    BEGINNER(8),
    SEMIPRO(18),
    PROFESSIONAL(24);

    companion object {
        fun getByValue(value: Int) = values().first {it.numCards == value }
    }

    fun getWidth(): Int {
        return when (this) {
            BEGINNER -> 2
            SEMIPRO -> 3
            PROFESSIONAL -> 4
        }
    }

    fun findHeight(): Int {
        return numCards / getWidth()
    }

    fun findNumPairs(): Int {
        return numCards / 2
    }

    fun findTime() {
        val begin = System.nanoTime()

        Thread.sleep(2000)

        val end = System.nanoTime()

        println("Elapsed time in nanoseconds: $(end-begin)")
    }
}