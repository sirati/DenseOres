package com.rwtema.denseores;

import com.rwtema.denseores.utils.LogHelper;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

import static com.rwtema.denseores.BlockStateInfo.createMinecraft;

@Mod.EventBusSubscriber(modid = DenseOresMod.MODID)
public class DenseOresRegistry {

	public static Object2ObjectMap<ResourceLocation, DenseOreInfo> oreInfos = new Object2ObjectOpenHashMap<>();
	public static ObjectSet<DenseOreInfo> vanillaOres = new ObjectOpenHashSet<>();
	public static Object2ObjectMap<ResourceLocation, DenseOre> ores = new Object2ObjectOpenHashMap<>();
	public static String blockPrefix = DenseOresMod.MODID;
	// add vanilla entries (TODO: add a way to disable vanilla ores)
	public static void initVanillaOres() {
		BlockStateInfo stone = createMinecraft("stone");
		BlockStateInfo netherrack = createMinecraft("netherrack");
		vanillaOres.add(createOreInfo("Vanilla", "Iron Ore", createMinecraft("iron_ore"), stone, null, ToolInfo.NONE, false, 0, 0, -1));
		vanillaOres.add(createOreInfo("Vanilla", "Gold Ore", createMinecraft("gold_ore"), stone, null, ToolInfo.NONE, false, 0, 0, -1));
		vanillaOres.add(createOreInfo("Vanilla", "Lapis Ore", createMinecraft("lapis_ore"), stone, null, ToolInfo.NONE, false, 0, 0, -1));
		vanillaOres.add(createOreInfo("Vanilla", "Diamond Ore", createMinecraft("diamond_ore"), stone, null, ToolInfo.NONE, false, 0, 0, -1));
		vanillaOres.add(createOreInfo("Vanilla", "Emerald Ore", createMinecraft("emerald_ore"), stone, null, ToolInfo.NONE, false, 0, 0, 80));
		vanillaOres.add(createOreInfo("Vanilla", "Redstone Ore", createMinecraft("redstone_ore"), stone, null, ToolInfo.NONE, false, 0, 0, -1));
		vanillaOres.add(createOreInfo("Vanilla", "Coal Ore", createMinecraft("coal_ore"), stone, null, ToolInfo.NONE, false, 0, 0, -1));
		vanillaOres.add(createOreInfo("Vanilla Nether", "Quartz Ore", createMinecraft("quartz_ore"), netherrack, null, ToolInfo.NONE, false, 0, 0, 160));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void registerBiome(final RegistryEvent.Register<Biome> event) {
		LogHelper.info("Starting ore block and item registry.");
		for (Map.Entry<ResourceLocation, DenseOreInfo> entry : oreInfos.entrySet()) {
			DenseOre ore = entry.getValue().register();
			ores.put(entry.getKey(), ore);

			LogHelper.info("Registered an ore block " + ore.info.configName + ".");
			ForgeRegistries.BLOCKS.register(ore.block);
			LogHelper.info("Registered an ore item " + ore.info.configName + ".");
			ForgeRegistries.ITEMS.register(ore.itemBlock);
		}
	}

	public static void registerOre(DenseOreInfo oreInfo) {
		oreInfos.put(oreInfo.name, oreInfo);
	}

	public static DenseOreInfo createOreInfo(String group, @Nullable String configName, BlockStateInfo ore, BlockStateInfo container, @Nullable String overrideTexture, ToolInfo tool, boolean dense, int retroGenId, int renderType, int maxColourDist) {
		return createOreInfo(group, configName, ore, container, ore, container, null, overrideTexture, tool, dense, retroGenId, renderType, maxColourDist);
	}


	public static DenseOreInfo createOreInfo(String group, @Nullable String configName, BlockStateInfo ore, BlockStateInfo container, BlockStateInfo texBaseOre, BlockStateInfo texBackdrop, @Nullable BlockStateInfo texNewBackdrop, @Nullable String overrideTexture, ToolInfo tool, boolean dense, int retroGenId, int renderType, int maxColourDist) {

		texNewBackdrop = texNewBackdrop == null?texBackdrop:texNewBackdrop;

		String resourceDomainOre = texBaseOre.getResourceLocation().getNamespace(); //this uses texBaseOre not ore, as ore might be by this mod!
		String resourceDomainContainer = container.getResourceLocation().getNamespace();

		if (!"minecraft".equals(resourceDomainOre) && !Loader.isModLoaded(resourceDomainOre.toLowerCase())) {
			return null;
		}
		StringBuilder containerNamePart = new StringBuilder();
		if (!resourceDomainOre.equals(resourceDomainContainer)) {
			containerNamePart.append(resourceDomainContainer).append("_");
		}

		containerNamePart.append(container.getResourceLocation().getPath()).append("_");
		for(Map.Entry<String, String> entry:container.getFixedOrderPropertyIterator()) {
			containerNamePart.append(entry.getValue()).append("_");
		}
		if (dense) {
			containerNamePart.append("dense_");
		}

		StringBuilder propNamePart = new StringBuilder();
		for(Map.Entry<String, String> entry:texBaseOre.getFixedOrderPropertyIterator()) {
			propNamePart.append("_").append(entry.getValue());
		}

		ResourceLocation name = new ResourceLocation("denseores", (resourceDomainOre + "_" + containerNamePart + texBaseOre.getResourceLocation().getPath() + propNamePart).toLowerCase(Locale.ENGLISH));

		if ("".equals(overrideTexture)) overrideTexture = null;

		if (configName == null) {
			configName = name.toString();
		}

		return new DenseOreInfo(group, configName, name, ore, container, texBaseOre, texBackdrop, texNewBackdrop, overrideTexture, tool, dense, retroGenId, renderType, maxColourDist);
	}

	//Look for valid ore dictionary references and add new ones
	public static void buildOreDictionary() {
		for (DenseOre ore : ores.values()) {

			if (ore.block.isValid()) {
				for (int oreid : OreDictionary.getOreIDs(ore.info.newBaseStack(1))) {
					String oreName = OreDictionary.getOreName(oreid);

					if (oreName.length() > 3 && oreName.startsWith("ore") && Character.isUpperCase(oreName.charAt(3))) {
						ore.info.baseOreDictionaryEntry = oreName;
						String newOreName = "dense" + oreName;
						ore.info.oreDictionary = newOreName;
						OreDictionary.registerOre(newOreName, new ItemStack(ore.block));
					}
				}
			}
		}
	}


}