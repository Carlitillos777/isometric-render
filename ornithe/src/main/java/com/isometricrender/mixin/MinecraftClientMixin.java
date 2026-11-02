package com.isometricrender.mixin;

import com.isometricrender.IsometricRenderMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (IsometricRenderMod.hasPendingScreen()) {
            ((Minecraft) (Object) this).displayGuiScreen(IsometricRenderMod.getPendingScreen());
        }
    }
}
