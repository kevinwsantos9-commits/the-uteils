package dev.nivek.utility.farming;

import dev.nivek.utility.config.ServerConfig;
import dev.nivek.utility.network.PlayerSettingsStore;
import dev.nivek.utility.network.SettingsPayload;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Colheita + replantio: clique direito em plantação madura e (opcional) modo automático em volta do jogador. */
public final class FarmingHandler {
    private FarmingHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || player.isSpectator() || player.isSneaking()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp) || !(world instanceof ServerWorld sw)) return ActionResult.PASS;
            if (!PlayerSettingsStore.get(sp).farmClick()) return ActionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            BlockState st = sw.getBlockState(pos);
            if (st.getBlock() instanceof CropBlock crop && crop.isMature(st) && sw.canPlayerModifyAt(sp, pos)) {
                harvest(sw, sp, pos, st, crop);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                SettingsPayload s = PlayerSettingsStore.get(sp);
                if (!s.farmAuto() || !ServerConfig.get().allowFarmAuto || sp.isSpectator()) continue;
                autoHarvest(sp.getServerWorld(), sp, s.farmRadius());
            }
        });
    }

    private static void autoHarvest(ServerWorld world, ServerPlayerEntity sp, int radius) {
        BlockPos c = sp.getBlockPos();
        int done = 0;
        for (int dx = -radius; dx <= radius && done < 8; dx++) {
            for (int dy = -1; dy <= 1 && done < 8; dy++) {
                for (int dz = -radius; dz <= radius && done < 8; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    BlockPos p = c.add(dx, dy, dz);
                    BlockState st = world.getBlockState(p);
                    if (st.getBlock() instanceof CropBlock crop && crop.isMature(st) && world.canPlayerModifyAt(sp, p)) {
                        harvest(world, sp, p, st, crop);
                        done++;
                    }
                }
            }
        }
    }

    /** Colhe (respeitando fortuna da mão), replanta usando uma semente dos próprios drops ou do inventário. */
    private static void harvest(ServerWorld world, ServerPlayerEntity sp, BlockPos pos, BlockState state, CropBlock crop) {
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, sp, sp.getMainHandStack());
        ItemStack seed = crop.getPickStack(world, pos, state);
        boolean replant = false;
        if (!seed.isEmpty()) {
            for (ItemStack d : drops) {
                if (!d.isEmpty() && ItemStack.areItemsEqual(d, seed)) {
                    d.decrement(1);
                    replant = true;
                    break;
                }
            }
            if (!replant) replant = consumeOne(sp, seed.getItem());
        }
        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
        if (replant) {
            world.setBlockState(pos, crop.withAge(0), Block.NOTIFY_ALL);
        } else {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
        for (ItemStack d : drops) {
            if (!d.isEmpty()) Block.dropStack(world, pos, d);
        }
        sp.addExhaustion(0.005F);
    }

    private static boolean consumeOne(ServerPlayerEntity sp, Item item) {
        for (ItemStack s : sp.getInventory().main) {
            if (s.isOf(item)) {
                s.decrement(1);
                return true;
            }
        }
        return false;
    }
}
