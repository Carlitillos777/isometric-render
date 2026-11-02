package com.isometricrender.render;

import net.minecraft.util.math.BlockPos;

public class AreaSelection {

    private final BlockPos pos1, pos2;

    public AreaSelection(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public int getMinX() { return Math.min(pos1.getX(), pos2.getX()); }
    public int getMinY() { return Math.min(pos1.getY(), pos2.getY()); }
    public int getMinZ() { return Math.min(pos1.getZ(), pos2.getZ()); }
    public int getMaxX() { return Math.max(pos1.getX(), pos2.getX()); }
    public int getMaxY() { return Math.max(pos1.getY(), pos2.getY()); }
    public int getMaxZ() { return Math.max(pos1.getZ(), pos2.getZ()); }

    public int getSizeX() { return getMaxX() - getMinX() + 1; }
    public int getSizeY() { return getMaxY() - getMinY() + 1; }
    public int getSizeZ() { return getMaxZ() - getMinZ() + 1; }

    public BlockPos getMinPos() { return new BlockPos(getMinX(), getMinY(), getMinZ()); }
    public BlockPos getMaxPos() { return new BlockPos(getMaxX(), getMaxY(), getMaxZ()); }
}
