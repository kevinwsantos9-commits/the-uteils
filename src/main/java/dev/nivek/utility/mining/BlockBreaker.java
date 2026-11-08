package dev.nivek.utility.mining;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Quebra blocos extras pelo caminho normal do jogo (drops, encantamentos, dano de ferramenta, eventos de proteção). */
public final class BlockBreaker {
    private BlockBreaker() {
    }

    /** False se a ferramenta sumiu ou está na última unidade de durabilidade (nunca deixamos ela quebrar). */
    public static boolean canContinue(ServerPlayerEntity sp) {
        ItemStack tool = sp.getMainHandStack();
        if (tool.isEmpty()) return false;
        return !tool.isDamageable() || tool.getMaxDamage() - tool.getDamage() > 1;
    }

    public static boolean breakBlock(ServerPlayerEntity sp, ServerWorld world, BlockPos pos) {
        if (!world.canPlayerModifyAt(sp, pos)) return false;
        return sp.interactionManager.tryBreakBlock(pos);
    }
}
