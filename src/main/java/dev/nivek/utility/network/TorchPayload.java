package dev.nivek.utility.network;

import dev.nivek.utility.NivekUtility;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Pedido de colocar uma tocha; maxLight = só coloca se a luz de bloco no local for menor ou igual a esse valor. */
public record TorchPayload(int maxLight) implements CustomPayload {
    public static final CustomPayload.Id<TorchPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NivekUtility.MOD_ID, "torch"));
    public static final PacketCodec<ByteBuf, TorchPayload> CODEC =
            PacketCodecs.VAR_INT.xmap(TorchPayload::new, TorchPayload::maxLight);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
