package com.lunarcycle.moon;

import com.lunarcycle.events.CobblemonHooks;
import com.lunarcycle.network.MoonSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Central server-side manager.
 * Ticked every server tick — detects moon transitions and applies buffs.
 */
public class LunarManager {

    private static LunarManager INSTANCE;

    private final MinecraftServer server;
    private MoonType currentMoon    = null;
    private MoonType moonOverride   = null; // set by /moon set, cleared at dawn
    private boolean  isNight        = false;
    private boolean  announcedNight = false;
    private boolean  wasCustomNight = false; // track if current night had a special moon

    private LunarManager(MinecraftServer server) {
        this.server = server;
    }

    public static LunarManager get(MinecraftServer server) {
        if (INSTANCE == null || INSTANCE.server != server) {
            INSTANCE = new LunarManager(server);
        }
        return INSTANCE;
    }

    public static MoonType getCurrentMoon() {
        return INSTANCE != null ? INSTANCE.currentMoon : null;
    }

    // ── Override (used by /moon set) ─────────────────────────────────────────

    /**
     * Forces a specific moon for the current night.
     * Clears automatically at dawn.
     * Requires permission level 2 (enforced by the command).
     */
    public void setOverride(MoonType moon) {
        moonOverride = moon;
        applyMoon(moon);
    }

    /** Removes the forced moon override, reverting to the scheduled moon. */
    public void clearOverride() {
        moonOverride = null;
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld != null) {
            long absoluteDay = overworld.getTimeOfDay() / 24000L;
            applyMoon(MoonType.forDay(absoluteDay));
        }
    }

    // ── Server tick ──────────────────────────────────────────────────────────

    public void tick() {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return;

        long time        = overworld.getTimeOfDay();
        long absoluteDay = time / 24000L;

        // Use override if active, otherwise follow the schedule
        MoonType moon = moonOverride != null ? moonOverride : MoonType.forDay(absoluteDay);

        if (moon != currentMoon) {
            applyMoon(moon);
        }

        // Night: ticks 13000–23000 (sunset to sunrise)
        boolean nowNight = (time % 24000) >= 13000 && (time % 24000) < 23000;

        if (nowNight && !isNight) {
            isNight        = true;
            announcedNight = false;
        }

        if (nowNight && !announcedNight) {
            long tickInNight = (time % 24000) - 13000;
            if (tickInNight >= 40) { // ~2 seconds after nightfall
                if (moon.isSpecial()) {
                    announceNight(moon);
                    wasCustomNight = true;
                }
                announcedNight = true;
            }
        }

        if (!nowNight && isNight) {
            // Dawn
            isNight = false;
            moonOverride = null; // override lasts one night only
            CobblemonHooks.removeMoonBuffs();
            if (wasCustomNight) {
                sendDawnMessage();
                wasCustomNight = false;
            }
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void applyMoon(MoonType moon) {
        currentMoon = moon;
        CobblemonHooks.applyMoonBuffs(moon);
        syncToAllClients();
    }

    // ── Announcements ────────────────────────────────────────────────────────

    private void announceNight(MoonType moon) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(buildNightMessage(moon), false);
        }
    }

    private void sendDawnMessage() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(
                Text.translatable("lunarcycle.dawn.message")
                    .formatted(Formatting.YELLOW),
                false
            );
        }
    }

    private Text buildNightMessage(MoonType moon) {
        return Text.empty()
            .append(Text.literal("★ ").formatted(Formatting.WHITE))
            .append(Text.translatable(moon.nameKey).formatted(moonFormatting(moon)))
            .append(Text.literal(" — ").formatted(Formatting.GRAY))
            .append(Text.translatable(moon.messageKey).formatted(Formatting.ITALIC, Formatting.WHITE));
    }

    private Formatting moonFormatting(MoonType moon) {
        return switch (moon) {
            case AZURE    -> Formatting.AQUA;
            case AMETHYST -> Formatting.LIGHT_PURPLE;
            case GOLDEN   -> Formatting.GOLD;
            case EMERALD  -> Formatting.GREEN;
            case SCARLET  -> Formatting.RED;
            case SILVER   -> Formatting.GRAY;
            case DARK     -> Formatting.DARK_PURPLE;
            default       -> Formatting.WHITE;
        };
    }

    // ── Client sync ──────────────────────────────────────────────────────────

    private void syncToAllClients() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            MoonSyncPacket.send(player, currentMoon);
        }
    }

    public void syncToPlayer(ServerPlayerEntity player) {
        if (currentMoon != null) {
            MoonSyncPacket.send(player, currentMoon);
        }
    }
}
