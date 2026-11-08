package dev.nivek.utility.mining;

import dev.nivek.utility.network.SettingsPayload;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class AreaMiner {
    private AreaMiner() {
    }

    /** Quebra o plano 3x3 perpendicular à face batida. Agachar quebra só 1 bloco. */
    public static boolean tryRun(ServerWorld world, ServerPlayerEntity sp, BlockPos pos, Direction face, SettingsPayload s) {
        if (!s.area() || face == null || sp.isSneaking()) return false;
        ItemStack tool = sp.getMainHandStack();
        if (!(tool.getItem() instanceof PickaxeItem || tool.getItem() instanceof ShovelItem)) return false;

        boolean any = false;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p = switch (face.getAxis()) {
                    case X -> pos.add(0, a, b);
                    case Y -> pos.add(a, 0, b);
                    case Z -> pos.add(a, b, 0);
                };
                if (!BlockBreaker.canContinue(sp)) return any;
                BlockState st = world.getBlockState(p);
                if (!compatible(world, p, st, sp.getMainHandStack())) continue;
                if (BlockBreaker.breakBlock(sp, world, p)) any = true;
            }
        }
        return any;
    }

    private static boolean compatible(ServerWorld world, BlockPos p, BlockState st, ItemStack tool) {
        if (st.isAir() || !st.getFluidState().isEmpty()) return false;
        if (st.getHardness(world, p) < 0) return false;   // bedrock, barreiras etc.
        if (st.hasBlockEntity()) return false;            // baús, fornalhas...
        if (tool.getMiningSpeedMultiplier(st) <= 1.0F) return false; // ferramenta não é eficaz nesse bloco
        return !st.isToolRequired() || tool.isSuitableFor(st);
    }
}
