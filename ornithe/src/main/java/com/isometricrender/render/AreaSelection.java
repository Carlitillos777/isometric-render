package com.isometricrender.render;

import net.minecraft.util.math.BlockPos;

public class AreaSelection {

    private BlockPos pos1, pos2;

    public AreaSelection() {}

    public AreaSelection(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public void setPos1(BlockPos pos) { this.pos1 = pos; }
    public void setPos2(BlockPos pos) { this.pos2 = pos; }

    public boolean isComplete() { return pos1 != null && pos2 != null; }

    public BlockPos getMinPos() {
        return new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ()));
    }

    public BlockPos getMaxPos() {
        return new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ()));
    }

    public int getSizeX() { return Math.abs(pos2.getX() - pos1.getX()) + 1; }
    public int getSizeY() { return Math.abs(pos2.getY() - pos1.getY()) + 1; }
    public int getSizeZ() { return Math.abs(pos2.getZ() - pos1.getZ()) + 1; }
}
