package com.isometricrender.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

// The world with everything outside the selection turned to air, so faces on the selection's edges are drawn.
class SelectionBlockAccess implements IBlockAccess {

    private final IBlockAccess world;
    private final BlockPos min, max;

    SelectionBlockAccess(IBlockAccess world, AreaSelection selection) {
        this.world = world;
        this.min   = selection.getMinPos();
        this.max   = selection.getMaxPos();
    }

    private boolean inside(BlockPos p) {
        return p.getX() >= min.getX() && p.getX() <= max.getX()
            && p.getY() >= min.getY() && p.getY() <= max.getY()
            && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    @Override public TileEntity getTileEntity(BlockPos p)          { return inside(p) ? world.getTileEntity(p) : null; }
    @Override public int getCombinedLight(BlockPos p, int light)   { return inside(p) ? world.getCombinedLight(p, light) : 0xF000F0; }
    @Override public IBlockState getBlockState(BlockPos p)         { return inside(p) ? world.getBlockState(p) : Blocks.AIR.getDefaultState(); }
    @Override public boolean isAirBlock(BlockPos p)                { return !inside(p) || world.isAirBlock(p); }
    @Override public Biome getBiome(BlockPos p)                    { return world.getBiome(p); }
    @Override public int getStrongPower(BlockPos p, EnumFacing d)  { return inside(p) ? world.getStrongPower(p, d) : 0; }
    @Override public WorldType getWorldType()                      { return world.getWorldType(); }
    @Override public boolean isSideSolid(BlockPos p, EnumFacing s, boolean def) { return inside(p) && world.isSideSolid(p, s, def); }
}
