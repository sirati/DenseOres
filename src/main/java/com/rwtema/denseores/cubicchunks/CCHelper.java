package com.rwtema.denseores.cubicchunks;

import com.google.common.base.Throwables;
import com.rwtema.denseores.client.ModelGen;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.CubicPopulatorList;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.DecorateCubeBiomeEvent;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomTerrainGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.populator.DefaultDecorator;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static com.rwtema.denseores.DenseOresMod.wrap;

public class CCHelper {


    public static void register() {
        MinecraftForge.EVENT_BUS.register(CCHelper.class);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public static void onDecorateCubeBiome(DecorateCubeBiomeEvent.Decorate event) {

    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!(event.getWorld() instanceof ICubicWorldServer))return;
        ICubicWorldServer world = (ICubicWorldServer) event.getWorld();
        if (!world.isCubicWorld()) return;
        if (world.getCubeGenerator() instanceof CustomTerrainGenerator) {
            CustomTerrainGenerator generator = (CustomTerrainGenerator) world.getCubeGenerator();
            Map<Biome, ICubicPopulator> populators= getPopulators(generator);
            for (ICubicPopulator popList:populators.values()) {
                if (popList instanceof CubicPopulatorList) {
                    OreDecorator oreDecorator = new OreDecorator(getConfig(generator));
                    List<ICubicPopulator> pops = getPopulatorList(popList);
                    for (int i = 0; i < pops.size(); i++) {
                        if (pops.get(i) instanceof DefaultDecorator.Ores) {
                            pops.set(i, oreDecorator);
                        }
                    }
                }
            }

        }

    }

    private static Map<Biome, ICubicPopulator> getPopulators(CustomTerrainGenerator generator) {
        try {
            Field f = CustomTerrainGenerator.class.getDeclaredField("populators");
            f.setAccessible(true);
            return (Map<Biome, ICubicPopulator>) f.get(generator);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw wrap(new IllegalStateException("Could not extract populators of world generator", e));
        }
    }
    private static CustomGeneratorSettings getConfig(CustomTerrainGenerator generator) {
        try {
            Field f = CustomTerrainGenerator.class.getDeclaredField("conf");
            f.setAccessible(true);
            return (CustomGeneratorSettings) f.get(generator);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw wrap(new IllegalStateException("Could not extract config of world generator", e));
        }
    }
    private static List<ICubicPopulator> getPopulatorList(ICubicPopulator generator) {
        try {
            Field f = CubicPopulatorList.class.getDeclaredField("list");
            f.setAccessible(true);
            return (List<ICubicPopulator>) f.get(generator);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw wrap(new IllegalStateException("Could not extract populators of biome", e));
        }
    }


}
