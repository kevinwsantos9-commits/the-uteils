package dev.nivek.utility.mining;

import dev.nivek.utility.network.SettingsPayload;
import dev.nivek.utility.util.BlockSearch;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class VeinMiner {
    /** Tag convencional do Fabric: inclui minérios de outros mods que seguem a convenção. */
    private static final TagKey<Block> ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));

    private VeinMiner() {
    }

    public static boolean tryRun(ServerWorld world, ServerPlayerEntity sp, BlockPos pos, BlockState state, SettingsPayload s) {
        if (!s.vein()) return false;
        boolean active = switch (s.veinMode()) {
            case 0 -> true;
            case 1 -> sp.isSneaking();
            default -> s.veinKeyHeld();
        };
        if (!active || !isOre(state)) return false;
        ItemStack tool = sp.getMainHandStack();
        if (!tool.isSuitableFor(state)) return false; // só com a ferramenta correta

        String key = familyKey(state.getBlock());
        List<BlockPos> ores = BlockSearch.flood(world, pos, st -> isOre(st) && familyKey(st.getBlock()).equals(key), s.veinMax());
        for (BlockPos p : ores) {
            if (!BlockBreaker.canContinue(sp)) break;
            BlockBreaker.breakBlock(sp, world, p);
        }
        return !ores.isEmpty();
    }

    private static boolean isOre(BlockState st) {
        return st.isIn(ORES) || Registries.BLOCK.getId(st.getBlock()).getPath().endsWith("_ore");
    }

    /** Minério normal e a variante deepslate contam como o mesmo veio. */
    private static String familyKey(Block b) {
        Identifier id = Registries.BLOCK.getId(b);
        return id.getNamespace() + ":" + id.getPath().replace("deepslate_", "");
    }
}
