package com.rwtema.denseores;

import com.rwtema.denseores.blocks.BlockDenseOre;
import com.rwtema.denseores.blocks.ItemBlockDenseOre;
import net.minecraft.block.state.IBlockState;

public class DenseOre {
    public final BlockDenseOre block;
    public final ItemBlockDenseOre itemBlock;
    public final DenseOreInfo info;

    public DenseOre(BlockDenseOre block, ItemBlockDenseOre itemBlock, DenseOreInfo info) {
        this.block = block;
        this.itemBlock = itemBlock;
        this.info = info;
    }


    public IBlockState getBaseState() {
        return block.getBaseOreBlockState();
    }
}
