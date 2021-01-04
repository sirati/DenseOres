package com.rwtema.denseores;

import com.rwtema.denseores.utils.LogHelper;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
@Mod.EventBusSubscriber(modid = DenseOresMod.MODID)
public class DenseOresRegistry {

	public static Map<ResourceLocation, DenseOre> ores = new HashMap<>();
	public static String blockPrefix = DenseOresMod.MODID;
	// add vanilla entries (TODO: add a way to disable vanilla ores)
	public static void initVanillaOres() {
		registerOre("Vanilla Iron Ore", new ResourceLocation("iron_ore"), Object2ObjectMaps.emptyMap(), "blocks/stone", "blocks/iron_ore", 0, 0);
		registerOre("Vanilla Gold Ore", new ResourceLocation("gold_ore"), Object2ObjectMaps.emptyMap(), "blocks/stone", "blocks/gold_ore", 0, 0);
		registerOre("Vanilla Lapis Ore", new ResourceLocation("lapis_ore"), Object2ObjectMaps.emptyMap(), "blocks/stone", "blocks/lapis_ore", 0, 0);
		registerOre("Vanilla Diamond Ore", new ResourceLocation("diamond_ore"), Object2ObjectMaps.emptyMap(), "blocks/stone", "blocks/diamond_ore", 0, 0);
		registerOre("Vanilla Emerald Ore", new ResourceLocation("emerald_ore"), Object2ObjectMaps.emptyMap(), "blocks/stone", "blocks/emerald_ore", 0, 0);
		registerOre("Vanilla Redstone Ore", new ResourceLocation("redstone_ore"), Object2ObjectMaps.emptyMap(), "blocks/stone", "blocks/redstone_ore", 0, 0);
		registerOre("Vanilla Coal Ore", new ResourceLocation("coal_ore"), Object2ObjectMaps.emptyMap(), "blocks/stone", "blocks/coal_ore", 0, 0);
		registerOre("Vanilla Quartz Ore", new ResourceLocation("quartz_ore"), Object2ObjectMaps.emptyMap(), "blocks/netherrack", "blocks/quartz_ore", 0, 0);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void registerBiome(final RegistryEvent.Register<Biome> event) {
		LogHelper.info("Starting ore block and item registry.");
		for (DenseOre ore : ores.values()) {
			LogHelper.info("Registered an ore block " + ore.unofficialName + ".");
			ForgeRegistries.BLOCKS.register(ore.block);
			LogHelper.info("Registered an ore item " + ore.unofficialName + ".");
			ForgeRegistries.ITEMS.register(ore.itemBlock);
		}
	}

	public static DenseOre registerOre(@Nullable String unofficialName, ResourceLocation baseBlock, Object2ObjectMap<String, String> properties, String underlyingBlock, @Nullable String texture, int retroGenId, int renderType) {
		if ("".equals(baseBlock.toString()) || "minecraft:air".equals(baseBlock.toString()))
			return null;

		String resourceDomain = baseBlock.getNamespace();

		if (!"minecraft".equals(resourceDomain) && !Loader.isModLoaded(resourceDomain.toString().toLowerCase())) {
			return null;
		}
		StringBuilder propNamePart = new StringBuilder();
		for(Map.Entry<String, String> entry:properties.entrySet()) {
			propNamePart.append("_").append(entry.getValue());

		}

		ResourceLocation name = new ResourceLocation("denseores", (resourceDomain + "_" + baseBlock.getPath() + propNamePart).toLowerCase(Locale.ENGLISH));

		if ("".equals(texture)) texture = null;

		if (unofficialName == null) {
			unofficialName = name.toString();
		}

		DenseOre ore = new DenseOre(unofficialName, name, baseBlock, properties, underlyingBlock, texture, retroGenId, renderType);
		ores.put(name, ore);
		return ore;
	}

	//Look for valid ore dictionary references and add new ones
	public static void buildOreDictionary() {
		for (DenseOre ore : ores.values()) {

			if (ore.block.isValid()) {
				for (int oreid : OreDictionary.getOreIDs(ore.newBaseStack(1))) {
					String oreName = OreDictionary.getOreName(oreid);

					if (oreName.length() > 3 && oreName.startsWith("ore") && Character.isUpperCase(oreName.charAt(3))) {
						ore.baseOreDictionaryEntry = oreName;
						String newOreName = "dense" + oreName;
						ore.oreDictionary = newOreName;
						OreDictionary.registerOre(newOreName, new ItemStack(ore.block));
					}
				}
			}
		}
	}


}