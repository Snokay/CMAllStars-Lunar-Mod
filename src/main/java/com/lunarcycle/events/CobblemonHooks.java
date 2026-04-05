package com.lunarcycle.events;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lunarcycle.moon.MoonType;

/**
 * All Cobblemon-specific hooks live here.
 *
 * Cobblemon 1.7.x exposes:
 *  - CobblemonEvents.BATTLE_VICTORY       → award extra EXP
 *  - CobblemonEvents.POKEMON_ENTITY_SPAWN → tweak shiny / level after spawn
 *
 * We use the post-spawn event to retroactively force shiny/rare on spawn,
 * because it's the safest hook that doesn't require modifying spawn weights
 * (which would need a config reload).
 */
public class CobblemonHooks {

    private static MoonType activeMoon = null;

    /** Called once when the moon changes. Registers/adjusts buffs. */
    public static void applyMoonBuffs(MoonType moon) {
        activeMoon = moon;
    }

    /** Called at dawn to remove active buffs. */
    public static void removeMoonBuffs() {
        activeMoon = null;
    }

    /**
     * Register all Cobblemon event listeners.
     * Called once during mod init.
     */
    public static void registerEvents() {

        // ── Post-spawn: chance to force shiny / apply rare boost ─────────────
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event -> {
            if (activeMoon == null || !activeMoon.isSpecial()) return;
            PokemonEntity entity = event.getEntity();
            Pokemon pokemon = entity.getPokemon();

            // Shiny boost — shinyBoost is used directly as a % chance per spawn.
            // e.g. shinyBoost=3.0 → 3% chance to be shiny (vs vanilla ~0.024%)
            if (activeMoon.shinyBoost > 1.0f) {
                float boostedChance = activeMoon.shinyBoost / 100f;
                if (!pokemon.getShiny() && Math.random() < boostedChance) {
                    pokemon.setShiny(true);
                }
            }

            // Type-specific spawn boost — already spawned, we can't change the species,
            // but we can make them higher level (reward for boosted types)
            if (activeMoon.typeBoosts != null && !activeMoon.typeBoosts.isEmpty()) {
                boolean isBoostType = false;
                outer:
                for (var type : pokemon.getTypes()) {
                    String typeName = type.getName().toLowerCase();
                    for (String boosted : activeMoon.typeBoosts) {
                        if (typeName.contains(boosted.toLowerCase())) {
                            isBoostType = true;
                            break outer;
                        }
                    }
                }
                if (isBoostType && activeMoon.typeMulti > 1.0f) {
                    // Bump level slightly as a "power" indicator
                    int bonusLevels = (int) (activeMoon.typeMulti);
                    int newLevel = Math.min(100, pokemon.getLevel() + bonusLevels);
                    pokemon.setLevel(newLevel);
                }
            }
        });

        // ── Battle victory: apply EXP multiplier ─────────────────────────────
        CobblemonEvents.BATTLE_VICTORY.subscribe(event -> {
            if (activeMoon == null || !activeMoon.isSpecial() || activeMoon.expBoost <= 1.0f) return;

            // Cobblemon handles EXP distribution internally.
            // We hook the victory event and add bonus EXP to winning party.
            event.getWinners().forEach(actor -> {
                actor.getPokemonList().forEach(battlePokemon -> {
                    Pokemon pokemon = battlePokemon.getEffectedPokemon();
                    int bonusExp = (int)(50 * (activeMoon.expBoost - 1.0f));
                    pokemon.addExperience(new SidemodExperienceSource("lunarcycle"), bonusExp);
                });
            });
        });
    }

    public static MoonType getActiveMoon() {
        return activeMoon;
    }
}
