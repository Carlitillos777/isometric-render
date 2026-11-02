package com.isometricrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public class GuiChatMixin {

    @Shadow
    protected GuiTextField inputField;

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (keyCode != Keyboard.KEY_TAB || inputField == null) return;

        String text = inputField.getText();
        if (text == null) return;

        // Complete command name: /is* -> /isorender
        if (text.startsWith("/") && !text.contains(" ")) {
            String partial = text.substring(1).toLowerCase();
            if (partial.length() > 0 && "isorender".startsWith(partial)) {
                inputField.setText("/isorender ");
                ci.cancel();
                return;
            }
        }

        if (!text.startsWith("/isorender ")) return;

        String afterCmd = text.substring("/isorender ".length());
        String[] parts = afterCmd.split(" ", -1);

        // Complete subcommand: /isorender [a*] -> /isorender area
        if (parts.length <= 1) {
            String partial = parts.length == 1 ? parts[0].toLowerCase() : "";
            if ("area".startsWith(partial)) {
                inputField.setText("/isorender area ");
                ci.cancel();
            }
            return;
        }

        // Complete coordinates for /isorender area
        if (!"area".equals(parts[0])) return;

        RayTraceResult hit = Minecraft.getMinecraft().objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) return;

        BlockPos pos = hit.getBlockPos();
        int[] coords = { pos.getX(), pos.getY(), pos.getZ() };

        // Count valid coordinate tokens already present (parts[1..])
        int filled = 0;
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) break;
            try { Integer.parseInt(parts[i]); filled++; }
            catch (NumberFormatException e) { break; }
        }
        if (filled >= 6) return;

        // Preserve existing coordinates, append the next full set (pos1 or pos2)
        StringBuilder sb = new StringBuilder("/isorender area");
        for (int i = 1; i <= filled && i < parts.length; i++) {
            if (!parts[i].isEmpty()) sb.append(" ").append(parts[i]);
        }
        int target = filled < 3 ? 3 : 6;
        for (int i = filled; i < target; i++) {
            sb.append(" ").append(coords[i % 3]);
        }
        sb.append(" ");
        inputField.setText(sb.toString());
        ci.cancel();
    }
}
