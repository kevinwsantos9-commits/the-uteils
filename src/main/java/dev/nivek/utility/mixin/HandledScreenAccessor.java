package dev.nivek.utility.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int nivek_getX();

    @Accessor("y")
    int nivek_getY();

    @Accessor("backgroundWidth")
    int nivek_getBackgroundWidth();

    @Accessor("backgroundHeight")
    int nivek_getBackgroundHeight();
}
