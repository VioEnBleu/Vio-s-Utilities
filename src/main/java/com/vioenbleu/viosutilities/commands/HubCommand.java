package com.vioenbleu.viosutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.vioenbleu.viosutilities.ViosUtilities;
import com.vioenbleu.viosutilities.data.ReturnLocationData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class HubCommand {
    private static final HashMap<UUID, TeleportTask> pendingTeleports = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hub").executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null || player.level().dimension() == ViosUtilities.HUB_WORLD_KEY){
                    return 0;
            }




            pendingTeleports.put(player.getUUID(), new TeleportTask(
                    player.getX(), player.getY(), player.getZ(), player.getHealth(), 100
            ));

            return 1;
        }));

        // Register the ticker once
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            pendingTeleports.entrySet().removeIf(entry -> {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                TeleportTask task = entry.getValue();

                if (player == null) return true;

                // Check movement or damage
                if (player.distanceToSqr(task.x, task.y, task.z) > 0.1 || player.getHealth() < task.health) {
                    player.sendSystemMessage(Component.literal("Teleport cancelled! You moved or took damage.").withStyle(ChatFormatting.RED), true);
                    return true;
                }

                if (task.ticksLeft % 20 == 0) {
                    player.sendSystemMessage(
                            Component.literal("Teleporting in " + (task.ticksLeft / 20) + " seconds...")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE),
                            true
                    );
                }

                task.ticksLeft--;

                if (task.ticksLeft <= 0) {
                    var hubWorld = server.getLevel(ViosUtilities.HUB_WORLD_KEY);
                    if (hubWorld != null) {

                        ReturnLocationData.saveLocation(player.getUUID(), new ReturnLocationData.SavedPos(
                                player.level().dimension().identifier().toString(),
                                player.getX(), player.getY(), player.getZ(),
                                player.getYRot(), player.getXRot()
                        ), server);

                        player.teleportTo(hubWorld, 0.5, 64, 0.5, Set.of(), 0, 0, true);
                        player.sendSystemMessage(Component.literal("Welcome to the Hub!").withStyle(net.minecraft.ChatFormatting.GREEN), true);
                    }
                    return true;
                }
                return false;
            });
        });
    }

    private static class TeleportTask {
        double x, y, z;
        float health;
        int ticksLeft;

        TeleportTask(double x, double y, double z, float health, int ticksLeft) {
            this.x = x; this.y = y; this.z = z;
            this.health = health;
            this.ticksLeft = ticksLeft;
        }
    }
}