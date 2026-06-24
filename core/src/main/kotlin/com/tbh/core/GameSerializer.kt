package com.tbh.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Czysty most String ↔ GameState (Kotlinx Serialization, zero zależności od Androida).
 * Faktyczne I/O pliku robi warstwa :app — tutaj tylko (de)serializacja.
 */
object GameSerializer {

    // ignoreUnknownKeys: stare zapisy dadzą się odczytać po dodaniu nowych pól w przyszłości.
    // encodeDefaults: pola z wartościami domyślnymi też trafiają do JSON (pełny snapshot).
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(state: GameState): String = json.encodeToString(state)

    fun fromJson(text: String): GameState = json.decodeFromString(text)
}
