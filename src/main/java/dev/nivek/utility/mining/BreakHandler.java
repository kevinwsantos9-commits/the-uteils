package dev.nivek.utility.mining;

import dev.nivek.utility.NivekUtility;
import dev.nivek.utility.network.PlayerSettingsStore;
import dev.nivek.utility.network.SettingsPayload;
import dev.nivek.utility.tree.TreeCapitator;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Liga Tree Capitator, Vein Miner e 3x3 ao evento de quebra de bloco. Prioridade: árvore > veio > área. */
public final class BreakHandler {
    private static boolean busy = false; // evita recursão: as quebras extras também disparam o evento
    private static final Map<UUID, Direction> FACES = new HashMap<>();

    private BreakHandler() {
    }

    public static void forget(ServerPlayerEntity p) {
        FACES.remove(p.getUuid());
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (!busy && player instanceof ServerPlayerEntity sp) {
                FACES.put(sp.getUuid(), faceOf(sp, pos));
            }
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (busy || !(player instanceof ServerPlayerEntity sp) || !(world instanceof ServerWorld sw)) return;
            if (sp.isCreative() || sp.isSpectator()) return;
            SettingsPayload s = PlayerSettingsStore.get(sp);
            Direction face = FACES.remove(sp.getUuid());
            busy = true;
            try {
                if (TreeCapitator.tryRun(sw, sp, pos, state, s)) return;
                if (VeinMiner.tryRun(sw, sp, pos, state, s)) return;
                AreaMiner.tryRun(sw, sp, pos, face, s);
            } catch (Exception e) {
                NivekUtility.LOGGER.warn("Erro na quebra em lote", e);
            } finally {
                busy = false;
            }
        });
    }

    /** Descobre qual face do bloco o jogador está batendo (necessário para o 3x3). */
    private static Direction faceOf(ServerPlayerEntity sp, BlockPos pos) {
        Vec3d eye = sp.getEyePos();
        Vec3d look = sp.getRotationVec(1.0F);
        Vec3d end = eye.add(look.multiply(8.0));
        BlockHitResult hit = sp.getServerWorld().raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, sp));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
            return hit.getSide();
        }
        return Direction.getFacing(look.x, look.y, look.z).getOpposite();
    }
}
