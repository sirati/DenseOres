package com.rwtema.denseores.cubicchunks;

import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.populator.CustomVeinGenerator;
import net.minecraft.block.state.IBlockState;

public class UsualVeinGenerator<Type> extends CustomVeinGenerator implements IHasConfig<Type> {
    private final Type config;

    public UsualVeinGenerator(IBlockState state, int blockCount, Type config) {
        super(state, blockCount);
        this.config = config;
    }

    public UsualVeinGenerator(IBlockState state, int stepCount, CustomGeneratorSettings.GenerationCondition blockPlaceCondition, Type config) {
        super(state, stepCount, blockPlaceCondition);
        this.config = config;
    }

    @Override
    public Type getConfig() {
        return config;
    }
}
