package com.isometricrender.render;

import com.isometricrender.AreaSelection;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class WorldRenderer {

    private final Minecraft mc;
    private final AreaSelection selection;

    // Block geometry compiled once into GL display lists
    private int dlOpaque      = -1;
    private int dlTranslucent = -1;

    // Water, stained glass, ice...: drawn last, back to front, re-sorted when the view angle changes
    private final List<BlockPos> translucentBlocks = new ArrayList<>();
    private float sortedRotation = Float.NaN, sortedSlant = Float.NaN;
    private SelectionBlockAccess view;

    private List<TileEntity> cachedTileEntities;
    private List<Entity>     cachedEntities;

    public WorldRenderer(AreaSelection selection) {
        this.mc = Minecraft.getMinecraft();
        this.selection = selection;
    }

    // Renders directly to screen using scissor+viewport - no FBO, no flash.
    // glPushAttrib/glPopAttrib isolates GL state; GlStateManager cache is
    // re-synced manually afterwards since popAttrib bypasses it.
    public void renderPreview(int screenX, int screenY, int w, int h,
                              float scale, float rotation, float slant,
                              float offsetX, float offsetY) {
        int sf = new ScaledResolution(mc).getScaleFactor();
        int px = screenX * sf;
        int py = mc.displayHeight - (screenY + h) * sf;
        int pw = w * sf;
        int ph = h * sf;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);  GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px, py, pw, ph);
        GL11.glClearColor(0.08f, 0.08f, 0.08f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glViewport(px, py, pw, ph);

        setupProjection(w, h, scale, rotation, slant, offsetX, offsetY);
        renderScene();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);  GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);

        // Re-sync GlStateManager cache to match the state restored by popAttrib
        GlStateManager.bindTexture(0);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                 GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // Renders to an offscreen FBO and reads back pixels for export.
    public BufferedImage renderToImage(int resolution, float scale, float rotation, float slant,
                                       float offsetX, float offsetY) {
        Framebuffer fb = new Framebuffer(resolution, resolution, true);
        IntBuffer prevVP = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, prevVP);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);  GL11.glPushMatrix();
        try {
            fb.bindFramebuffer(true);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glViewport(0, 0, resolution, resolution);
            setupProjection(resolution, resolution, scale, rotation, slant, offsetX, offsetY);
            renderScene();
            GL11.glFlush();
            GL11.glFinish();
            ByteBuffer buf = BufferUtils.createByteBuffer(resolution * resolution * 4);
            GL11.glReadPixels(0, 0, resolution, resolution, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            BufferedImage img = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < resolution; y++)
                for (int x = 0; x < resolution; x++) {
                    int i = (x + (resolution - 1 - y) * resolution) * 4;
                    img.setRGB(x, y, ((buf.get(i+3)&0xFF)<<24) | ((buf.get(i)&0xFF)<<16)
                                   | ((buf.get(i+1)&0xFF)<<8)  |  (buf.get(i+2)&0xFF));
                }
            return img;
        } finally {
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);  GL11.glPopMatrix();
            GL11.glPopAttrib();
            fb.unbindFramebuffer();
            mc.getFramebuffer().bindFramebuffer(true);
            GL11.glViewport(prevVP.get(0), prevVP.get(1), prevVP.get(2), prevVP.get(3));
            fb.deleteFramebuffer();
        }
    }

    private void drawBlock(IBlockState s, BlockPos pos, BufferBuilder buf) {
        try { mc.getBlockRendererDispatcher().renderBlock(s.getActualState(view, pos), pos, view, buf); }
        catch (Exception ignored) {}
    }

    // ponytail: whole-block sort and a full list rebuild per angle change; fine until areas hold tens of thousands of translucent blocks
    private void sortTranslucent(float rotation, float slant) {
        if (translucentBlocks.isEmpty() || (rotation == sortedRotation && slant == sortedSlant)) return;
        sortedRotation = rotation;
        sortedSlant    = slant;
        // Eye-space depth of a block for the modelview built in setupProjection; larger = nearer the viewer
        double r = Math.toRadians(rotation), sl = Math.toRadians(slant);
        double kx = -Math.sin(r) * Math.cos(sl), ky = Math.sin(sl), kz = Math.cos(r) * Math.cos(sl);
        translucentBlocks.sort(Comparator.comparingDouble(p -> p.getX() * kx + p.getY() * ky + p.getZ() * kz));

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        if (dlTranslucent == -1) dlTranslucent = GL11.glGenLists(1);
        GL11.glNewList(dlTranslucent, GL11.GL_COMPILE);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        for (BlockPos p : translucentBlocks) drawBlock(view.getBlockState(p), p, buf);
        tess.draw();
        GL11.glEndList();
    }

    private void setupProjection(int w, int h, float scale, float rotation, float slant,
                                 float offsetX, float offsetY) {
        buildDisplayLists();
        sortTranslucent(rotation, slant);
        BlockPos min = selection.getMinPos();
        BlockPos max = selection.getMaxPos();
        float cx = (min.getX() + max.getX()) / 2f + 0.5f;
        float cy = (min.getY() + max.getY()) / 2f + 0.5f;
        float cz = (min.getZ() + max.getZ()) / 2f + 0.5f;
        float maxSz = Math.max(selection.getSizeX(), Math.max(selection.getSizeY(), selection.getSizeZ()));
        float view  = maxSz * 1.5f / (scale / 16f);
        float aspect = (float) w / h;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-view * aspect, view * aspect, -view, view, -500, 500);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(offsetX, offsetY, 0);
        GL11.glRotatef(slant,    1, 0, 0);
        GL11.glRotatef(rotation, 0, 1, 0);
        GL11.glTranslatef(-cx, -cy, -cz);
    }

    private void renderScene() {
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                 GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.color(1f, 1f, 1f, 1f);

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        if (dlOpaque != -1) GL11.glCallList(dlOpaque);

        renderTileEntities();
        renderEntities();

        if (dlTranslucent != -1) {
            GlStateManager.disableLighting();
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.depthMask(false);
            GL11.glCallList(dlTranslucent);
            GlStateManager.depthMask(true);
        }
    }

    // Compiles all block geometry into two display lists (opaque + translucent).
    // Called once on first render; subsequent calls are free (glCallList).
    private void buildDisplayLists() {
        if (dlOpaque != -1) return;

        World world = mc.world;
        if (world == null) return;

        BlockPos min = selection.getMinPos();
        BlockPos max = selection.getMaxPos();

        cachedTileEntities = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = min.getY(); y <= max.getY(); y++)
            for (int x = min.getX(); x <= max.getX(); x++)
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    p.setPos(x, y, z);
                    TileEntity te = world.getTileEntity(p);
                    if (te != null) cachedTileEntities.add(te);
                }

        cachedEntities = new ArrayList<>();
        AxisAlignedBB aabb = new AxisAlignedBB(
            min.getX() - 2, min.getY() - 2, min.getZ() - 2,
            max.getX() + 3, max.getY() + 3, max.getZ() + 3);
        for (Entity e : world.getEntitiesWithinAABBExcludingEntity(null, aabb))
            if (e != mc.player) cachedEntities.add(e);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        view = new SelectionBlockAccess(world, selection);
        translucentBlocks.clear();

        dlOpaque = GL11.glGenLists(1);
        GL11.glNewList(dlOpaque, GL11.GL_COMPILE);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        for (int y = min.getY(); y <= max.getY(); y++)
            for (int x = min.getX(); x <= max.getX(); x++)
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    pos.setPos(x, y, z);
                    IBlockState s = view.getBlockState(pos);
                    if (s.getMaterial() == Material.AIR) continue;
                    if (s.getBlock().getBlockLayer() == BlockRenderLayer.TRANSLUCENT) translucentBlocks.add(pos.toImmutable());
                    else drawBlock(s, pos, buf);
                }
        tess.draw();
        GL11.glEndList();
    }

    private void renderTileEntities() {
        if (cachedTileEntities == null || cachedTileEntities.isEmpty()) return;
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        for (TileEntity te : cachedTileEntities) {
            BlockPos tp = te.getPos();
            try { TileEntityRendererDispatcher.instance.render(te, tp.getX(), tp.getY(), tp.getZ(), 0f); }
            catch (Exception ignored) {}
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
    }

    // renderEntityStatic offsets by renderPosX/Y/Z. Zeroing it makes entities render at world coords,
    // which our modelview matrix (T(-cx,-cy,-cz)*R) maps correctly into view space.
    private void renderEntities() {
        if (cachedEntities == null || cachedEntities.isEmpty()) return;
        RenderManager rm = mc.getRenderManager();
        if (rm.renderEngine == null) rm.renderEngine = mc.getTextureManager();
        if (rm.world == null) rm.setWorld(mc.world);
        rm.setRenderPosition(0, 0, 0);
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        for (Entity entity : cachedEntities) {
            try { rm.renderEntityStatic(entity, 0f, false); }
            catch (Exception ignored) {}
        }
        if (mc.player != null) rm.setRenderPosition(mc.player.posX, mc.player.posY, mc.player.posZ);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
    }

    public void exportImage(BufferedImage img) {
        if (img == null) return;
        try {
            File dir = new File(mc.mcDataDir, "renders");
            dir.mkdirs();
            String name = "isometric_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".png";
            ImageIO.write(img, "PNG", new File(dir, name));
        } catch (Exception e) {
            System.err.println("[IsoRender] Export failed: " + e.getMessage());
        }
    }

    public void cleanup() {
        if (dlOpaque      != -1) { GL11.glDeleteLists(dlOpaque,      1); dlOpaque      = -1; }
        if (dlTranslucent != -1) { GL11.glDeleteLists(dlTranslucent, 1); dlTranslucent = -1; }
        translucentBlocks.clear();
        sortedRotation = sortedSlant = Float.NaN;
        cachedTileEntities = null;
        cachedEntities     = null;
    }
}
