# Specyfikacja gry — idle RPG w nakładce na Androida

> Dokument projektowy do wykorzystania przy budowie gry z Claude Code.
> Zawiera wizję, decyzje projektowe, stos technologiczny, architekturę i plan MVP krok po kroku.

---

## 1. Wizja w jednym zdaniu

Pływająca, przesuwalna nakładka na Androidzie, w której drużyna kreskówkowych, heroicznych bohaterów automatycznie walczy z falami potworów, przebijając się przez kolejne strefy nieskończonego świata — a gracz zerka kątem oka, czasem coś dotyka i rozwija widget w pełny ekran, by zarządzać drużyną i łupem.

Inspiracja: *TBH: Task Bar Hero* (idle RPG żyjący na pasku zadań Windows), ale przeniesione na telefon i działające jako ruchoma nakładka nad innymi aplikacjami.

---

## 2. Filary gry

- **Klimat:** kreskówkowe fantasy, ton epicki i heroiczny.
- **Bohaterowie:** drużyna kilku postaci (na start stała, np. 3 sloty).
- **Sedno zabawy:** stawanie się coraz silniejszym.
- **Tryb:** głównie automatyczny, z okazjonalnym dotykiem gracza (umiejętność, otwarcie skrzyni).
- **Struktura:** nieskończona (świat skaluje się bez końca, brak zakończenia).
- **Postęp offline:** umiarkowany — trochę nalicza się w tle, ale sesje na żywo są wartościowsze.

---

## 3. Pętla rozgrywki

1. Bohaterowie auto-walczą z falami potworów.
2. Pokonani wrogowie dropią złoto, przedmioty i skrzynie.
3. Łup, poziomy i umiejętności wzmacniają drużynę.
4. Silniejsza drużyna wchodzi w dalsze, trudniejsze strefy.
5. Nowi bohaterowie poszerzają skład i otwierają nowe taktyki.

### Cztery filary mocy

- **Ekwipunek / loot** — bronie i zbroje o różnych rzadkościach.
- **Poziomy postaci** — zdobywane z doświadczenia.
- **Umiejętności / drzewka** — rozwój zdolności bohaterów.
- **Kolekcja bohaterów** — odblokowywanie i dobieranie składu.

---

## 4. Nakładka i UX

- Nakładka jest **pływająca i przesuwalna** — gracz może ją ustawić w dowolnym miejscu ekranu i przeciągać palcem.
- Działa **nad innymi aplikacjami** (uprawnienie „Wyświetlanie nad innymi aplikacjami").
- W zwiniętej formie pokazuje małą scenę walki (bohaterowie, potwór, paski HP, licznik fal/strefy).
- **Stuknięcie** rozwija widget w **pełny ekran** z menu: drużyna, ekwipunek, drzewka umiejętności, bohaterowie, strefy.
- Wydajność/bateria: scena nie potrzebuje 60 fps. Cel ~10–15 fps rysowania, logika tykana kilka razy na sekundę; gdy widget zwinięty lub w tle — rzadziej.

---

## 5. Monetyzacja

- Model: **darmowa z mikropłatnościami.**
- Sprzedajemy: **kosmetykę (skiny, wygląd)** oraz **sloty / odblokowania bohaterów.**
- Zasada: **bez sprzedaży czystej mocy** (uczciwy model, brak pay-to-win).
- Implementacja: Google Play Billing — **poza MVP.**

---

## 6. Zakres etapów

### Etap 1 — MVP (w pełni offline)
Nakładka, auto-walka drużyny z falami, kilka stref ze skalującą się trudnością, podstawowy loot + poziomy + proste ulepszenia ekwipunku, zapis i umiarkowany postęp offline, rozwijanie widgetu w pełny ekran z prostym menu. Art zastępczy, drużyna stała.

**Poza MVP:** rozbudowane drzewka umiejętności, kolekcja wielu bohaterów do odblokowania, skiny, mikropłatności, rankingi.

### Etap 2
Rankingi przez Google Play Games Services (Leaderboards — bez własnego backendu), skiny i sloty bohaterów, płatności przez Google Play Billing.

### Dalej (opcjonalnie)
Rozbudowa zawartości, więcej bohaterów, drzewka umiejętności, ewentualne dalsze funkcje online.

> **Świadoma decyzja:** online ograniczamy na razie wyłącznie do rankingów. Rezygnujemy (przynajmniej na start) z handlu między graczami — to ogromny kawałek (backend, ekonomia, walka z oszustami) i nieproporcjonalnie zwiększa ryzyko projektu.

---

## 7. Stos technologiczny

- **Język/baza:** Kotlin + natywny Android SDK. Min SDK 26 (Android 8, wymóg `TYPE_APPLICATION_OVERLAY`); target najnowszy stabilny.
- **Nakładka:** `WindowManager` + własny widok z flagą `TYPE_APPLICATION_OVERLAY`, utrzymywany przez **foreground service** z trwałym powiadomieniem. Zgoda przez `Settings.canDrawOverlays`.
- **Scena walki (w widgecie):** `Canvas` / `SurfaceView` z własną pętlą rysowania (lekkie, oszczędne, pełna kontrola).
- **Menu pełnoekranowe:** Jetpack Compose (szybkie budowanie list, zakładek, statystyk).
- **Rdzeń gry:** osobny moduł `:core` w czystym Kotlinie, bez zależności od Androida.
- **Współbieżność:** Kotlin Coroutines + Flow. Pętla gry w korutynie w serwisie, stały krok czasowy.
- **Stan dla UI:** jedno źródło prawdy — `StateFlow<GameState>`, obserwowane przez nakładkę i menu.
- **Zapis:** Kotlinx Serialization → plik JSON.
- **Narzędzia:** Android Studio, Gradle (Kotlin DSL), Git.

---

## 8. Architektura

### Moduły

- **`:core`** — czysta logika gry, zero zależności od Androida. Zawiera `GameState`, `tick()`, reguły walki, loot, progresję, serializację. Testowalny zwykłym JUnit.
- **`:app`** — warstwa Androida: foreground service, nakładka (`WindowManager` + `SurfaceView`), menu w Compose, zapis na dysk, obsługa uprawnień.

### Zasada „gotowe na online"

Cała logika musi być **deterministyczna** i opierać się na czystej funkcji:

```
tick(state: GameState, ticks: Int): GameState
```

Dzięki temu:
- ten sam stan + ta sama liczba ticków zawsze daje ten sam wynik,
- postęp offline = „przewinięcie" symulacji do przodu o miniony czas (z limitem),
- późniejsze przeniesienie liczenia na serwer (gdyby było potrzebne) nie wymaga przepisywania gry.

### Szkic modelu danych (do doprecyzowania w kodzie)

```
GameState
├── party: List<Hero>
│   └── Hero { id, klasa, poziom, xp, statystyki, equipped: List<Item> }
├── inventory: List<Item>
│   └── Item { id, typ, rzadkość, statystyki }
├── currentZone: Int
├── currentWave: Int
├── gold: Long
├── lastSeenTimestamp: Long   // do postępu offline
└── rngSeed: Long             // deterministyczny RNG
```

### Źródło prawdy i przepływ

`GameLoopService` (foreground) trzyma `StateFlow<GameState>` → nakładka (`SurfaceView`) i menu (Compose) tylko **obserwują** ten stan i go renderują. Zmiany stanu zawsze przez `tick()` lub akcje gracza, nigdy bezpośrednio z UI.

---

## 9. Plan MVP — krok po kroku

Kolejność rozbraja najpierw największe ryzyko. Każdy krok kończy się czymś działającym.

1. **Setup.** Projekt w Android Studio (Kotlin), repo git, moduły `:core` i `:app`. Pusta apka, która się uruchamia.
2. **Rozbroić nakładkę (krok krytyczny).** Foreground service + okno przez `WindowManager` z `TYPE_APPLICATION_OVERLAY` + obsługa zgody `canDrawOverlays` + przeciąganie palcem. Cel: przesuwalny kwadrat trzymający się ekranu nad innymi aplikacjami.
3. **Mały świat w widgecie.** Zamiast kwadratu — `SurfaceView` z zastępczymi bohaterami i potworem, napędzany pętlą gry (tick) w serwisie.
4. **Rdzeń walki (czysta logika w `:core`).** Deterministyczna symulacja: auto-ataki, fale potworów, HP, obrażenia, śmierć, licznik fal, przechodzenie stref. `GameState` + `tick()`, niezależnie od rysowania. Pokryć testami JUnit.
5. **Loot i progresja.** Drop złota/przedmiotów, XP i poziomy bohaterów, prosty ekwipunek podbijający statystyki. Domyka pętlę „rośnij w siłę".
6. **Zapis i offline.** Serializacja `GameState` do JSON; przy wejściu liczenie minionego czasu i naliczenie umiarkowanego postępu offline (z limitem) przez przewinięcie symulacji.
7. **Pełny ekran.** Stuknięcie widgetu otwiera `Activity` w Compose z zakładkami: Drużyna (statystyki, zakładanie sprzętu), Ekwipunek (lista lootu), Strefy. Czyta i zapisuje ten sam `GameState`.
8. **Pierwsze szlify i test.** Onboarding zgody, start/stop nakładki, zgrubny balans tak, by pętla wciągała przez ~30–60 min. Playtest → decyzja, czy rdzeń jest fajny → dopiero wtedy Etap 2.

---

## 10. Pułapki i ograniczenia (ważne!)

- **Foreground service na Androidzie 14+** wymaga zadeklarowania *typu* usługi w manifeście — pominięcie powoduje crash przy starcie.
- **Bateria/wydajność:** nakładka działa cały czas. Trzymać niski fps i rzadkie ticki logiki; ograniczać pracę, gdy widget zwinięty lub aplikacja w tle.
- **Uprawnienie do nakładki** musi przyznać użytkownik ręcznie w ustawieniach systemu — potrzebny przejrzysty onboarding.
- **iOS odpada** dla tego pomysłu — system nie pozwala rysować nad innymi aplikacjami. Projekt celuje wyłącznie w Androida.
- **Wersje bibliotek** (Compose, Billing, Play Games) zmieniają się szybko — sprawdzać aktualną dokumentację, nie polegać na utrwalonych wartościach.
- **Rankingi (Etap 2)** wymagają konta Google Play Console (jednorazowy koszt ~$25).
- **Losowe skrzynie + płatności:** projektując monetyzację, mieć z tyłu głowy przepisy o lootboksach/ochronie konsumenta w docelowych krajach.

---

## 11. Jak używać tego dokumentu z Claude Code

- Zacznij od kroków 1–2 z sekcji 9 — to serce ryzyka; gdy nakładka działa, reszta idzie szybciej.
- Pilnuj separacji: cała logika gry w `:core` bez importów Androida; `:app` tylko renderuje i obsługuje system.
- Każdą regułę walki/lootu/progresji pisz jako czystą funkcję i pokrywaj testami w `:core`.
- Trzymaj jedno źródło prawdy (`StateFlow<GameState>`); UI nigdy nie modyfikuje stanu w obejściu `tick()`/akcji.
- Realizuj etapami: najpierw grywalne offline MVP, dopiero potem rankingi i monetyzacja.
