package com.rwtema.denseores.ores;

import com.rwtema.denseores.BlockStateReference;
import net.minecraft.block.state.IBlockState;

public class OreTuple {
    private IBlockState normal;
    private IBlockState dense;

    public IBlockState getNormal() {
        return normal;
    }

    public IBlockState getDense() {
        return dense;
    }

    public void setDense(IBlockState dense) {
        this.dense = dense;
    }

    public void setNormal(IBlockState normal) {
        this.normal = normal;
    }

    public boolean hasDense() {
        return dense != null;
    }
}
