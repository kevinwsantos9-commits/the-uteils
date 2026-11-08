package dev.nivek.utility.tree;

import dev.nivek.utility.mining.BlockBreaker;
import dev.nivek.utility.network.SettingsPayload;
import dev.nivek.utility.util.BlockSearch;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public final class TreeCapitator {
    private TreeCapitator() {
    }

    /** Chamado depois que o jogador quebrou um bloco. Retorna true se tratou como árvore. */
    public static boolean tryRun(ServerWorld world, ServerPlayerEntity sp, BlockPos pos, BlockState state, SettingsPayload s) {
        if (!s.tree() || !state.isIn(BlockTags.LOGS)) return false;
        ItemStack tool = sp.getMainHandStack();
        if (!(tool.getItem() instanceof AxeItem)) return false;
        if (sp.isSneaking()) return false; // agachado = quebra normal, 1 bloco
        if (s.treeBaseOnly() && world.getBlockState(pos.down()).isIn(BlockTags.LOGS)) return false;

        Block log = state.getBlock();
        List<BlockPos> logs = BlockSearch.flood(world, pos, st -> st.isOf(log), s.treeMax());
        if (logs.isEmpty() || !hasLeaves(world, pos, logs)) return false; // pilha de troncos sem folhas: não é árvore

        logs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        for (BlockPos p : logs) {
            if (!BlockBreaker.canContinue(sp)) break;
            BlockBreaker.breakBlock(sp, world, p);
        }
        return true;
    }

    private static boolean hasLeaves(ServerWorld world, BlockPos origin, List<BlockPos> logs) {
        if (touchesLeaves(world, origin)) return true;
        for (BlockPos p : logs) {
            if (touchesLeaves(world, p)) return true;
        }
        return false;
    }

    private static boolean touchesLeaves(ServerWorld world, BlockPos p) {
        for (Direction d : Direction.values()) {
            if (world.getBlockState(p.offset(d)).isIn(BlockTags.LEAVES)) return true;
        }
        return false;
    }
}
