package dev.nivek.utility.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class BlockSearch {
    private BlockSearch() {
    }

    /**
     * Busca em largura (vizinhança 3x3x3) a partir de origin, sem incluir origin.
     * Limitada por max blocos e por distância máxima, só em chunks carregados.
     */
    public static List<BlockPos> flood(World world, BlockPos origin, Predicate<BlockState> match, int max) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && result.size() < max) {
            BlockPos cur = queue.poll();
            for (int dx = -1; dx <= 1 && result.size() < max; dx++) {
                for (int dy = -1; dy <= 1 && result.size() < max; dy++) {
                    for (int dz = -1; dz <= 1 && result.size() < max; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos n = cur.add(dx, dy, dz);
                        if (!seen.add(n)) continue;
                        if (n.getManhattanDistance(origin) > 96) continue;
                        if (!world.isChunkLoaded(n)) continue;
                        if (match.test(world.getBlockState(n))) {
                            result.add(n);
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return result;
    }
}
