package com.vioenbleu.viosutilities.events;

import com.vioenbleu.viosutilities.ViosUtilities;
import com.vioenbleu.viosutilities.data.ReturnLocationData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.storage.LevelData;

public class PlayerJoinListener {

    public static void register() {
        // Handle Join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();

            // Create a separate thread-safe task that runs after a short delay
            // This is much more reliable than an immediate teleport
            server.execute(() -> {
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        server.execute(() -> {
                            if (player == null || player.isRemoved()) return;
                            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, true));
                            player.teleportTo(0, 100000, 0);

                            ServerLevel hubWorld = server.getLevel(ViosUtilities.HUB_WORLD_KEY);
                            if (hubWorld != null) {
                                // 1. Perform the teleport
                                player.teleportTo(hubWorld, 0.5, 64, 0.5, java.util.Set.of(), 0, 0, true);

                                // 2. The corrected Packet logic for 1.21 Mojang Mappings
                                // We use the level's data directly
                                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket(
                                        new LevelData.RespawnData( new GlobalPos(ViosUtilities.HUB_WORLD_KEY, new BlockPos(0, 64, 0)),0,0)
                                ));
                            }
                        });
                    }
                }, 100);
            });
        });

        // Handle Disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();

            ReturnLocationData.saveLocation(player.getUUID(), new ReturnLocationData.SavedPos(
                    player.level().dimension().identifier().toString(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot()
            ), server);
        });
    }
}