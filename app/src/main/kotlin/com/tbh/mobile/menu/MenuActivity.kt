package com.tbh.mobile.menu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbh.core.GameState
import com.tbh.core.Hero
import com.tbh.core.Item
import com.tbh.core.Rarity
import com.tbh.mobile.overlay.displayName
import com.tbh.mobile.state.GameRepository
import com.tbh.mobile.state.MenuVisibility
import com.tbh.mobile.ui.theme.TBHMobileTheme

/**
 * Pełnoekranowe menu otwierane tapnięciem nakładki.
 * Czyta i modyfikuje ten sam GameState co OverlayService — przez GameRepository.state (StateFlow).
 */
class MenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TBHMobileTheme {
                val state by GameRepository.state.collectAsState()
                MenuScreen(state)
            }
        }
    }

    // Chowanie nakładki na czas, gdy menu jest widoczne.
    override fun onStart() {
        super.onStart()
        MenuVisibility.setOpen(true)
    }

    override fun onStop() {
        super.onStop()
        MenuVisibility.setOpen(false)
    }
}

private val TABS = listOf("Drużyna", "Ekwipunek", "Strefy")

@Composable
private fun MenuScreen(state: GameState) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                TABS.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> TeamScreen(state)
                1 -> InventoryScreen(state)
                else -> ZonesScreen(state)
            }
        }
    }
}

// ── Drużyna ─────────────────────────────────────────────────────────────────

@Composable
private fun TeamScreen(state: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(state.heroes) { _, hero -> HeroCard(hero) }
    }
}

@Composable
private fun HeroCard(hero: Hero) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(hero.heroClass.displayName(), fontWeight = FontWeight.Bold)
                Text("Poziom ${hero.level}")
            }
            Spacer(Modifier.height(6.dp))

            val threshold = 100 * hero.level
            val xpFraction = (hero.xp.toFloat() / threshold).coerceIn(0f, 1f)
            Text("XP: ${hero.xp} / $threshold", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { xpFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp)
            )

            Spacer(Modifier.height(6.dp))
            Text("HP: ${hero.hp} / ${hero.effectiveMaxHp}", style = MaterialTheme.typography.bodySmall)
            Text("Atak: ${hero.effectiveAttack}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Ekwipunek: ${hero.equippedItem?.let { "${it.rarity.displayName()} ${it.name}" } ?: "brak"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ── Ekwipunek ───────────────────────────────────────────────────────────────

@Composable
private fun InventoryScreen(state: GameState) {
    // Indeks przedmiotu wybranego do założenia (otwiera dialog wyboru bohatera).
    var pendingIndex by remember { mutableStateOf<Int?>(null) }

    if (state.inventory.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Brak przedmiotów — pokonuj potwory, by zdobyć loot.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(state.inventory) { index, item ->
            ItemCard(item) { pendingIndex = index }
        }
    }

    val idx = pendingIndex
    if (idx != null) {
        EquipHeroDialog(
            heroes = state.heroes,
            onDismiss = { pendingIndex = null },
            onPick = { heroId ->
                GameRepository.equip(heroId, idx)
                pendingIndex = null
            }
        )
    }
}

@Composable
private fun ItemCard(item: Item, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = item.rarity.color()),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.name, fontWeight = FontWeight.Bold, color = Color.White)
            Text(item.rarity.displayName(), color = Color.White, style = MaterialTheme.typography.bodySmall)
            val bonus = buildList {
                if (item.attackBonus > 0) add("+${item.attackBonus} atak")
                if (item.hpBonus > 0) add("+${item.hpBonus} HP")
            }.joinToString(", ").ifEmpty { "brak bonusu" }
            Text(bonus, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EquipHeroDialog(heroes: List<Hero>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
        title = { Text("Załóż na bohatera") },
        text = {
            Column {
                heroes.forEach { hero ->
                    TextButton(
                        onClick = { onPick(hero.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${hero.heroClass.displayName()} (poz. ${hero.level})")
                    }
                }
            }
        }
    )
}

// ── Strefy ──────────────────────────────────────────────────────────────────

@Composable
private fun ZonesScreen(state: GameState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Postęp", style = MaterialTheme.typography.titleLarge)
        Text("Aktualna fala: ${state.wave}")
        Text("Aktualna strefa: ${state.zone}")
        Text("Najwyższa osiągnięta fala: ${state.highestWave}")
        Text("Przeciwnik: ${state.monster.name}")
    }
}

// ── Pomocnicze ──────────────────────────────────────────────────────────────

private fun Rarity.color(): Color = when (this) {
    Rarity.COMMON -> Color(0xFF607D8B)   // szaroniebieski
    Rarity.RARE   -> Color(0xFF1976D2)   // niebieski
    Rarity.EPIC   -> Color(0xFF7B1FA2)   // fiolet
}
