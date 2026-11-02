package com.isometricrender.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public class GuiSlider extends Gui {
    
    public final int x, y, width, height;
    private final float minVal, maxVal;
    private float value;
    private boolean dragging;
    
    public GuiSlider(int x, int y, int width, int height, float minVal, float maxVal, float value) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.value = value;
    }
    
    public void draw(Minecraft mc) {
        drawRect(x, y, x + width, y + height, 0xFF000000);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF333333);
        
        float frac = (value - minVal) / (maxVal - minVal);
        int fw = (int)((width - 2) * frac);
        if (fw > 0) {
            drawRect(x + 1, y + 1, x + 1 + fw, y + height - 1, 0xFF4488AA);
        }
        
        int hx = x + 1 + fw;
        drawRect(hx - 2, y, hx + 2, y + height, 0xFFDDDDDD);
    }
    
    public boolean mousePressed(int mx, int my) {
        if (mx >= x && mx <= x + width && my >= y - 2 && my <= y + height + 2) {
            dragging = true;
            updateFromMouse(mx);
            return true;
        }
        return false;
    }
    
    public void mouseReleased() { dragging = false; }
    
    public void mouseDragged(int mx) {
        if (dragging) updateFromMouse(mx);
    }
    
    private void updateFromMouse(int mx) {
        float frac = (float)(mx - x) / width;
        frac = Math.max(0, Math.min(1, frac));
        value = minVal + frac * (maxVal - minVal);
    }
    
    public float getValue() { return value; }
    
    public void setValue(float v) {
        value = Math.max(minVal, Math.min(maxVal, v));
    }
    
    public boolean isDragging() { return dragging; }
}
