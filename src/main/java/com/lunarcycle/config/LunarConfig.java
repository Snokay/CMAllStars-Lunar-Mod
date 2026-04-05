package com.lunarcycle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lunarcycle.LunarCycleMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and saves config/lunarcycle.json.
 * Call LunarConfig.load() before MoonType is used.
 */
public class LunarConfig {

    public static LunarConfig INSTANCE;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Top-level fields ─────────────────────────────────────────────────────

    /** Total length of the moon cycle in Minecraft days. */
    public int cycleLength = 49;

    /**
     * One entry per special moon (key = enum name: AZURE, AMETHYST, GOLDEN,
     * EMERALD, SCARLET, SILVER, DARK).
     */
    public Map<String, MoonConfig> moons = new LinkedHashMap<>();

    // ── Per-moon config ───────────────────────────────────────────────────────

    public static class MoonConfig {
        /** 1-indexed day within the cycle on which this moon appears. */
        public int    cycleDay;
        /** Sky/fog tint colour as a hex string (e.g. "5599DD"). Use "none" or "" for no tint. */
        public String skyColor;
        /** Extra shiny-spawn chance added to the base rate (e.g. 3.0 = +3 %). */
        public float  shinyBoost;
        /** Multiplier applied to rare-Pokémon spawn weight (e.g. 3.0 = ×3). */
        public float  rareBoost;
        /** Multiplier applied to battle EXP gain (e.g. 2.0 = ×2). */
        public float  expBoost;
        /** Cobblemon types whose spawn weight is boosted (e.g. ["fire", "fighting"]). null/empty = none. */
        public List<String> typeBoosts;
        /** Multiplier applied to the boosted types' spawn weight. */
        public float  typeMulti;
    }

    // ── Load / save ───────────────────────────────────────────────────────────

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("cobblemon-allstars/cmallstar_lunarcycle.json");

        try {
            Files.createDirectories(path.getParent());
        } catch (Exception e) {
            LunarCycleMod.LOGGER.error("[LunarCycle] Failed to create config directory: {}", e.getMessage());
        }
        if (!Files.exists(path)) {
            INSTANCE = defaults();
            save(path);
            LunarCycleMod.LOGGER.info("[LunarCycle] Config not found — created default config at {}", path);
        } else {
            try (Reader r = Files.newBufferedReader(path)) {
                INSTANCE = GSON.fromJson(r, LunarConfig.class);
                LunarCycleMod.LOGGER.info("[LunarCycle] Config loaded from {}", path);
            } catch (Exception e) {
                LunarCycleMod.LOGGER.error("[LunarCycle] Failed to read config, using defaults. Error: {}", e.getMessage());
                INSTANCE = defaults();
            }
        }
    }

    private static void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Exception e) {
            LunarCycleMod.LOGGER.error("[LunarCycle] Failed to write config: {}", e.getMessage());
        }
    }

    // ── Default values (mirror of hardcoded values) ───────────────────────────

    private static LunarConfig defaults() {
        LunarConfig cfg = new LunarConfig();
        cfg.cycleLength = 49;

        cfg.moons.put("AZURE",    moon(7,  "5599DD", 3.0f, 1.0f, 1.0f, 1.0f));
        cfg.moons.put("AMETHYST", moon(13, "7766CC", 1.0f, 3.0f, 1.0f, 1.0f));
        cfg.moons.put("GOLDEN",   moon(19, "DDAA33", 1.0f, 1.0f, 2.0f, 1.0f));
        cfg.moons.put("EMERALD",  moon(26, "33AA77", 1.0f, 1.5f, 1.0f, 2.5f, "grass"));
        cfg.moons.put("SCARLET",  moon(32, "CC3333", 1.0f, 1.5f, 1.0f, 2.5f, "fire", "fighting"));
        cfg.moons.put("SILVER",   moon(39, "AAAAAA", 1.5f, 1.0f, 1.0f, 1.0f));
        cfg.moons.put("DARK",     moon(45, "111122", 1.0f, 2.0f, 1.0f, 3.0f, "ghost"));

        return cfg;
    }

    private static MoonConfig moon(int day, String color,
                                   float shiny, float rare, float exp,
                                   float multi, String... types) {
        MoonConfig m = new MoonConfig();
        m.cycleDay   = day;
        m.skyColor   = color;
        m.shinyBoost = shiny;
        m.rareBoost  = rare;
        m.expBoost   = exp;
        m.typeBoosts = (types != null && types.length > 0) ? List.of(types) : null;
        m.typeMulti  = multi;
        return m;
    }
}
