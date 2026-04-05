package com.lunarcycle;

import com.lunarcycle.config.LunarConfig;
import com.lunarcycle.events.CobblemonHooks;
import com.lunarcycle.moon.LunarManager;
import com.lunarcycle.moon.MoonType;
import com.lunarcycle.network.MoonSyncPacket;
import com.lunarcycle.util.LunarCommand;import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LunarCycleMod implements ModInitializer {

    public static final String MOD_ID = "lunarcycle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[LunarCycle] Initializing...");

        // 0. Load config then apply values to MoonType
        LunarConfig.load();
        MoonType.reloadFromConfig();

        // 1. Register network payload type (must be before any sending)
        MoonSyncPacket.register();

        // 2. Register Cobblemon event hooks (shiny, exp, spawn)
        CobblemonHooks.registerEvents();

        // 2. Register /moon command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            LunarCommand.register(dispatcher));

        // 3. Tick the lunar manager every server tick
        ServerTickEvents.END_SERVER_TICK.register(server ->
            LunarManager.get(server).tick());

        // 4. Sync moon to player on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            LunarManager.get(server).syncToPlayer(handler.getPlayer()));

        LOGGER.info("[LunarCycle] Ready! 7-moon cycle active.");
    }
}
