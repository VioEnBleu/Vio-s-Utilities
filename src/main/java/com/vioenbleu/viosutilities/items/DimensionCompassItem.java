package com.vioenbleu.viosutilities.items;

import com.vioenbleu.viosutilities.gui.DimensionCompassMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult; // Use this instead
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DimensionCompassItem extends Item {
    public DimensionCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (!world.isClientSide() && user instanceof ServerPlayer serverPlayer) {

            // Open the Menu
            serverPlayer.openMenu(new DimensionCompassMenu());

            // Play the chime
            world.playSound(null, user.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0f, 0.5f);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }
}