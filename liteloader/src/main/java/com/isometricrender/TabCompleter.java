package com.isometricrender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import java.lang.reflect.Field;

public class TabCompleter {

    private static final Field inputFieldRef;

    static {
        Field f = null;
        for (Field field : GuiChat.class.getDeclaredFields()) {
            if (field.getType() == GuiTextField.class) {
                field.setAccessible(true);
                f = field;
                break;
            }
        }
        inputFieldRef = f;
    }

    private static GuiTextField getInputField(GuiChat chat) {
        try { return inputFieldRef != null ? (GuiTextField) inputFieldRef.get(chat) : null; }
        catch (Exception e) { return null; }
    }

    public static boolean handleTabComplete(GuiChat chat) {
        GuiTextField input = getInputField(chat);
        if (input == null) return false;

        String text = input.getText();

        if (text.matches("^/is[^\\s]*$")) {
            input.setText("/isorender "); return true;
        }
        if (text.matches("^/isorender\\s*(?:a(?:r(?:e(?:a)?)?)?)?$")) {
            input.setText("/isorender area "); return true;
        }
        if (text.startsWith("/isorender area")) {
            RayTraceResult trace = Minecraft.getMinecraft().objectMouseOver;
            if (trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK) return false;

            BlockPos pos = trace.getBlockPos();

            String afterArea = text.substring("/isorender area".length()).trim();
            int numCoords = afterArea.isEmpty() ? 0 : afterArea.split("\\s+").length;
            if (numCoords >= 6) return false;

            String base = text.replaceAll("\\s+$", "");
            input.setText(base + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            return true;
        }
        return false;
    }
}
