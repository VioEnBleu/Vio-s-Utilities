package com.vioenbleu.viosutilities;
import com.vioenbleu.viosutilities.commands.HubCommand;
import com.vioenbleu.viosutilities.commands.SurvivalCommand;
import com.vioenbleu.viosutilities.events.PlayerJoinListener;
import com.vioenbleu.viosutilities.gui.DimensionCompassMenu;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViosUtilities implements ModInitializer {
    public static final String MOD_ID = "viosutilities";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceKey<Level> HUB_WORLD_KEY = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(MOD_ID, "hub")
    );

    public static final ResourceKey<Level> AMPLIFIED_WORLD_KEY = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(MOD_ID, "amplified")
    );


    @Override
    public void onInitialize() {
        LOGGER.info("ViosUtilities Initialized - Preparing the Hub!");

        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerLevel world) -> {
            if (world.dimension().equals(HUB_WORLD_KEY)) {
                if (entity instanceof Mob || entity instanceof Animal) {
                    entity.discard();
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            HubCommand.register(dispatcher);
            SurvivalCommand.register(dispatcher);

        });

        PlayerJoinListener.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {});

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);

            if (stack.is(Items.RECOVERY_COMPASS)) {
                CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);

                // 1.21.1+ Mojmap: Check if the list of floats contains your ID
                if (data != null && !data.floats().isEmpty() && data.floats().get(0) == 101f) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.openMenu(new DimensionCompassMenu());

                        world.playSound(null, player.blockPosition(),
                                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.5f);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });

    }

}