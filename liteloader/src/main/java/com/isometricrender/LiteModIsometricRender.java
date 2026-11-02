package com.isometricrender;

import com.isometricrender.gui.RenderScreen;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.Tickable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

import java.io.File;

public class LiteModIsometricRender implements LiteMod, OutboundChatFilter, Tickable {

    private boolean tabPressed;
    private AreaSelection pendingSelection;
    private int openGuiDelay;

    @Override public String getName()    { return "IsometricRender"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public void init(File configPath) {}
    @Override public void upgradeSettings(String version, File configPath, File oldConfigPath) {}

    @Override
    public void onTick(Minecraft mc, float partialTicks, boolean inGame, boolean clock) {
        // TAB autocomplete for chat
        if (mc.currentScreen instanceof GuiChat) {
            boolean tabDown = Keyboard.isKeyDown(Keyboard.KEY_TAB);
            if (tabDown && !tabPressed) {
                tabPressed = true;
                TabCompleter.handleTabComplete((GuiChat) mc.currentScreen);
            } else if (!tabDown) {
                tabPressed = false;
            }
        } else {
            tabPressed = false;
        }

        // Deferred GUI open - wait 1 tick after chat closes
        if (openGuiDelay > 0 && pendingSelection != null) {
            if (--openGuiDelay == 0) {
                mc.displayGuiScreen(new RenderScreen(pendingSelection));
                pendingSelection = null;
            }
        }
    }

    @Override
    public boolean onSendChatMessage(String message) {
        String cmd = message.split("\\s+")[0];
        if (!cmd.equals("/isorender") && !cmd.equals("/iso")) return true;

        Minecraft mc = Minecraft.getMinecraft();

        if (message.startsWith("/iso ") || message.equals("/iso")) {
            mc.player.sendMessage(new TextComponentString("§7Did you mean §e/isorender§7?"));
            return false;
        }

        String[] parts = message.substring(1).split(" ");

        if (parts.length == 1) {
            mc.player.sendMessage(new TextComponentString("§e=== Isometric Render ==="));
            mc.player.sendMessage(new TextComponentString("§a/isorender area <x1> <y1> <z1> <x2> <y2> <z2>"));
            mc.player.sendMessage(new TextComponentString("§7Press TAB to autocomplete coordinates"));
            return false;
        }

        if ("area".equals(parts[1])) {
            if (parts.length != 8) {
                mc.player.sendMessage(new TextComponentString("§cUsage: /isorender area <x1> <y1> <z1> <x2> <y2> <z2>"));
                return false;
            }
            try {
                BlockPos p1 = new BlockPos(
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                BlockPos p2 = new BlockPos(
                    Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7]));
                AreaSelection sel = new AreaSelection(p1, p2);
                int blocks = sel.getSizeX() * sel.getSizeY() * sel.getSizeZ();
                mc.player.sendMessage(new TextComponentString(
                    "§aOpening renderer... (" + blocks + " blocks)"));
                mc.player.sendMessage(new TextComponentString(
                    "§7Range: §f(" + sel.getMinX() + ", " + sel.getMinY() + ", " + sel.getMinZ() + ")" +
                    " §7to §f(" + sel.getMaxX() + ", " + sel.getMaxY() + ", " + sel.getMaxZ() + ")"));
                pendingSelection = sel;
                openGuiDelay = 1;
            } catch (NumberFormatException e) {
                mc.player.sendMessage(new TextComponentString("§cInvalid coordinates"));
            }
        } else {
            mc.player.sendMessage(new TextComponentString("§cUnknown subcommand: " + parts[1]));
        }

        return false;
    }
}
