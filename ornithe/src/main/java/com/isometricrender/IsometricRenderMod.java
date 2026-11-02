package com.isometricrender;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.GuiScreen;

public class IsometricRenderMod implements ClientModInitializer {

    private static GuiScreen pendingScreen;

    @Override
    public void onInitializeClient() {}

    public static void openGuiNextTick(GuiScreen screen) {
        pendingScreen = screen;
    }

    public static GuiScreen getPendingScreen() {
        GuiScreen screen = pendingScreen;
        pendingScreen = null;
        return screen;
    }

    public static boolean hasPendingScreen() {
        return pendingScreen != null;
    }
}
