package com.isometricrender.command;

import com.isometricrender.IsometricRenderMod;
import com.isometricrender.gui.IsometricRenderScreen;
import com.isometricrender.render.AreaSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class ClientCommandHandler {

    public static boolean handleCommand(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return false;

        String[] parts = message.split("\\s+");
        if (!parts[0].equalsIgnoreCase("/isorender")) return false;

        if (parts.length < 2) {
            send(TextFormatting.YELLOW + "=== Isometric Render ===");
            send(TextFormatting.GREEN + "/isorender area <x1> <y1> <z1> <x2> <y2> <z2>");
            send(TextFormatting.GRAY + "Press TAB to autocomplete coordinates from crosshair");
            return true;
        }

        if (!"area".equalsIgnoreCase(parts[1])) {
            send(TextFormatting.RED + "Unknown subcommand: " + parts[1]);
            return true;
        }

        if (parts.length != 8) {
            send(TextFormatting.RED + "Usage: /isorender area <x1> <y1> <z1> <x2> <y2> <z2>");
            return true;
        }

        try {
            BlockPos pos1 = new BlockPos(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
            BlockPos pos2 = new BlockPos(Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7]));
            AreaSelection sel = new AreaSelection(pos1, pos2);
            send(TextFormatting.GREEN + "Opening renderer... (" + sel.getSizeX() * sel.getSizeY() * sel.getSizeZ() + " blocks)");
            IsometricRenderMod.openGuiNextTick(new IsometricRenderScreen(sel));
        } catch (NumberFormatException e) {
            send(TextFormatting.RED + "Invalid coordinates.");
        }

        return true;
    }

    private static void send(String msg) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) mc.player.sendMessage(new TextComponentString(msg));
    }
}
