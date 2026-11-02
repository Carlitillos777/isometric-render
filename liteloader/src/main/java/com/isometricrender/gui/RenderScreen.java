package com.isometricrender.gui;

import com.isometricrender.AreaSelection;
import com.isometricrender.render.WorldRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;

public class RenderScreen extends GuiScreen {

    private final AreaSelection selection;
    private WorldRenderer renderer;

    // Persisted across GUI opens
    private static float  savedScale      = 16f;
    private static float  savedRotation   = 45f;
    private static float  savedSlant      = 30f;
    private static float  savedOffsetX    = 0f;
    private static float  savedOffsetY    = 0f;
    private static String savedResolution = "2048";

    private float scale, rotation, slant, offsetX, offsetY;

    private GuiTextField scaleField, rotationField, slantField, resolutionField;
    private GuiSlider    scaleSlider, rotationSlider, slantSlider;

    private boolean draggingPreview;
    private int lastMX, lastMY;

    private int pvX, pvY, pvW, pvH, panelX, panelW;
    private int yScale, yRotation, ySlant, yPresets, yReset;
    private int yExportLabel, yCustom;

    private String statusMsg = "";
    private long   statusTime;

    public RenderScreen(AreaSelection selection) {
        this.selection = selection;
        scale    = savedScale;
        rotation = savedRotation;
        slant    = savedSlant;
        offsetX  = savedOffsetX;
        offsetY  = savedOffsetY;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        pvX = 10; pvY = 30;
        pvW = (int) (this.width * 0.55f) - 15;
        pvH = this.height - 55;

        panelX = pvX + pvW + 15;
        panelW = this.width - panelX - 10;

        int fieldW  = 50;
        int sliderX = panelX + fieldW + 75;
        int sliderW = Math.max(40, panelW - fieldW - 80);
        int y = 55;

        yScale = y;
        scaleField = field(0, panelX + 60, y, fieldW, String.valueOf((int) scale));
        scaleSlider = new GuiSlider(sliderX, y, sliderW, 16, 1, 80, scale);
        y += 28;

        yRotation = y;
        rotationField = field(1, panelX + 60, y, fieldW, String.valueOf((int) rotation));
        rotationSlider = new GuiSlider(sliderX, y, sliderW, 16, 0, 360, rotation);
        y += 28;

        ySlant = y;
        slantField = field(2, panelX + 60, y, fieldW, String.valueOf((int) slant));
        slantSlider = new GuiSlider(sliderX, y, sliderW, 16, -90, 90, slant);
        y += 35;

        yPresets = y;
        int bw = (panelW - 5) / 2;
        buttonList.add(new GuiButton(10, panelX,          y, bw, 20, "Dimetric"));
        buttonList.add(new GuiButton(11, panelX + bw + 5, y, bw, 20, "Isometric"));
        y += 24;

        yReset = y;
        buttonList.add(new GuiButton(12, panelX, y, panelW, 20, "Reset View"));
        y += 30;

        yExportLabel = y; y += 14;
        int b3w = (panelW - 10) / 3;
        buttonList.add(new GuiButton(21, panelX,             y, b3w, 20, "1024"));
        buttonList.add(new GuiButton(22, panelX + b3w + 5,   y, b3w, 20, "2048"));
        buttonList.add(new GuiButton(23, panelX + 2*(b3w+5), y, b3w, 20, "4096"));
        y += 25;

        yCustom = y;
        int expBtnW = 55;
        resolutionField = field(5, panelX + 55, y + 1, panelW - expBtnW - 60, savedResolution);
        resolutionField.setMaxStringLength(5);
        buttonList.add(new GuiButton(24, panelX + panelW - expBtnW, y, expBtnW, 20, "Export"));
        y += 25;

        buttonList.add(new GuiButton(30, panelX, y,                  panelW, 20, "Copy to Clipboard"));
        buttonList.add(new GuiButton(0,  panelX, this.height - 30,   panelW, 20, "Close"));

        if (renderer == null) renderer = new WorldRenderer(selection);
    }

    private GuiTextField field(int id, int x, int y, int w, String text) {
        GuiTextField f = new GuiTextField(id, fontRenderer, x, y, w, 16);
        f.setText(text);
        f.setMaxStringLength(4);
        return f;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        syncValues();

        if (renderer != null)
            renderer.renderPreview(pvX, pvY, pvW, pvH, scale, rotation, slant, offsetX, offsetY);
        // 1px border right around the preview; everything outside it is dimmed
        drawRect(0,             0,           this.width,  pvY - 1,      0xCC000000);
        drawRect(0,             pvY - 1,     pvX - 1,     pvY + pvH + 1, 0xCC000000);
        drawRect(pvX + pvW + 1, pvY - 1,     this.width,  pvY + pvH + 1, 0xCC000000);
        drawRect(0,             pvY + pvH + 1, this.width, this.height,  0xCC000000);
        drawHorizontalLine(pvX - 1, pvX + pvW, pvY - 1,   0xFF666666);
        drawHorizontalLine(pvX - 1, pvX + pvW, pvY + pvH, 0xFF666666);
        drawVerticalLine(pvX - 1,   pvY - 1, pvY + pvH, 0xFF666666);
        drawVerticalLine(pvX + pvW, pvY - 1, pvY + pvH, 0xFF666666);

        drawCenteredString(fontRenderer, "Isometric Render", this.width / 2, 10, 0xFFFFFF);

        drawRect(panelX - 5, 30, panelX + panelW + 5, this.height - 10, 0xDD000000);

        fontRenderer.drawString("\u00a7e\u00a7lTransform Options", panelX, 38,           0xFFFFFF);
        fontRenderer.drawString("Scale:",    panelX, yScale    + 4, 0xFFFFFF);
        fontRenderer.drawString("Rotation:", panelX, yRotation + 4, 0xFFFFFF);
        fontRenderer.drawString("Slant:",    panelX, ySlant    + 4, 0xFFFFFF);
        fontRenderer.drawString("Presets",   panelX, yPresets  - 10, 0xAAAAAA);
        fontRenderer.drawString("\u00a7e\u00a7lExport", panelX, yExportLabel, 0xFFFFFF);
        fontRenderer.drawString("Custom:",   panelX, yCustom   + 5, 0xAAAAAA);

        scaleField.drawTextBox();
        rotationField.drawTextBox();
        slantField.drawTextBox();
        resolutionField.drawTextBox();
        scaleSlider.draw(mc);
        rotationSlider.draw(mc);
        slantSlider.draw(mc);

        fontRenderer.drawString(
            String.format("Area: %dx%dx%d", selection.getSizeX(), selection.getSizeY(), selection.getSizeZ()),
            pvX, this.height - 18, 0xAAAAAA);
        String hint = "Drag=Pan | Scroll=Zoom";
        fontRenderer.drawString(hint, pvX + pvW - fontRenderer.getStringWidth(hint),
            this.height - 18, 0x888888);

        if (!statusMsg.isEmpty() && System.currentTimeMillis() - statusTime < 4000)
            drawCenteredString(fontRenderer, statusMsg, pvX + pvW / 2, pvY + 5, 0x55FF55);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void syncValues() {
        if (scaleSlider.isDragging()) {
            scale = scaleSlider.getValue();
            scaleField.setText(String.valueOf((int) scale));
        } else {
            try {
                float v = Float.parseFloat(scaleField.getText());
                if (v >= 1 && v <= 80 && v != scale) { scale = v; scaleSlider.setValue(v); }
            } catch (NumberFormatException ignored) {}
        }
        if (rotationSlider.isDragging()) {
            rotation = rotationSlider.getValue();
            rotationField.setText(String.valueOf((int) rotation));
        } else {
            try {
                float v = Float.parseFloat(rotationField.getText());
                if (v != rotation) { rotation = v; rotationSlider.setValue(v); }
            } catch (NumberFormatException ignored) {}
        }
        if (slantSlider.isDragging()) {
            slant = slantSlider.getValue();
            slantField.setText(String.valueOf((int) slant));
        } else {
            try {
                float v = Float.parseFloat(slantField.getText());
                if (v >= -90 && v <= 90 && v != slant) { slant = v; slantSlider.setValue(v); }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        switch (btn.id) {
            case 0:  mc.displayGuiScreen(null); break;
            case 10: applyTransform(scale, 45, 30);      break;
            case 11: applyTransform(scale, 45, 35.264f); break;
            case 12: applyTransform(16, 45, 30); offsetX = offsetY = 0; break;
            case 21: doExport(1024,  false); break;
            case 22: doExport(2048,  false); break;
            case 23: doExport(4096,  false); break;
            case 24:
                try { doExport(clamp(Integer.parseInt(resolutionField.getText().trim()), 64, 8192), false); }
                catch (NumberFormatException e) { setStatus("\u00a7cInvalid resolution!"); }
                break;
            case 30:
                try { doExport(clamp(Integer.parseInt(resolutionField.getText().trim()), 64, 8192), true); }
                catch (NumberFormatException e) { doExport(2048, true); }
                break;
        }
    }

    private void applyTransform(float s, float r, float sl) {
        scale = s; rotation = r; slant = sl;
        scaleSlider.setValue(s);    scaleField.setText(String.valueOf((int) s));
        rotationSlider.setValue(r); rotationField.setText(String.valueOf((int) r));
        slantSlider.setValue(sl);   slantField.setText(String.valueOf((int) sl));
    }

    private void setStatus(String m) { statusMsg = m; statusTime = System.currentTimeMillis(); }
    private int  clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private void doExport(int res, boolean clipboard) {
        if (renderer == null) return;
        BufferedImage img = renderer.renderToImage(res, scale, rotation, slant, offsetX, offsetY);
        if (img == null) { setStatus("\u00a7cRender failed!"); return; }
        if (clipboard) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                       .setContents(new TransferableImage(img), null);
                setStatus("\u00a7aCopied (" + res + "px)");
            } catch (Exception e) { setStatus("\u00a7cClipboard failed!"); }
        } else {
            renderer.exportImage(img);
            setStatus("\u00a7aSaved " + res + "px \u2192 renders/");
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) {
        try { super.mouseClicked(mx, my, btn); } catch (Exception ignored) {}
        scaleField.mouseClicked(mx, my, btn);
        rotationField.mouseClicked(mx, my, btn);
        slantField.mouseClicked(mx, my, btn);
        resolutionField.mouseClicked(mx, my, btn);
        if (scaleSlider.mousePressed(mx, my))    return;
        if (rotationSlider.mousePressed(mx, my)) return;
        if (slantSlider.mousePressed(mx, my))    return;
        if (mx >= pvX && mx <= pvX + pvW && my >= pvY && my <= pvY + pvH) {
            draggingPreview = true; lastMX = mx; lastMY = my;
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int st) {
        super.mouseReleased(mx, my, st);
        draggingPreview = false;
        scaleSlider.mouseReleased();
        rotationSlider.mouseReleased();
        slantSlider.mouseReleased();
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        scaleSlider.mouseDragged(mx);
        rotationSlider.mouseDragged(mx);
        slantSlider.mouseDragged(mx);
        if (draggingPreview && !scaleSlider.isDragging()
                            && !rotationSlider.isDragging()
                            && !slantSlider.isDragging()) {
            offsetX += (mx - lastMX) * 0.05f;
            offsetY -= (my - lastMY) * 0.05f;
            lastMX = mx; lastMY = my;
        }
    }

    @Override
    public void handleMouseInput() {
        try { super.handleMouseInput(); } catch (Exception ignored) {}
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int mx = Mouse.getEventX() * this.width  / mc.displayWidth;
            int my = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;
            if (mx >= pvX && mx <= pvX + pvW && my >= pvY && my <= pvY + pvH) {
                scaleSlider.setValue(scroll > 0
                    ? scaleSlider.getValue() * 1.15f
                    : scaleSlider.getValue() / 1.15f);
                scale = scaleSlider.getValue();
                scaleField.setText(String.valueOf((int) scale));
            }
        }
    }

    @Override
    protected void keyTyped(char c, int k) {
        try { super.keyTyped(c, k); } catch (Exception ignored) {}
        scaleField.textboxKeyTyped(c, k);
        rotationField.textboxKeyTyped(c, k);
        slantField.textboxKeyTyped(c, k);
        resolutionField.textboxKeyTyped(c, k);
    }

    @Override
    public void updateScreen() {
        scaleField.updateCursorCounter();
        rotationField.updateCursorCounter();
        slantField.updateCursorCounter();
        resolutionField.updateCursorCounter();
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void onGuiClosed() {
        savedScale      = scale;
        savedRotation   = rotation;
        savedSlant      = slant;
        savedOffsetX    = offsetX;
        savedOffsetY    = offsetY;
        savedResolution = resolutionField.getText();
        if (renderer != null) renderer.cleanup();
    }

    private static class TransferableImage implements Transferable {
        private final Image img;
        TransferableImage(Image img) { this.img = img; }
        public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
        public boolean isDataFlavorSupported(DataFlavor f) { return DataFlavor.imageFlavor.equals(f); }
        public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(f)) throw new UnsupportedFlavorException(f);
            return img;
        }
    }
}
