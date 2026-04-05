package com.lunarcycle.util;

import com.lunarcycle.moon.LunarManager;
import com.lunarcycle.moon.MoonType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class LunarCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("moon")

                // /moon — shows current moon and next special moon
                .executes(ctx -> {
                    ServerWorld overworld = ctx.getSource().getServer().getWorld(World.OVERWORLD);
                    if (overworld == null) return 0;

                    long absoluteDay = overworld.getTimeOfDay() / 24000L;
                    MoonType moon    = MoonType.forDay(absoluteDay);
                    int dayInCycle   = (int)(absoluteDay % MoonType.CYCLE_LENGTH);

                    ctx.getSource().sendFeedback(() ->
                        Text.empty()
                            .append(Text.literal("═══ ").formatted(Formatting.DARK_GRAY))
                            .append(Text.translatable("lunarcycle.command.current_moon").formatted(Formatting.WHITE))
                            .append(Text.literal(" ═══").formatted(Formatting.DARK_GRAY)),
                        false
                    );

                    if (moon.isSpecial()) {
                        // Special moon tonight
                        ctx.getSource().sendFeedback(() ->
                            Text.empty()
                                .append(Text.literal("  ★ ").formatted(Formatting.YELLOW))
                                .append(Text.translatable(moon.nameKey).formatted(Formatting.AQUA))
                                .append(Text.literal("  (tonight only)").formatted(Formatting.DARK_GRAY)),
                            false
                        );

                        if (moon.shinyBoost > 1f) {
                            ctx.getSource().sendFeedback(() ->
                                Text.literal("  ✦ Shiny chance +" + moon.shinyBoost + "%").formatted(Formatting.AQUA),
                                false
                            );
                        }
                        if (moon.rareBoost > 1f) {
                            ctx.getSource().sendFeedback(() ->
                                Text.literal("  ✦ Rare spawn ×" + moon.rareBoost).formatted(Formatting.LIGHT_PURPLE),
                                false
                            );
                        }
                        if (moon.expBoost > 1f) {
                            ctx.getSource().sendFeedback(() ->
                                Text.literal("  ✦ EXP ×" + moon.expBoost).formatted(Formatting.GOLD),
                                false
                            );
                        }
                        if (moon.typeBoosts != null && !moon.typeBoosts.isEmpty()) {
                            ctx.getSource().sendFeedback(() ->
                                Text.literal("  ✦ " + String.join(", ", moon.typeBoosts) + " type spawn ×" + moon.typeMulti)
                                    .formatted(Formatting.GREEN),
                                false
                            );
                        }
                    } else {
                        // Normal night
                        ctx.getSource().sendFeedback(() ->
                            Text.literal("  Normal night — no special moon").formatted(Formatting.GRAY),
                            false
                        );
                    }

                    // Always show next special moon
                    MoonType nextMoon = MoonType.nextSpecialMoon(absoluteDay);
                    int daysUntil    = MoonType.daysUntilNextSpecialMoon(absoluteDay);
                    ctx.getSource().sendFeedback(() ->
                        Text.empty()
                            .append(Text.literal("  Next : ").formatted(Formatting.GRAY))
                            .append(Text.translatable(nextMoon.nameKey).formatted(Formatting.YELLOW))
                            .append(Text.literal(" in " + daysUntil + " day" + (daysUntil == 1 ? "" : "s")).formatted(Formatting.GRAY)),
                        false
                    );

                    ctx.getSource().sendFeedback(() ->
                        Text.literal("  Cycle day : " + (dayInCycle + 1) + " / " + MoonType.CYCLE_LENGTH)
                            .formatted(Formatting.DARK_GRAY),
                        false
                    );

                    return 1;
                })

                // /moon cycle — shows full 49-day schedule
                .then(CommandManager.literal("cycle")
                    .executes(ctx -> {
                        ServerWorld overworld = ctx.getSource().getServer().getWorld(World.OVERWORLD);
                        if (overworld == null) return 0;
                        long absoluteDay = overworld.getTimeOfDay() / 24000L;
                        MoonType current = MoonType.forDay(absoluteDay);

                        ctx.getSource().sendFeedback(() ->
                            Text.literal("═══ Moon Schedule (49-day cycle) ═══").formatted(Formatting.GOLD),
                            false
                        );

                        MoonType[] specials = new MoonType[]{
                            MoonType.AZURE, MoonType.AMETHYST, MoonType.GOLDEN,
                            MoonType.EMERALD, MoonType.SCARLET, MoonType.SILVER, MoonType.DARK
                        };
                        for (MoonType m : specials) {
                            boolean active = (m == current);
                            ctx.getSource().sendFeedback(() ->
                                Text.empty()
                                    .append(Text.literal(active ? "  ► " : "    ").formatted(Formatting.WHITE))
                                    .append(Text.translatable(m.nameKey)
                                        .formatted(active ? Formatting.YELLOW : Formatting.GRAY))
                                    .append(Text.literal("  (day " + m.cycleDay() + ")")
                                        .formatted(Formatting.DARK_GRAY)),
                                false
                            );
                        }
                        return 1;
                    })
                )

                // /moon set <moon> — op/cheats only — force a moon for tonight
                .then(CommandManager.literal("set")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("moon", StringArgumentType.word())
                        .executes(ctx -> {
                            String input = StringArgumentType.getString(ctx, "moon").toUpperCase();
                            MoonType target;
                            try {
                                target = MoonType.valueOf(input);
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFeedback(() ->
                                    Text.literal("[LunarCycle] Unknown moon: \"" + input.toLowerCase() + "\". "
                                        + "Valid: azure, amethyst, golden, emerald, scarlet, silver, dark, normal")
                                        .formatted(Formatting.RED),
                                    false
                                );
                                return 0;
                            }

                            LunarManager manager = LunarManager.get(ctx.getSource().getServer());
                            if (target == MoonType.NORMAL) {
                                manager.clearOverride();
                                ctx.getSource().sendFeedback(() ->
                                    Text.literal("[LunarCycle] Moon override cleared — reverting to schedule.")
                                        .formatted(Formatting.YELLOW),
                                    true
                                );
                            } else {
                                manager.setOverride(target);
                                ctx.getSource().sendFeedback(() ->
                                    Text.empty()
                                        .append(Text.literal("[LunarCycle] Moon set to ").formatted(Formatting.GREEN))
                                        .append(Text.translatable(target.nameKey).formatted(Formatting.AQUA))
                                        .append(Text.literal(" until dawn.").formatted(Formatting.GREEN)),
                                    true
                                );
                            }
                            return 1;
                        })
                    )
                )
        );
    }
}
