package com.lunarcycle.client;

import com.lunarcycle.moon.MoonType;

/**
 * Client-only singleton that holds the moon received from the server.
 * Read by SkyColorMixin to tint sky/fog.
 */
public class LunarClientState {

    private static MoonType currentMoon = null;
    private static boolean  isNight     = false;

    public static void setCurrentMoon(MoonType moon) {
        currentMoon = moon;
    }

    public static MoonType getCurrentMoon() {
        return currentMoon;
    }

    public static void setNight(boolean night) {
        isNight = night;
    }

    public static boolean isNight() {
        return isNight;
    }

    /**
     * Returns the fog/sky RGB color for the current moon, or -1 if no tint.
     * Only active at night.
     */
    public static int getSkyTintColor() {
        if (!isNight || currentMoon == null) return -1;
        return currentMoon.skyFogColor;
    }
}
