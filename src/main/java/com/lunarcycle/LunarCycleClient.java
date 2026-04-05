package com.lunarcycle;

import com.lunarcycle.client.LunarClientState;
import com.lunarcycle.moon.MoonType;
import com.lunarcycle.network.MoonSyncPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class LunarCycleClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register S→C moon sync packet
        ClientPlayNetworking.registerGlobalReceiver(MoonSyncPacket.ID, (payload, context) -> {
            MoonType moon = payload.moon();
            context.client().execute(() -> LunarClientState.setCurrentMoon(moon));
        });

        // Track night client-side (for sky color activation)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;
            long time = client.world.getTimeOfDay() % 24000;
            boolean night = time >= 13000 && time < 23000;
            LunarClientState.setNight(night);
        });
    }
}
