package com.rwtema.denseores.cubicchunks;

import com.rwtema.denseores.DenseOresRegistry;
import com.rwtema.denseores.ores.OreLookup;
import com.rwtema.denseores.ores.OreTuple;
import io.github.opencubicchunks.cubicchunks.api.util.MathUtil;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

public class CCVeinGenerator<Type> extends WorldGenerator implements IHasConfig<Type> {
    private final Type config;
    private final IBlockState baseOre;
    private final OreLookup oreLookup;
    private final int stepCount;
    private final CustomGeneratorSettings.GenerationCondition blockPlaceCondition;

    public CCVeinGenerator(IBlockState state, int blockCount, Type config) {
        this(config, state, blockCount, (r, w, p) -> {
            IBlockState s = w.getBlockState(p);
            return DenseOresRegistry.containers.contains(s);
        });
    }

    public CCVeinGenerator(Type config, IBlockState state, int stepCount, CustomGeneratorSettings.GenerationCondition blockPlaceCondition) {
        this.config = config;
        this.baseOre = state;
        this.stepCount = stepCount;
        this.blockPlaceCondition = blockPlaceCondition;
        this.oreLookup = DenseOresRegistry.oreVariantLookup.get(baseOre);
    }

    public boolean generate(World world, Random rand, BlockPos pos) {
        float angle = rand.nextFloat() * 3.1415927F;
        double x1 = (double)((float)pos.getX() + MathHelper.sin(angle) * (float)this.stepCount / 8.0F);
        double x2 = (double)((float)pos.getX() - MathHelper.sin(angle) * (float)this.stepCount / 8.0F);
        double z1 = (double)((float)pos.getZ() + MathHelper.cos(angle) * (float)this.stepCount / 8.0F);
        double z2 = (double)((float)pos.getZ() - MathHelper.cos(angle) * (float)this.stepCount / 8.0F);
        double y1 = (double)(pos.getY() + rand.nextInt(3) - 2);
        double y2 = (double)(pos.getY() + rand.nextInt(3) - 2);

        for(int i = 0; i < this.stepCount; ++i) {
            float progress = (float)i / (float)this.stepCount;
            double stepX = MathUtil.lerp((double)progress, x1, x2);
            double stepY = MathUtil.lerp((double)progress, y1, y2);
            double stepZ = MathUtil.lerp((double)progress, z1, z2);
            double sizeFactor = rand.nextDouble() * (double)this.stepCount / 16.0D;
            double xzDiameter = (double)(MathHelper.sin(3.1415927F * progress) + 1.0F) * sizeFactor + 1.0D;
            double yDiameter = (double)(MathHelper.sin(3.1415927F * progress) + 1.0F) * sizeFactor + 1.0D;
            generateEllipsoid(rand, world, this.baseOre, this.blockPlaceCondition, stepX, stepY, stepZ, xzDiameter, yDiameter);
        }

        return true;
    }

    private void generateEllipsoid(Random rand, World world, IBlockState blockState, CustomGeneratorSettings.GenerationCondition placeCondition, double centerX, double centerY, double centerZ, double xzDiameter, double yDiameter) {
        int minX = MathHelper.floor(centerX - xzDiameter / 2.0D);
        int minY = MathHelper.floor(centerY - yDiameter / 2.0D);
        int minZ = MathHelper.floor(centerZ - xzDiameter / 2.0D);
        int maxX = MathHelper.floor(centerX + xzDiameter / 2.0D);
        int maxY = MathHelper.floor(centerY + yDiameter / 2.0D);
        int maxZ = MathHelper.floor(centerZ + xzDiameter / 2.0D);

        IBlockState lastStone = null;
        OreTuple lastOre = null;

        for(int x = minX; x <= maxX; ++x) {
            double dxNorm = ((double)x + 0.5D - centerX) / (xzDiameter / 2.0D);
            if (!(dxNorm * dxNorm > 1.0D)) {
                for(int y = minY; y <= maxY; ++y) {
                    double dyNorm = ((double)y + 0.5D - centerY) / (yDiameter / 2.0D);
                    if (!(dxNorm * dxNorm + dyNorm * dyNorm > 1.0D)) {
                        for(int z = minZ; z <= maxZ; ++z) {
                            double dzNorm = ((double)z + 0.5D - centerZ) / (xzDiameter / 2.0D);
                            if (!(dxNorm * dxNorm + dyNorm * dyNorm + dzNorm * dzNorm > 1.0D)) {
                                BlockPos position = new BlockPos(x, y, z);
                                if (placeCondition.canGenerate(rand, world, position)) {
                                    IBlockState current = world.getBlockState(position);
                                    if (lastStone != current) {
                                        lastStone = current;
                                        lastOre = oreLookup.get(current);
                                    }
                                    boolean dense = lastOre.hasDense() && rand.nextFloat() < 0.0167f;

                                    world.setBlockState(position, dense?lastOre.getDense():lastOre.getNormal(), 2);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public Type getConfig() {
        return config;
    }
}
