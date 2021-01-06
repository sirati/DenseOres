package com.rwtema.denseores.ores;

import com.rwtema.denseores.BlockStateReference;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;

public class OreLookup {
    private final IBlockState base;
    private Object2ObjectMap<IBlockState, OreTuple> lookup = new Object2ObjectOpenHashMap<>();

    public OreLookup(IBlockState base) {
        this.base = base;
    }

    public OreTuple get(IBlockState container) {
        return lookup.get(container);
    }

    public IBlockState getBase() {
        return base;
    }

    public void setVariant(IBlockState container, IBlockState value, boolean dense) {
        OreTuple tuple = lookup.computeIfAbsent(container, __ -> new OreTuple());
        if (dense) {
            tuple.setDense(value);
        } else {
            tuple.setNormal(value);
        }
    }
}
