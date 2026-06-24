package com.tbh.mobile.state

import android.content.Context
import android.util.Log
import com.tbh.core.Equipment
import com.tbh.core.GameEngine
import com.tbh.core.GameSerializer
import com.tbh.core.GameState
import com.tbh.core.OfflineResult
import com.tbh.core.OfflineProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Jedno źródło prawdy dla GameState w całym procesie.
 * Zarówno OverlayService (pętla + nakładka), jak i MenuActivity (Compose) czytają/modyfikują
 * ten sam StateFlow. Persystencja (JSON) jest tutaj — każda zmiana stanu może być zapisana.
 */
object GameRepository {

    private val _state = MutableStateFlow(GameState.initial())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveFile: File? = null

    /**
     * Wczytuje zapis (jeśli istnieje) i nalicza postęp offline.
     * Zwraca OfflineResult do pokazania toastu powitalnego, albo null gdy brak zapisu/błąd.
     */
    fun init(context: Context, tickMs: Long): OfflineResult? {
        val file = File(context.filesDir, SAVE_FILE)
        saveFile = file
        if (!file.exists()) {
            _state.value = GameState.initial()
            return null
        }
        return try {
            val loaded = GameSerializer.fromJson(file.readText())
            val result = OfflineProgress.apply(loaded, System.currentTimeMillis(), tickMs)
            _state.value = result.state
            result
        } catch (e: Exception) {
            Log.w(TAG, "Nie udało się wczytać zapisu, start od nowa", e)
            _state.value = GameState.initial()
            null
        }
    }

    /** Pojedynczy krok symulacji — wołane przez pętlę gry w serwisie. */
    fun tick() {
        _state.update { GameEngine.tick(it) }
    }

    /** Zakłada przedmiot z inventory na bohatera i od razu zapisuje stan. */
    fun equip(heroId: Int, inventoryIndex: Int) {
        _state.update { Equipment.equip(it, heroId, inventoryIndex) }
        saveAsync()
    }

    fun saveAsync() {
        val file = saveFile ?: return
        val toSave = _state.updateAndGetTimestamped()
        ioScope.launch {
            try {
                file.writeText(GameSerializer.toJson(toSave))
            } catch (e: Exception) {
                Log.w(TAG, "Zapis stanu nie powiódł się", e)
            }
        }
    }

    /** Synchroniczny zapis (np. w onDestroy serwisu). */
    fun saveBlocking() {
        val file = saveFile ?: return
        try {
            file.writeText(GameSerializer.toJson(_state.updateAndGetTimestamped()))
        } catch (e: Exception) {
            Log.w(TAG, "Zapis stanu (blocking) nie powiódł się", e)
        }
    }

    /** Aktualizuje lastSeenTimestamp w stanie i zwraca świeży snapshot. */
    private fun MutableStateFlow<GameState>.updateAndGetTimestamped(): GameState {
        update { it.copy(lastSeenTimestamp = System.currentTimeMillis()) }
        return value
    }

    private const val TAG = "GameRepository"
    private const val SAVE_FILE = "savegame.json"
}
