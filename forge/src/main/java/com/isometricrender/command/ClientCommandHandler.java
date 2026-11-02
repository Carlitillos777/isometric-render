package com.isometricrender.command;

import com.isometricrender.render.AreaSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class ClientCommandHandler {

    public static AreaSelection handle(String message, Minecraft mc) {
        if (mc.player == null) return null;

        if (message.startsWith("/iso ") || message.equals("/iso")) {
            send(mc, TextFormatting.GRAY + "Did you mean " + TextFormatting.YELLOW + "/isorender" + TextFormatting.GRAY + "?");
            return null;
        }

        String[] p = message.split("\\s+");

        if (p.length == 1) {
            send(mc, TextFormatting.YELLOW + "=== Isometric Render ===");
            send(mc, TextFormatting.GREEN + "/isorender area <x1> <y1> <z1> <x2> <y2> <z2>");
            send(mc, TextFormatting.GRAY + "Press TAB to autocomplete coordinates from crosshair");
            return null;
        }

        if (!"area".equals(p[1])) {
            send(mc, TextFormatting.RED + "Unknown subcommand: " + p[1]);
            return null;
        }

        if (p.length != 8) {
            send(mc, TextFormatting.RED + "Usage: /isorender area <x1> <y1> <z1> <x2> <y2> <z2>");
            return null;
        }

        try {
            BlockPos pos1 = new BlockPos(Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]));
            BlockPos pos2 = new BlockPos(Integer.parseInt(p[5]), Integer.parseInt(p[6]), Integer.parseInt(p[7]));
            AreaSelection sel = new AreaSelection(pos1, pos2);
            send(mc, TextFormatting.GREEN + "Opening renderer... (" + sel.getSizeX() * sel.getSizeY() * sel.getSizeZ() + " blocks)");
            send(mc, TextFormatting.GRAY + "Range: " + TextFormatting.WHITE +
                "(" + sel.getMinX() + ", " + sel.getMinY() + ", " + sel.getMinZ() + ")" +
                TextFormatting.GRAY + " to " + TextFormatting.WHITE +
                "(" + sel.getMaxX() + ", " + sel.getMaxY() + ", " + sel.getMaxZ() + ")");
            return sel;
        } catch (NumberFormatException e) {
            send(mc, TextFormatting.RED + "Invalid coordinates.");
            return null;
        }
    }

    private static void send(Minecraft mc, String msg) {
        mc.player.sendMessage(new TextComponentString(msg));
    }
}
