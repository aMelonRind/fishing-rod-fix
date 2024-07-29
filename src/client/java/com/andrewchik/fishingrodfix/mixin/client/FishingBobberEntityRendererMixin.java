package com.andrewchik.fishingrodfix.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.andrewchik.fishingrodfix.FishingRodFixClient.projection;

@Mixin(FishingBobberEntityRenderer.class)
public class FishingBobberEntityRendererMixin {
    @Unique
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    @Unique
    private static final float D2R = (float) Math.PI / 180;

    @Redirect(method = "getHandPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"))
    private boolean fixFreeCam(Perspective instance, @Local(argsOnly = true) PlayerEntity player) {
        return instance.isFirstPerson() && player == mc.getCameraEntity();
    }

    @Inject(method = "getHandPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getFov()Lnet/minecraft/client/option/SimpleOption;"), cancellable = true)
    private void fixFirstPersonPosition(PlayerEntity player, float f, float tickDelta, CallbackInfoReturnable<Vec3d> cir, @Local int i) {
        cir.setReturnValue(firstPersonFallback(player, f, tickDelta, i));
    }

    @Unique // a calculating approach of rendering fishing line in first person, it's imperfect but pretty accurate
    private Vec3d firstPersonFallback(PlayerEntity player, float swingHandCoefficient, float tickDelta, int side) {
        ClientPlayerEntity p = mc.player;
        assert p != null;
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        double fov = dispatcher.gameOptions.getFov().getValue();
        double dFovCorrection = 1 / (Math.tan(fov / 2 * D2R) * projection.m11());

        boolean isMain = dispatcher.gameOptions.getMainArm().getValue() == Arm.RIGHT ^ side < 0;
        // preferredHand, aka swinging hand
        if (player.preferredHand == Hand.MAIN_HAND ^ isMain) {
            swingHandCoefficient = 0;
        }

        float dPitch = (player.getPitch(tickDelta) - MathHelper.lerp(tickDelta, p.lastRenderPitch, p.renderPitch)) * 0.1F * D2R;
        float dYaw = (player.getYaw(tickDelta) - MathHelper.lerp(tickDelta, p.lastRenderYaw, p.renderYaw)) % 360 * 0.1F;
        if (dYaw > 18) dYaw -= 36;
        else if (dYaw < -18) dYaw += 36;
        dYaw *= D2R;

        HeldItemRendererAccess fpr = (HeldItemRendererAccess) mc.gameRenderer.firstPersonRenderer;
        float progress = isMain
                ? MathHelper.lerp(tickDelta, fpr.getPrevEquipProgressMainHand(), fpr.getEquipProgressMainHand())
                : MathHelper.lerp(tickDelta, fpr.getPrevEquipProgressOffHand(), fpr.getEquipProgressOffHand());

        return dispatcher.camera.getProjection().getPosition( // ah yes, magic numbers
                (float) (side * (1.125 / ((double) mc.getWindow().getWidth() / mc.getWindow().getHeight())) * (1 - swingHandCoefficient * 1.04) * dFovCorrection),
                (float) ((-1.1 + progress - 0.5 * swingHandCoefficient) * dFovCorrection)
        ).multiply(960.0 / fov).rotateY(dYaw).rotateX(dPitch).add(dispatcher.camera.getPos());
    }

}
