package dev.nivek.utility.network;

import dev.nivek.utility.NivekUtility;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** mode: 0 = organizar inventário, 1 = organizar baú aberto, 2 = mover itens iguais para o baú aberto. */
public record OrganizePayload(int mode) implements CustomPayload {
    public static final CustomPayload.Id<OrganizePayload> ID =
            new CustomPayload.Id<>(Identifier.of(NivekUtility.MOD_ID, "organize"));
    public static final PacketCodec<ByteBuf, OrganizePayload> CODEC =
            PacketCodecs.VAR_INT.xmap(OrganizePayload::new, OrganizePayload::mode);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
