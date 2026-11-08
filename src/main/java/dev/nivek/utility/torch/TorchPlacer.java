package dev.nivek.utility.torch;

import dev.nivek.utility.config.ServerConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

/** Coloca uma tocha do inventário na posição válida mais próxima do jogador. */
public final class TorchPlacer {
    private TorchPlacer() {
    }

    public static void place(ServerPlayerEntity sp, int maxLight) {
        if (!ServerConfig.get().allowTorch || sp.isSpectator() || !sp.canModifyBlocks()) return;
        ServerWorld world = sp.getServerWorld();

        ItemStack torch = find(sp, false);
        boolean soul = false;
        if (torch == null) {
            torch = find(sp, true);
            soul = true;
        }
        if (torch == null) {
            sp.sendMessage(Text.literal("Você não tem tochas."), true);
            return;
        }

        BlockPos feet = sp.getBlockPos();
        if (world.getLightLevel(LightType.BLOCK, feet) > maxLight) {
            sp.sendMessage(Text.literal("Aqui já está iluminado."), true);
            return;
        }

        Block block = soul ? Blocks.SOUL_TORCH : Blocks.TORCH;
        BlockState state = block.getDefaultState();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = feet.add(dx, dy, dz);
                    if (!world.getBlockState(p).isAir()) continue;
                    if (!state.canPlaceAt(world, p)) continue;
                    if (!world.canPlayerModifyAt(sp, p)) continue;
                    double d = p.getSquaredDistance(sp.getPos());
                    if (d < bestDist) {
                        bestDist = d;
                        best = p;
                    }
                }
            }
        }
        if (best == null) {
            sp.sendMessage(Text.literal("Sem posição válida para a tocha por perto."), true);
            return;
        }
        world.setBlockState(best, state, Block.NOTIFY_ALL);
        world.playSound(null, best, state.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!sp.isCreative()) torch.decrement(1);
        sp.getInventory().markDirty();
    }

    private static ItemStack find(ServerPlayerEntity sp, boolean soul) {
        PlayerInventory inv = sp.getInventory();
        for (ItemStack s : inv.main) {
            if (!s.isEmpty() && s.isOf(soul ? Items.SOUL_TORCH : Items.TORCH)) return s;
        }
        for (ItemStack s : inv.offHand) {
            if (!s.isEmpty() && s.isOf(soul ? Items.SOUL_TORCH : Items.TORCH)) return s;
        }
        return null;
    }
}
