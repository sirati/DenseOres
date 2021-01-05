package com.rwtema.denseores;


import com.rwtema.denseores.utils.Validate;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.Arrays;

import static com.rwtema.denseores.BlockStateInfo.writeAsDefault;

// Load the config file
public class DenseOresConfig {

	public final static DenseOresConfig instance = new DenseOresConfig();

	public final static String CATEGORY_ORE = "ores";
	public final static String CATEGORY_GROUP = "groups";
	public final static String CATEGORY_ACTIVE_GROUP = "active";
	public static final String BASE_BACKDROP = "base backdrop";
	public static final String BASE_ORE = "base ore";
	public static final String OVERRIDDEN_ORE_TEXTURE = "overridden ore texture";
	public static final String RETRO_GEN_ID = "retroGenID";
	public static final String PARENTS = "parents";
	public static final String GENERATE = "generate";
	public static final String DENSE = "dense";
	public static final String MAX_COLOUR_DIST = "maxColourDist";
	public static final String RENDER_TYPE = "renderType";
	public static final String PROVIDED = "provided";

	public void loadConfig(File file) {
		boolean generateConfig = file.exists() && file.length() < 100;

		Configuration config = new Configuration(file);

		config.load();

		if (generateConfig) {
			DenseOresRegistry.initVanillaOres();

			// 'get' the vanilla ore entries to ensure that they exist
			ObjectSet<String> vanillaGroups = new ObjectOpenHashSet<>();
			for (DenseOreInfo ore : DenseOresRegistry.vanillaOres) {
				vanillaGroups.add(ore.group);

				String cat = CATEGORY_GROUP + "." + ore.group + "." + CATEGORY_ORE + "." + ore.configName;

				writeAsDefault(config, cat, BASE_ORE, ore.texBaseOre);
				writeAsDefault(config, cat, BASE_BACKDROP, ore.texBackdrop);
				if (ore.overrideTexture != null) config.get(cat, OVERRIDDEN_ORE_TEXTURE, ore.overrideTexture);
				if (ore.rendertype != 0) config.get(cat, RENDER_TYPE, ore.rendertype);
				if (ore.maxColourDist >= 0) config.get(cat, MAX_COLOUR_DIST, ore.maxColourDist);
				//config.get(cat, "requiredMod", "minecraft");
			}
			for (String vanillaGroup:vanillaGroups) {
				config.getBoolean(PROVIDED, CATEGORY_GROUP + "." + vanillaGroup, true, "This group just represents the vanilla ores, it cannot be activated or inherited. Its only use is as a base for generators");
			}

			String cat = CATEGORY_GROUP + "." + DENSE;
			config.getBoolean(DENSE, cat + "." + GENERATE, true, "Lets generate dense ores for all vanilla ores");
			String[] vanillaGroupsArr = vanillaGroups.toArray(new String[0]);
			config.getStringList(PARENTS, cat, vanillaGroupsArr, "The list of vanilla groups");
		}


		ObjectSet<String> groupKeys = new ObjectOpenHashSet<>();
		Object2ObjectMap<String, ObjectSet<String>> groupOres = new Object2ObjectOpenHashMap<>();
		ObjectSet<String> ores = new ObjectOpenHashSet<>();


		for (String cat : config.getCategoryNames()) {
			String[] parts = cat.split("\\.");
			if (parts.length == 2) {
				if (parts[0].equals(CATEGORY_GROUP)) {
					groupKeys.add(parts[1]);
				} else if (parts[0].equals(CATEGORY_ORE)) {
					ores.add(parts[1]);
				}
			} else if (parts.length == 4) {
				if (parts[0].equals(CATEGORY_GROUP) && parts[2].equals(CATEGORY_ORE)) {
					String group = parts[1].toLowerCase();
					ObjectSet<String> thisOres = groupOres.computeIfAbsent(group, (k)-> new ObjectOpenHashSet<>());
					thisOres.add(parts[3]);
				}
			}
		}


		Object2ObjectMap<String, OreGroup> groups = new Object2ObjectOpenHashMap<>(groupKeys.size());
		for (String key : groupKeys) {
			groups.put(key.toLowerCase(), new OreGroup(key.toLowerCase()));
		}

		//load per group config
		String[] validGroupKeys = groupKeys.toArray(new String[0]);
		for (OreGroup group : groups.values()) {
			String longKey = CATEGORY_GROUP + "." + group.getName();
			group.isProvided = config.hasKey(longKey, PROVIDED) && config.get(longKey, PROVIDED, false).getBoolean();
			String[] parentKeys = config.getStringList(PARENTS, longKey, new String[0], "The groups, this group inherits from", validGroupKeys);
			for (String parentKey: parentKeys) {
				OreGroup parent = groups.get(parentKey.toLowerCase());
				Validate.notNull(parent, () -> String.format("Parent definition for: %s.\n There is no group with name: %s,\n available: %s", group.getName(), parentKey, Arrays.toString(validGroupKeys)));
				group.addParent(parent);
			}
			for (String oreKey: groupOres.getOrDefault(group.getName(), ObjectSets.emptySet())) {
				String oreLongKey = longKey + "." + CATEGORY_ORE + "." + oreKey;
				group.addOre(readDenseOre(config, oreLongKey, group.getName(), oreKey, false));
			}
			if (config.hasCategory(longKey + "." + GENERATE)) {
				group.generator = GeneratorInfo.read(config, longKey, GENERATE);
			}
		}

		//resolve dependencies
		for (OreGroup group : groups.values()) {
			group.init();
		}

		String[] activatableGroups = groups.values().stream().filter(g -> !g.isProvided).map(OreGroup::getName).toArray(String[]::new);
		String[] activeGroups = config.getStringList(CATEGORY_ACTIVE_GROUP, CATEGORY_GROUP, activatableGroups, "Add defined groups here to activate them", activatableGroups);
		for (String active:activeGroups) {
			OreGroup group = groups.get(active.toLowerCase());
			Validate.notNull(group, () -> String.format("Configuration file, active groups:\n Cannot find group %s, available: %s", active, Arrays.toString(activatableGroups)));
			Validate.isFalse(group.isProvided, () -> String.format("A provided group cannot be active, but group %s is marked as active!\n active groups: %s\nactivatable groups: %s", group.getName(), Arrays.toString(activeGroups), Arrays.toString(activatableGroups)));
			for (DenseOreInfo ore: group.getOres()) {
				DenseOresRegistry.registerOre(ore);
			}
		}

		// go through all categories and add them to the registry if they match
		for (String name : ores) {
			String cat = CATEGORY_ORE + "." + name;
			String requiredMod;

			if (config.hasKey(cat, "requiredMod") && !(requiredMod = config.get(cat, "requiredMod", "").getString()).equals("") && !"minecraft".equals(requiredMod) && !Loader.isModLoaded(requiredMod))
				return;

			// register the block
			if (config.hasKey(cat, BASE_ORE)) {
				DenseOresRegistry.registerOre(readDenseOre(config, cat, "NoGroup", name, true));
			}
		}

		config.save();
	}

	private DenseOreInfo readDenseOre(Configuration config, String baseKey, String group, String name, boolean dense) {
		BlockStateInfo baseBackDrop = BlockStateInfo.read(config, baseKey, BASE_BACKDROP);
		BlockStateInfo baseOre = BlockStateInfo.read(config, baseKey, BASE_ORE);


		return DenseOresRegistry.createOreInfo(
				group, name, baseOre, baseBackDrop, baseOre, baseBackDrop, null,
				config.hasKey(baseKey, OVERRIDDEN_ORE_TEXTURE) ? config.get(baseKey, OVERRIDDEN_ORE_TEXTURE, "").getString().trim() : null,
				ToolInfo.NONE,
				dense,
				config.get(baseKey, RETRO_GEN_ID, 0).getInt(),
				config.hasKey(baseKey, RENDER_TYPE) ? config.get(baseKey, RENDER_TYPE, 0).getInt(0) : 0,
				config.hasKey(baseKey, MAX_COLOUR_DIST) ? config.get(baseKey, MAX_COLOUR_DIST, -1).getInt() : -1);
	}
}
