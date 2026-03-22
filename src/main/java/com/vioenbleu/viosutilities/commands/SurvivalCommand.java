package com.vioenbleu.viosutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.vioenbleu.viosutilities.ViosUtilities;
import com.vioenbleu.viosutilities.data.ReturnLocationData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import java.util.Set;

public class SurvivalCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("survival")
                .then(Commands.literal("play")) // Alias /survival play
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();

                    if (player != null) {
                        // Check if player is NOT in the Hub
                        if (!player.level().dimension().equals(ViosUtilities.HUB_WORLD_KEY)) {
                            player.sendSystemMessage(Component.literal("You are already in Survival!")
                                    .withStyle(ChatFormatting.RED), true);
                            return 0;
                        }

                        executeTeleport(player, context.getSource().getServer());
                    }
                    return 1;
                })
        );

        // Register /play as a direct command
        dispatcher.register(Commands.literal("play").executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                if (!player.level().dimension().equals(ViosUtilities.HUB_WORLD_KEY)) {
                    player.sendSystemMessage(Component.literal("You are already in Survival!")
                            .withStyle(ChatFormatting.RED), true);
                    return 0;
                }
                executeTeleport(player, context.getSource().getServer());
            }
            return 1;
        }));
    }

    public static void executeTeleport(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;


        // In Mojang, server.getWorld is server.getLevel
        ServerLevel survivalWorld = server.getLevel(Level.OVERWORLD);

        if (survivalWorld != null) {
            BlockPos spawn = new BlockPos(0, 64, 0);

            ReturnLocationData.SavedPos pos = ReturnLocationData.getLocation(player.getUUID(), server);
            if (pos != null) {
                Identifier worldId = Identifier.tryParse(pos.worldId());

                ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
                // 2. Get that specific Level from the server
                ServerLevel targetWorld = server.getLevel(worldKey);

                // 3. If the world exists, send them back to their exact spot
                if (targetWorld != null) {
                    player.teleportTo(targetWorld, pos.x(), pos.y(), pos.z(), Set.of() ,pos.yaw(), pos.pitch(), false);

                    player.sendSystemMessage(Component.literal("Returning to your last location...")
                            .withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.teleportTo(survivalWorld, 0.5, 64, 0.5, Set.of(), 0, 0, true);
                    player.sendSystemMessage(Component.literal("Last location not found. Returning to world spawn.")
                            .withStyle(ChatFormatting.GREEN), true);
                }
            } else {
                player.teleportTo(survivalWorld, 0.5, 64, 0.5, Set.of(), 0, 0, true);
                player.sendSystemMessage(Component.literal("Last location not found. Returning to world spawn.")
                        .withStyle(ChatFormatting.GREEN), true);
            }
        }
    }
}