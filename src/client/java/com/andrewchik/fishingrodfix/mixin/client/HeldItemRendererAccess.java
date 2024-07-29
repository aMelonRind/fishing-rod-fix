package com.andrewchik.fishingrodfix.mixin.client;

import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererAccess {

    @Accessor
    float getPrevEquipProgressMainHand();
    @Accessor
    float getEquipProgressMainHand();
    @Accessor
    float getPrevEquipProgressOffHand();
    @Accessor
    float getEquipProgressOffHand();

}
