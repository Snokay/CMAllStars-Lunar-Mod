package com.lunarcycle.moon;

import com.lunarcycle.config.LunarConfig;
import java.util.List;

/**
 * All custom moon types + NORMAL (vanilla).
 *
 * Special moons appear on specific days of a 49-day cycle.
 * All other nights are NORMAL (no buffs, no sky tint).
 *
 * Schedule (1-indexed days for players):
 *   Day  7 → Azure Moon
 *   Day 13 → Amethyst Moon
 *   Day 19 → Golden Moon
 *   Day 26 → Emerald Moon
 *   Day 32 → Scarlet Moon
 *   Day 39 → Silver Moon
 *   Day 45 → Dark Moon
 *   (cycle repeats every 49 days)
 */
public enum MoonType {

    NORMAL(
        "lunarcycle.moon.normal.name",
        "lunarcycle.moon.normal.message",
        -1,     // no sky tint
        1.0f, 1.0f, 1.0f, null, 1.0f
    ),

    AZURE(
        "lunarcycle.moon.azure.name",
        "lunarcycle.moon.azure.message",
        0x5599DD,
        3.0f,   // shinyBoost — used as X% chance per spawn (3%)
        1.0f,
        1.0f,
        null,
        1.0f
    ),

    AMETHYST(
        "lunarcycle.moon.amethyst.name",
        "lunarcycle.moon.amethyst.message",
        0x7766CC,
        1.0f,
        3.0f,
        1.0f,
        null,
        1.0f
    ),

    GOLDEN(
        "lunarcycle.moon.golden.name",
        "lunarcycle.moon.golden.message",
        0xDDAA33,
        1.0f,
        1.0f,
        2.0f,
        null,
        1.0f
    ),

    EMERALD(
        "lunarcycle.moon.emerald.name",
        "lunarcycle.moon.emerald.message",
        0x33AA77,
        1.0f,
        1.5f,
        1.0f,
        List.of("grass"),
        2.5f
    ),

    SCARLET(
        "lunarcycle.moon.scarlet.name",
        "lunarcycle.moon.scarlet.message",
        0xCC3333,
        1.0f,
        1.5f,
        1.0f,
        List.of("fire", "fighting"),
        2.5f
    ),

    SILVER(
        "lunarcycle.moon.silver.name",
        "lunarcycle.moon.silver.message",
        0xAAAAAA,
        1.5f,
        1.0f,
        1.0f,
        null,
        1.0f
    ),

    DARK(
        "lunarcycle.moon.dark.name",
        "lunarcycle.moon.dark.message",
        0x111122,
        1.0f,
        2.0f,
        1.0f,
        List.of("ghost"),
        3.0f
    );

    // ── Fields ───────────────────────────────────────────────────────────────

    public final String nameKey;
    public final String messageKey;
    public int    skyFogColor;  // -1 = no tint
    public float  shinyBoost;   // % chance per spawn (e.g. 3.0 = 3%)
    public float  rareBoost;
    public float  expBoost;
    public List<String> typeBoosts;
    public float        typeMulti;

    MoonType(String nameKey, String messageKey, int skyFogColor,
             float shinyBoost, float rareBoost, float expBoost,
             List<String> typeBoosts, float typeMulti) {
        this.nameKey     = nameKey;
        this.messageKey  = messageKey;
        this.skyFogColor = skyFogColor;
        this.shinyBoost  = shinyBoost;
        this.rareBoost   = rareBoost;
        this.expBoost    = expBoost;
        this.typeBoosts  = typeBoosts;
        this.typeMulti   = typeMulti;
    }

    /** Returns true if this moon has special effects (not a vanilla night). */
    public boolean isSpecial() {
        return this != NORMAL;
    }

    // ── Schedule ─────────────────────────────────────────────────────────────

    /** Total cycle length in Minecraft days. */
    public static int CYCLE_LENGTH = 49;

    /**
     * Cycle-day index (0-based) on which each special moon occurs.
     * SPECIAL_MOONS[i] appears on day SPECIAL_DAYS[i] of the cycle.
     */
    private static int[] SPECIAL_DAYS = { 6, 12, 18, 25, 31, 38, 44 };

    private static MoonType[] specialMoons() {
        return new MoonType[]{ AZURE, AMETHYST, GOLDEN, EMERALD, SCARLET, SILVER, DARK };
    }

    /**
     * Applies values from LunarConfig.INSTANCE to all enum fields and schedule.
     * Call once after LunarConfig.load().
     */
    public static void reloadFromConfig() {
        LunarConfig cfg = LunarConfig.INSTANCE;
        if (cfg == null) return;

        CYCLE_LENGTH = cfg.cycleLength;

        MoonType[] ordered = specialMoons();
        int[] newDays = new int[ordered.length];

        for (int i = 0; i < ordered.length; i++) {
            MoonType moon = ordered[i];
            LunarConfig.MoonConfig mc = cfg.moons.get(moon.name());
            if (mc == null) {
                newDays[i] = SPECIAL_DAYS[i];
                continue;
            }
            String color = mc.skyColor;
            moon.skyFogColor = (color != null && !color.isEmpty() && !color.equalsIgnoreCase("none"))
                ? (int) Long.parseLong(color, 16) : -1;
            moon.shinyBoost = mc.shinyBoost;
            moon.rareBoost  = mc.rareBoost;
            moon.expBoost   = mc.expBoost;
            moon.typeBoosts = (mc.typeBoosts != null && !mc.typeBoosts.isEmpty()) ? mc.typeBoosts : null;
            moon.typeMulti  = mc.typeMulti;
            newDays[i]      = mc.cycleDay - 1; // config is 1-indexed, internal is 0-indexed
        }

        SPECIAL_DAYS = newDays;
    }

    /**
     * Returns the active MoonType for a given absolute Minecraft day.
     * Returns NORMAL on nights with no special moon.
     */
    public static MoonType forDay(long absoluteDay) {
        int dayInCycle = (int)(absoluteDay % CYCLE_LENGTH);
        MoonType[] moons = specialMoons();
        for (int i = 0; i < SPECIAL_DAYS.length; i++) {
            if (dayInCycle == SPECIAL_DAYS[i]) return moons[i];
        }
        return NORMAL;
    }

    /**
     * Returns how many days until the next special moon (minimum 1 if tonight is special,
     * counting from tomorrow).
     */
    public static int daysUntilNextSpecialMoon(long absoluteDay) {
        int dayInCycle = (int)(absoluteDay % CYCLE_LENGTH);
        for (int offset = 1; offset <= CYCLE_LENGTH; offset++) {
            int check = (dayInCycle + offset) % CYCLE_LENGTH;
            for (int d : SPECIAL_DAYS) {
                if (check == d) return offset;
            }
        }
        return CYCLE_LENGTH; // should never happen
    }

    /** Returns the next upcoming special moon (skipping today if today is special). */
    public static MoonType nextSpecialMoon(long absoluteDay) {
        int dayInCycle = (int)(absoluteDay % CYCLE_LENGTH);
        MoonType[] moons = specialMoons();
        for (int offset = 1; offset <= CYCLE_LENGTH; offset++) {
            int check = (dayInCycle + offset) % CYCLE_LENGTH;
            for (int i = 0; i < SPECIAL_DAYS.length; i++) {
                if (check == SPECIAL_DAYS[i]) return moons[i];
            }
        }
        return AZURE; // fallback
    }

    /** Returns the 1-indexed cycle day on which this special moon occurs, or -1 if NORMAL. */
    public int cycleDay() {
        MoonType[] moons = specialMoons();
        for (int i = 0; i < moons.length; i++) {
            if (moons[i] == this) return SPECIAL_DAYS[i] + 1;
        }
        return -1;
    }
}
