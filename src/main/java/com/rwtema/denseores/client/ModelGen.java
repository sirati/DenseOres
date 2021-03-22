package com.rwtema.denseores.client;

import com.rwtema.denseores.DenseOre;
import com.rwtema.denseores.DenseOreInfo;
import com.rwtema.denseores.DenseOresRegistry;
import com.rwtema.denseores.blocks.BlockDenseOre;
import com.rwtema.denseores.utils.LogHelper;
import com.rwtema.denseores.utils.ModelBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

public class ModelGen {

	public static void register() {
		MinecraftForge.EVENT_BUS.register(ModelGen.class);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SideOnly(Side.CLIENT)
	public static void loadTextures(TextureStitchEvent.Pre event) {
		ModelManager manager = ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "modelManager", "field_175617_aL","field_178090_d","field_178128_c");

		BlockModelShapes shapes = manager.getBlockModelShapes();
		BlockStateMapper mapper = shapes.getBlockStateMapper();
		for (DenseOreInfo ore : DenseOresRegistry.oreInfos.values()) {
			if (ore.overrideTexture != null) {
				continue;
			}
			IBlockState state = ore.getBaseState();
			Map<IBlockState, ModelResourceLocation> map = mapper.getVariants(ore.getBaseBlock());
			ModelResourceLocation modelResourceLocation = map.get(state);

			IModel model = null;
			try {
				model = ModelLoaderRegistry.getModel(modelResourceLocation);
			} catch (Exception e) {
				continue;
			}

			Collection<ResourceLocation> textures = model.getTextures();
			ModelResourceLocation backdrop = mapper.getVariants(ore.texBackdrop.getBlock()).get(ore.texBackdrop.getBlockState());

			for (ResourceLocation texture : textures) {
				if (!texture.equals(backdrop)) {
					ore.overrideTexture = texture.toString();
					break;
				}
			}

		}

		for (DenseOreInfo ore : DenseOresRegistry.oreInfos.values()) {
			ModelResourceLocation backdrop = mapper.getVariants(ore.texBackdrop.getBlock()).get(ore.texBackdrop.getBlockState());
			IModel modelBase = null;
			try {
				modelBase = ModelLoaderRegistry.getModel(backdrop);
			} catch (Exception e) {
				ore.sprite = event.getMap().getMissingSprite();
				continue;
			}
			IModel modelNewBase = modelBase;
			if (ore.texBackdrop != ore.texNewBackdrop) {
				ModelResourceLocation backdropNew = mapper.getVariants(ore.texNewBackdrop.getBlock()).get(ore.texNewBackdrop.getBlockState());
				try {
					modelNewBase = ModelLoaderRegistry.getModel(backdropNew);
				} catch (Exception e) {
					modelNewBase = modelBase;
				}
			}

			if (ore.overrideTexture == null || "".equals(ore.overrideTexture)) {
				ore.sprite = event.getMap().getMissingSprite();
			} else {
				TextureOre textureOre = new TextureOre(ore, modelBase.getTextures().iterator().next(), modelNewBase.getTextures().iterator().next());
				event.getMap().setTextureEntry(textureOre);
				ore.sprite = event.getMap().getTextureExtry(textureOre.getIconName());
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SideOnly(Side.CLIENT)
	public static void addModels(ModelBakeEvent event) {
		IRegistry<ModelResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();
		ModelManager manager = event.getModelManager();
		BlockModelShapes shapes = manager.getBlockModelShapes();
		BlockStateMapper mapper = shapes.getBlockStateMapper();

		for (DenseOre denseOre : DenseOresRegistry.ores.values()) {
			BlockDenseOre block = denseOre.block;
			Item item = Item.getItemFromBlock(block);
			modelRegistry.putObject(new ModelResourceLocation(Item.REGISTRY.getNameForObject(item), "inventory"), new EmptyBakedModel());

			Map<IBlockState, ModelResourceLocation> locations = new DefaultStateMapper().putStateModelLocations(block);
			for (IBlockState iBlockState : block.getBlockState().getValidStates()) {
				ModelResourceLocation blockLocation = locations.get(iBlockState);
				ModelResourceLocation inventoryLocation = new ModelResourceLocation(Item.REGISTRY.getNameForObject(item) + "_" + "dense", "inventory");

				ModelResourceLocation location = mapper.getVariants(denseOre.info.getBaseBlock()).get(denseOre.getBaseState());
				IBakedModel parentModel = null;
				if (location != null) {
					parentModel = modelRegistry.getObject(location);
				}

				if (parentModel == null) {
					parentModel = modelRegistry.getObject(mapper.getVariants(Blocks.STONE).get(Blocks.STONE.getDefaultState()));
				}

				IBakedModel iBakedModel = ModelBuilder.changeIcon(denseOre.getBaseState(), parentModel, denseOre.info.sprite);

				modelRegistry.putObject(blockLocation, iBakedModel);
				modelRegistry.putObject(inventoryLocation, iBakedModel);
			}

		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SideOnly(Side.CLIENT)
	public static void registerMesh() {

		LogHelper.info("registerMesh waw called, mixin worked");
		ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();

		for (DenseOre denseOre : DenseOresRegistry.ores.values()) {
			BlockDenseOre block = denseOre.block;
			Item item = Item.getItemFromBlock(block);

			final ModelResourceLocation[] invModels = new ModelResourceLocation[1];
			ModelResourceLocation inventoryLocation = new ModelResourceLocation(Item.REGISTRY.getNameForObject(item) + "_" + "dense", "inventory");

			mesher.register(item, 0, inventoryLocation);
			invModels[0] = inventoryLocation;

			mesher.register(item, new ItemMeshDefinition() {
				@Nonnull
				@Override
				public ModelResourceLocation getModelLocation(@Nonnull ItemStack stack) {

					return invModels[0];
				}
			});
		}
	}
}
