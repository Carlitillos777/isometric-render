package com.isometricrender.event;

import com.isometricrender.command.ClientCommandHandler;
import com.isometricrender.gui.IsometricRenderScreen;
import com.isometricrender.render.AreaSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {

    private AreaSelection pendingSelection;
    private int           openDelay;
    private boolean       tabWasDown;

    // GuiChat.inputField is obfuscated; locate it by type at class-load time.
    private static final Field INPUT_FIELD;
    static {
        Field f = null;
        for (Field field : GuiChat.class.getDeclaredFields()) {
            if (field.getType() == GuiTextField.class) {
                field.setAccessible(true);
                f = field;
                break;
            }
        }
        INPUT_FIELD = f;
    }

    @SubscribeEvent
    public void onChat(ClientChatEvent event) {
        String msg = event.getMessage();
        String cmd = msg.split("\\s+")[0];
        if (!cmd.equals("/isorender") && !cmd.equals("/iso")) return;
        Minecraft mc = Minecraft.getMinecraft();
        // Forge fires ClientChatEvent before addToSentMessages runs, so we add it manually.
        mc.ingameGUI.getChatGUI().addToSentMessages(msg);
        event.setCanceled(true);
        if (mc.player == null) return;
        AreaSelection sel = ClientCommandHandler.handle(msg, mc);
        if (sel != null) { pendingSelection = sel; openDelay = 1; }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();

        if (openDelay > 0 && pendingSelection != null && --openDelay == 0) {
            mc.displayGuiScreen(new IsometricRenderScreen(pendingSelection));
            pendingSelection = null;
        }

        if (mc.currentScreen instanceof GuiChat) {
            boolean tabDown = Keyboard.isKeyDown(Keyboard.KEY_TAB);
            if (tabDown && !tabWasDown) handleTabComplete((GuiChat) mc.currentScreen);
            tabWasDown = tabDown;
        } else {
            tabWasDown = false;
        }
    }

    private void handleTabComplete(GuiChat chat) {
        GuiTextField input = getInputField(chat);
        if (input == null) return;
        String text = input.getText();

        if (text.matches("^/is[^\\s]*$"))                            { input.setText("/isorender ");      return; }
        if (text.matches("^/isorender\\s*(?:a(?:r(?:e(?:a)?)?)?)?$")) { input.setText("/isorender area "); return; }
        if (!text.startsWith("/isorender area")) return;

        RayTraceResult trace = Minecraft.getMinecraft().objectMouseOver;
        if (trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK) return;
        BlockPos pos = trace.getBlockPos();

        String afterArea = text.substring("/isorender area".length()).trim();
        int n = afterArea.isEmpty() ? 0 : afterArea.split("\\s+").length;
        if (n >= 6) return;

        StringBuilder sb = new StringBuilder(text.replaceAll("\\s+$", ""));
        int fill = n < 3 ? 3 - n : 6 - n;
        for (int i = 0; i < fill; i++) {
            int c = (n + i) % 3;
            sb.append(" ").append(c == 0 ? pos.getX() : c == 1 ? pos.getY() : pos.getZ());
        }
        input.setText(sb.toString());
    }

    private static GuiTextField getInputField(GuiChat chat) {
        if (INPUT_FIELD == null) return null;
        try { return (GuiTextField) INPUT_FIELD.get(chat); }
        catch (Exception e) { return null; }
    }
}
