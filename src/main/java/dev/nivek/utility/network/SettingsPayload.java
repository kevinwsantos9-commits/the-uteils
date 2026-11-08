package dev.nivek.utility.network;

import dev.nivek.utility.NivekUtility;
import dev.nivek.utility.config.ServerConfig;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Opções do jogador enviadas ao servidor (o servidor aplica os próprios limites por cima). */
public record SettingsPayload(boolean tree, int treeMax, boolean treeBaseOnly,
                              boolean vein, int veinMax, int veinMode, boolean veinKeyHeld,
                              boolean area, boolean farmClick, boolean farmAuto, int farmRadius)
        implements CustomPayload {

    public static final CustomPayload.Id<SettingsPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NivekUtility.MOD_ID, "settings"));
    public static final PacketCodec<PacketByteBuf, SettingsPayload> CODEC =
            PacketCodec.of(SettingsPayload::write, SettingsPayload::new);
    /** Usado para jogadores sem o mod: nada é ativado para eles. */
    public static final SettingsPayload DISABLED =
            new SettingsPayload(false, 1, true, false, 1, 2, false, false, false, false, 1);

    public SettingsPayload(PacketByteBuf b) {
        this(b.readBoolean(), b.readVarInt(), b.readBoolean(),
                b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readBoolean(),
                b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readVarInt());
    }

    private void write(PacketByteBuf b) {
        b.writeBoolean(tree);
        b.writeVarInt(treeMax);
        b.writeBoolean(treeBaseOnly);
        b.writeBoolean(vein);
        b.writeVarInt(veinMax);
        b.writeVarInt(veinMode);
        b.writeBoolean(veinKeyHeld);
        b.writeBoolean(area);
        b.writeBoolean(farmClick);
        b.writeBoolean(farmAuto);
        b.writeVarInt(farmRadius);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Aplica as regras do servidor. */
    public SettingsPayload sanitized() {
        ServerConfig c = ServerConfig.get();
        return new SettingsPayload(
                tree && c.allowTree, clamp(treeMax, 1, Math.max(1, c.maxTreeBlocks)), treeBaseOnly,
                vein && c.allowVein, clamp(veinMax, 1, Math.max(1, c.maxVeinBlocks)), clamp(veinMode, 0, 2), veinKeyHeld,
                area && c.allowArea, farmClick, farmAuto && c.allowFarmAuto, clamp(farmRadius, 1, Math.max(1, c.maxFarmRadius)));
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
