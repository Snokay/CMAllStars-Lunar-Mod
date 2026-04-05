# Lunar Cycle — Cobblemon All Stars

Adds a **49-day moon cycle** with 7 special moons that each bring unique bonuses to Cobblemon gameplay — shiny boosts, rare spawns, EXP multipliers, and type-specific spawn buffs. The sky and fog change color to match the active moon.

---

## Requirements

- Minecraft **1.21.1**
- Fabric Loader **≥ 0.16.5**
- Fabric API
- Cobblemon **1.7.3+**

---

## Installation

Drop the jar into your `mods/` folder.

---

## Moon Schedule (default — 49-day cycle)

| Day | Moon | Sky Color | Effect |
|-----|------|-----------|--------|
| 7 | 🌙 Azure Moon | Blue `#5599DD` | +3% shiny spawn chance |
| 13 | 🌙 Amethyst Moon | Purple `#7766CC` | ×3 rare Pokémon spawn weight |
| 19 | 🌙 Golden Moon | Gold `#DDAA33` | ×2 EXP from battles |
| 26 | 🌙 Emerald Moon | Green `#33AA77` | ×2.5 Grass-type spawn weight |
| 32 | 🌙 Scarlet Moon | Red `#CC3333` | ×2.5 Fire & Fighting spawn weight |
| 39 | 🌙 Silver Moon | Silver `#AAAAAA` | +1.5% shiny spawn chance |
| 45 | 🌙 Dark Moon | Dark `#111122` | ×3 Ghost-type spawn weight, ×2 rare weight |

All other nights are normal — no effects, no sky tint.

---

## Configuration

File: `config/cobblemon-allstars/cmallstar_lunarcycle.json` (auto-generated on first launch)

```json
{
  "cycleLength": 49,
  "moons": {
    "AZURE": {
      "cycleDay": 7,
      "skyColor": "5599DD",
      "shinyBoost": 3.0,
      "rareBoost": 1.0,
      "expBoost": 1.0,
      "typeBoosts": null,
      "typeMulti": 1.0
    },
    ...
  }
}
```

| Key | Description |
|-----|-------------|
| `cycleLength` | Total length of the cycle in Minecraft days |
| `cycleDay` | Day within the cycle this moon appears (1-indexed) |
| `skyColor` | Hex color string for the sky/fog tint (`"none"` to disable) |
| `shinyBoost` | Extra shiny chance added per spawn (e.g. `3.0` = +3%) |
| `rareBoost` | Multiplier on rare Pokémon spawn weight |
| `expBoost` | Multiplier on battle EXP |
| `typeBoosts` | List of Cobblemon types boosted (e.g. `["fire", "ghost"]`) |
| `typeMulti` | Spawn weight multiplier for the boosted types |

---

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/moon` | Player | Show current moon, effects, and days until next special moon |
| `/moon set <type>` | OP | Force a specific moon (NORMAL, AZURE, AMETHYST, GOLDEN, EMERALD, SCARLET, SILVER, DARK) |
| `/moon reload` | OP | Reload config from disk |
