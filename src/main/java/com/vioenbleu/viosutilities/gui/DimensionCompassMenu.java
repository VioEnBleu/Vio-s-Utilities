package com.vioenbleu.viosutilities.gui;

import com.vioenbleu.viosutilities.ViosUtilities;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class DimensionCompassMenu implements MenuProvider {

    @Override
    public Component getDisplayName() {
        return Component.literal("Dimensional Rift").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.BOLD);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        SimpleContainer container = new SimpleContainer(9);

        // Define Buttons
        container.setItem(1, createGuiItem(Items.GRASS_BLOCK, "§aOverworld"));
        container.setItem(3, createGuiItem(Items.NETHERRACK, "§cThe Nether"));
        container.setItem(5, createGuiItem(Items.END_STONE, "§eThe End"));
        container.setItem(7, createGuiItem(Items.RECOVERY_COMPASS, "§dAmplified Dimension"));

        return new ChestMenu(MenuType.GENERIC_9x1, syncId, inv, container, 1) {
            @Override
            public void clicked(int slotIndex, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
                if (!(player instanceof ServerPlayer serverPlayer)) return;
                if (slotIndex < 0 || slotIndex >= 9) return;

                serverPlayer.closeContainer();

                switch (slotIndex) {
                    case 1 -> teleport(serverPlayer, Level.OVERWORLD);
                    case 3 -> teleport(serverPlayer, Level.NETHER);
                    case 5 -> teleport(serverPlayer, Level.END);
                    case 7 -> teleport(serverPlayer, ViosUtilities.AMPLIFIED_WORLD_KEY);
                }
            }

            @Override
            public boolean stillValid(Player player) { return true; }
        };
    }

    private ItemStack createGuiItem(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private void teleport(ServerPlayer player, ResourceKey<Level> dim) {


        if (player.level().dimension().equals(dim)) {
            player.sendSystemMessage(Component.literal("You are already in this reality.")
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
            player.closeContainer();
            return;
        }

        if (player.level().dimension().equals(ViosUtilities.HUB_WORLD_KEY)) {
            player.sendSystemMessage(Component.literal("You can't warp in the hub. Do /survival before.")
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
            player.closeContainer();
            return;
        }

        ServerLevel target = player.level().getServer().getLevel(dim);
        if (target != null) {
            player.closeContainer();

            // 1. Send the Respawn Packet FIRST
            // This tells the vanilla client: "Prepare to change worlds"
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(
                    player.createCommonSpawnInfo(target),
                    (byte) 3 // Flag 3 resets the world renderer but keeps player data
            ));

            // 2. Perform the actual Teleport
            // We use the full teleport method to ensure the server moves the entity correctly

            Vec3i Vec = new Vec3i((int) player.getX(), (int) player.getX(), (int) player.getZ());

            if (dim.equals(Level.END)){
                Vec = new Vec3i(100, 49, 0);
            }

            if (dim.equals(Level.NETHER)){
                Vec = new Vec3i((int) player.getX()/8, (int) player.getY(), (int) player.getZ()/8);
            }

            if (player.level().dimension() == Level.NETHER){
                Vec = new Vec3i((int) player.getX()*8, (int) player.getY(), (int) player.getZ()*8);
            }

            player.teleportTo(
                    target,
                    Vec.getX(),
                    Vec.getY(),
                    Vec.getZ(),
                    java.util.Set.of(), // No relative movement flags
                    player.getYRot(),
                    player.getXRot(),
                    true
            );

            // 3. Final Sync
            // Forces the client to align its coordinates with the server
            player.connection.teleport(Vec.getX(), Vec.getY(), Vec.getZ(), player.getYRot(), player.getXRot());
        }
    }
}