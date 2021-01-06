package com.rwtema.denseores;

import com.rwtema.denseores.client.ModelGen;
import com.rwtema.denseores.cubicchunks.CCHelper;
import com.rwtema.denseores.utils.LogHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.io.File;

import static com.rwtema.denseores.BlockStateInfo.*;

@Mod(modid = DenseOresMod.MODID, name = "Dense Ores", version = DenseOresMod.VERSION, acceptedMinecraftVersions = "[1.12.2]", dependencies = DenseOresMod.DEPENCIES)
public class DenseOresMod {
	public static final String MODID = "denseores";
	public static final String VERSION = "1.0";
	public static final String DEPENCIES = "required:cubicchunks@[0.0.989.0,);required:cubicgen@[0.0.67.0,);required:forge@[14.23.3.2658,)";

	@SidedProxy(serverSide = "com.rwtema.denseores.Proxy", clientSide = "com.rwtema.denseores.ProxyClient")
	public static Proxy proxy;


	public static RuntimeException wrap(RuntimeException throwable) {
		return proxy.wrap(throwable);
	}

	private File config;

	public DenseOresMod() {
		ModelGen.register();
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = event.getSuggestedConfigurationFile();
		LogHelper.info("Loading the config.");
		DenseOresConfig.instance.loadConfig(config);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		LogHelper.info("Ph'nglui mglw'nafh, y'uln Dense Ores shugg ch'agl");
		for (FMLInterModComms.IMCMessage message : FMLInterModComms.fetchRuntimeMessages(this)) {
			String key = message.key;
			try {
				if (key.startsWith("addDenseOre")) {
					Class<?> messageType = message.getMessageType();

					int rendertype = 0;
					BlockStateInfo backdrop = createMinecraft("stone");
					switch (key.substring("addDenseOre".length())) {
						case "Stone":
							backdrop = createMinecraft("stone");
							break;
						case "Netherrack":
							backdrop = createMinecraft("netherrack");
							break;
						case "EndStone":
							backdrop = createMinecraft("end_stone");
							break;
						case "Obsidian":
							backdrop = createMinecraft("obsidian");
							break;
					}
					BlockStateInfo baseOre;
					@Nullable
					String overrideTexture = null;

					String unofficialName = null;

					if (messageType == ItemStack.class) {
						baseOre = read(message.getItemStackValue());
						
						
					} else if (messageType == NBTTagCompound.class) {
						NBTTagCompound nbt = message.getNBTValue();
						baseOre = read(nbt.getCompoundTag("baseOre"));
						if (nbt.hasKey("overrideTexture", Constants.NBT.TAG_COMPOUND)) {
							backdrop = read(nbt.getCompoundTag("overrideTexture"));
						}
						if (nbt.hasKey("overrideTexture", Constants.NBT.TAG_STRING)) {
							overrideTexture = nbt.getString("overrideTexture");
						}
						rendertype = nbt.getInteger("renderType");
						unofficialName = nbt.getString("config_entry");
					} else {
						throw new IllegalArgumentException("Unable to process IMC type: " + messageType);
					}

					if(unofficialName == null || "".equals(unofficialName)){
						unofficialName = null;
					}
					DenseOresRegistry.createOreInfo(message.getSender(),
							unofficialName, baseOre, backdrop, baseOre, backdrop, null, overrideTexture, ToolInfo.NONE, true, 0, rendertype, -1
					);
				}
			} catch (Exception err) {
				throw new ReportedException(new CrashReport("Unabled to load IMC message from " + message.getSender(), err));
			}
		}

		ModIntegration.addModIntegration();
		LogHelper.info("Building the ore dictionary.");
		DenseOresRegistry denseore_registry = new DenseOresRegistry();
		MinecraftForge.EVENT_BUS.register(denseore_registry);
		denseore_registry.buildOreDictionary();
		LogHelper.info("Registering the world generator.");
		WorldGenOres worldGen = new WorldGenOres();
		GameRegistry.registerWorldGenerator(worldGen, 1000);
		MinecraftForge.EVENT_BUS.register(worldGen);
	}

	public static boolean finishedStartup;
	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit();
		LogHelper.info("Ores are fully densified.");
		finishedStartup = true;
		if (Loader.isModLoaded("cubicchunks")) {
			CCHelper.register();
		}
		DenseOresRegistry.buildLookup();
	}


	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
	}

}
