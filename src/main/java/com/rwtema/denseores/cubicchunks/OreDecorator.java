package com.rwtema.denseores.cubicchunks;


import com.rwtema.denseores.DenseOresRegistry;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.CubicOreGenEvent;
import io.github.opencubicchunks.cubicchunks.cubicgen.CWGEventFactory;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings.PeriodicGaussianOreConfig;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings.StandardOreConfig;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.populator.CustomVeinGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.preset.wrapper.BiomeDesc;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.MinecraftForge;

import java.util.Random;

import static io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.populator.PopulatorUtils.genOreBellCurve;
import static io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.populator.PopulatorUtils.genOreUniform;

public class OreDecorator implements ICubicPopulator {
    private final CustomGeneratorSettings cfg;
    private final Object2ObjectMap<BiomeDesc, BiomeGenSettings> biomeLookup = new Object2ObjectOpenHashMap<>();

    public OreDecorator(CustomGeneratorSettings cfg) {
        this.cfg = cfg;
    }

    @Override
    public void generate(World world, Random random, CubePos pos, Biome biome) {
        MinecraftForge.ORE_GEN_BUS.post(new CubicOreGenEvent.Pre(world, random, pos));
        BiomeGenSettings biomeGenSettings = getBiomeGenSettings(new BiomeDesc(biome));

        generateNormal(world, random, pos, biomeGenSettings.geologyGeneratorNorm);
        generateBellCurve(world, random, pos, biomeGenSettings.geologyGeneratorBell);
        generateNormal(world, random, pos, biomeGenSettings.oreGeneratorNorm);
        generateBellCurve(world, random, pos, biomeGenSettings.oreGeneratorBell);
        generateNormal(world, random, pos, biomeGenSettings.unknownGeneratorNorm);
        generateBellCurve(world, random, pos, biomeGenSettings.unknownGeneratorBell);

        MinecraftForge.ORE_GEN_BUS.post(new CubicOreGenEvent.Post(world, random, pos));


    }

    private <T extends WorldGenerator & IHasConfig<StandardOreConfig>> void generateNormal(World world, Random random, CubePos pos, ObjectList<? extends T> generators) {
        for (T gen : generators) {
            StandardOreConfig c = gen.getConfig();
            if (CWGEventFactory.generateOre(world, random, gen, pos, c.blockstate.getBlockState())) {
                genOreUniform(world, cfg, random, pos, c.generateWhen, c.spawnTries, c.spawnProbability, gen, c.minHeight, c.maxHeight);
            }
        }
    }

    private <T extends WorldGenerator & IHasConfig<PeriodicGaussianOreConfig>> void generateBellCurve(World world, Random random, CubePos pos, ObjectList<? extends T> generators) {
        for (T gen : generators) {
            PeriodicGaussianOreConfig c = gen.getConfig();
            if (CWGEventFactory.generateOre(world, random, gen, pos, c.blockstate.getBlockState())) {
                genOreBellCurve(world, cfg, random, pos, c.generateWhen, c.spawnTries, c.spawnProbability, gen, c.heightMean, c.heightStdDeviation,
                        c.heightSpacing, c.minHeight, c.maxHeight);
            }
        }
    }

    public BiomeGenSettings getBiomeGenSettings(BiomeDesc biome) {
        BiomeGenSettings result = biomeLookup.computeIfAbsent(biome, __ -> new BiomeGenSettings());
        if (result.needInit) {
            result.needInit = false;
            for (StandardOreConfig c : cfg.standardOres) {
                if (c.blockstate.getBlockState() == null) {
                    continue;
                }
                if (c.biomes != null && !c.biomes.contains(biome)) {
                    continue;
                }
                if (DenseOresRegistry.oreVariantLookup.containsKey(c.blockstate.getBlockState())) {
                    CCVeinGenerator<StandardOreConfig> gen = c.placeBlockWhen == null ?
                            new CCVeinGenerator<>(c.blockstate.getBlockState(), c.spawnSize, c) :
                            new CCVeinGenerator<>(c, c.blockstate.getBlockState(), c.spawnSize, c.placeBlockWhen);
                    result.oreGeneratorNorm.add(gen);

                } else {
                    UsualVeinGenerator<StandardOreConfig> gen = c.placeBlockWhen == null ?
                            new UsualVeinGenerator<>(c.blockstate.getBlockState(), c.spawnSize, c) :
                            new UsualVeinGenerator<>(c.blockstate.getBlockState(), c.spawnSize, c.placeBlockWhen, c);
                    if (DenseOresRegistry.containers.contains(c.blockstate.getBlockState())) {
                        result.geologyGeneratorNorm.add(gen);
                    } else {
                        result.unknownGeneratorNorm.add(gen);
                    }
                }
            }
            for (PeriodicGaussianOreConfig c : cfg.periodicGaussianOres) {
                if (c.blockstate.getBlockState() == null) {
                    continue;
                }
                if (c.biomes != null && !c.biomes.contains(biome)) {
                    continue;
                }
                if (DenseOresRegistry.oreVariantLookup.containsKey(c.blockstate.getBlockState())) {
                    CCVeinGenerator<PeriodicGaussianOreConfig> gen = c.placeBlockWhen == null ?
                            new CCVeinGenerator<>(c.blockstate.getBlockState(), c.spawnSize, c) :
                            new CCVeinGenerator<>(c, c.blockstate.getBlockState(), c.spawnSize, c.placeBlockWhen);
                    result.oreGeneratorBell.add(gen);

                } else {
                    UsualVeinGenerator<PeriodicGaussianOreConfig> gen = c.placeBlockWhen == null ?
                            new UsualVeinGenerator<>(c.blockstate.getBlockState(), c.spawnSize, c) :
                            new UsualVeinGenerator<>(c.blockstate.getBlockState(), c.spawnSize, c.placeBlockWhen, c);
                    if (DenseOresRegistry.containers.contains(c.blockstate.getBlockState())) {
                        result.geologyGeneratorBell.add(gen);
                    } else {
                        result.unknownGeneratorBell.add(gen);
                    }
                }
            }
        }

        return result;
    }



    public static class BiomeGenSettings{
        private final ObjectList<UsualVeinGenerator<StandardOreConfig>> geologyGeneratorNorm = new ObjectArrayList<>();
        private final ObjectList<UsualVeinGenerator<PeriodicGaussianOreConfig>> geologyGeneratorBell = new ObjectArrayList<>();
        private final ObjectList<CCVeinGenerator<StandardOreConfig>> oreGeneratorNorm = new ObjectArrayList<>();
        private final ObjectList<CCVeinGenerator<PeriodicGaussianOreConfig>> oreGeneratorBell = new ObjectArrayList<>();
        private final ObjectList<UsualVeinGenerator<StandardOreConfig>> unknownGeneratorNorm = new ObjectArrayList<>();
        private final ObjectList<UsualVeinGenerator<PeriodicGaussianOreConfig>> unknownGeneratorBell = new ObjectArrayList<>();
        private boolean needInit = true;
    }
}
