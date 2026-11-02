package com.isometricrender.render;

/**
 * Configuration container for isometric rendering parameters.
 */
public class RenderSettings {
    
    private float scale = 1.0f;
    private float zoom = 1.0f;
    private float rotationX = 30.0f;
    private float rotationY = 45.0f;
    private int exportResolution = 1000;
    private int backgroundColor = 0x00000000;
    private boolean freezeEntities = true;
    private boolean runAnimations = false;
    private boolean showParticles = true;
    private boolean dumpIntoRoot = true;
    private boolean overwriteLatestFile = false;
    private float lightLevel = 15.0f;
    private boolean useWorldLight = false;
    
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = Math.max(0.1f, Math.min(10.0f, scale)); }
    
    public float getRotationX() { return rotationX; }
    public void setRotationX(float rotationX) { this.rotationX = rotationX % 360; }
    
    public float getRotationY() { return rotationY; }
    public void setRotationY(float rotationY) { this.rotationY = rotationY % 360; }
    
    public int getExportResolution() { return exportResolution; }
    public void setExportResolution(int resolution) { this.exportResolution = Math.max(100, Math.min(8192, resolution)); }
    
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int color) { this.backgroundColor = color; }
    
    public String getBackgroundColorHex() { return String.format("#%06X", backgroundColor & 0xFFFFFF); }
    
    public void setBackgroundColorFromHex(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int color = Integer.parseInt(hex, 16);
            this.backgroundColor = 0xFF000000 | color;
        } catch (NumberFormatException e) { }
    }
    
    public boolean isFreezeEntities() { return freezeEntities; }
    public void setFreezeEntities(boolean freeze) { this.freezeEntities = freeze; }
    
    public boolean isRunAnimations() { return runAnimations; }
    public void setRunAnimations(boolean run) { this.runAnimations = run; }
    
    public boolean isShowParticles() { return showParticles; }
    public void setShowParticles(boolean show) { this.showParticles = show; }
    
    public boolean isDumpIntoRoot() { return dumpIntoRoot; }
    public void setDumpIntoRoot(boolean dump) { this.dumpIntoRoot = dump; }
    
    public boolean isOverwriteLatestFile() { return overwriteLatestFile; }
    public void setOverwriteLatestFile(boolean overwrite) { this.overwriteLatestFile = overwrite; }
    
    public float getLightLevel() { return lightLevel; }
    public void setLightLevel(float level) { this.lightLevel = Math.max(0, Math.min(15, level)); }
    
    public boolean isUseWorldLight() { return useWorldLight; }
    public void setUseWorldLight(boolean useWorld) { this.useWorldLight = useWorld; }
    
    public float getZoom() { return zoom; }
    public void setZoom(float zoom) { this.zoom = Math.max(0.1f, Math.min(10.0f, zoom)); }
    
    public void reset() {
        scale = 1.0f;
        rotationX = 30.0f;
        rotationY = 45.0f;
        exportResolution = 1000;
        backgroundColor = 0xFF000000;
        freezeEntities = true;
        runAnimations = false;
        showParticles = true;
        dumpIntoRoot = true;
        overwriteLatestFile = false;
        lightLevel = 15.0f;
        useWorldLight = false;
        zoom = 1.0f;
    }
}
