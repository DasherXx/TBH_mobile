package com.tbh.core

/**
 * Prosty deterministyczny generator liczb losowych (LCG — Knuth MMIX).
 * Wyłącznie w :core — bez importów Androida.
 * internal żeby :app nie mógł go użyć bezpośrednio.
 */
internal class LcgRng(seed: Long) {
    private var s = seed

    /** Zwraca losową liczbę całkowitą z przedziału [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        s = s * 6364136223846793005L + 1442695040888963407L
        // ushr = unsigned right shift — wynik zawsze nieujemny
        return ((s ushr 33) % bound.toLong()).toInt()
    }

    /** Aktualny stan generatora — zapisywany w GameState jako rngSeed. */
    val state: Long get() = s
}
