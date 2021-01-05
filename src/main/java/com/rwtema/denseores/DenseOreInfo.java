package com.rwtema.denseores;

import com.rwtema.denseores.blocks.BlockDenseOre;
import com.rwtema.denseores.blocks.ItemBlockDenseOre;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Map;

/*
 * Dense ore entry
 * 
 * holds data for when we need it
 */
public class DenseOreInfo {
	public final String configName;
	public final ResourceLocation name;
	public final String group;
	public final int maxColourDist;


	public int rendertype;

	@Nullable
	public String overrideTexture;
	public final BlockStateReference ore;
	public final BlockStateReference container;
	public final BlockStateReference texBaseOre;
	public final BlockStateReference texBackdrop;
	public final BlockStateReference texNewBackdrop;

	public int retroGenId;
	public final ToolInfo tool;
	public final boolean dense;


	@SideOnly(Side.CLIENT)
	public TextureAtlasSprite sprite;
	public String baseOreDictionaryEntry;
	boolean initSmelt = false;
	private ItemStack smelt;
	public String oreDictionary;


	public DenseOreInfo(String group, String configName, ResourceLocation name, BlockStateInfo ore, BlockStateInfo container, BlockStateInfo texBaseOre, BlockStateInfo texBackdrop, @Nullable BlockStateInfo texNewBackdrop, @Nullable String overrideTexture, ToolInfo tool, boolean dense, int retroGenId, int renderType, int maxColourDist) {
		this.group = group;
		this.configName = configName;
		this.name = name;
		this.ore = ore.create();
		this.container = container.create();
		this.texBaseOre = texBaseOre.create();
		this.texBackdrop = texBackdrop.create();
		this.texNewBackdrop = texNewBackdrop ==null?this.texBackdrop : texNewBackdrop.create();
		this.overrideTexture = overrideTexture == null ? null:overrideTexture.toLowerCase();
		this.tool = tool;
		this.dense = dense;
		this.retroGenId = retroGenId;
		this.rendertype = renderType;

		this.maxColourDist = maxColourDist;
	}

	public DenseOre register() {
		BlockDenseOre block;
		ItemBlockDenseOre itemBlock;
		block = new BlockDenseOre(this);
		block.setRegistryName(this.name);
		block.setTranslationKey(this.name.toString());
		itemBlock = new ItemBlockDenseOre(block);
		itemBlock.setRegistryName(this.name);
		itemBlock.setTranslationKey(this.name.toString());
		return new DenseOre(block, itemBlock, this);
	}



	public Block getBaseBlock() {
		return texBaseOre.getBlock();
	}

	public ItemStack newBaseStack(int stacksize) {
		return texBaseOre.newStack(1);
	}

	public IBlockState getBaseState() {
		return texBaseOre.getBlockState();
	}

	private static String convertWithIteration(Map<String, String> map) {
		if (map.size() == 0) return "{}";
		StringBuilder mapAsString = new StringBuilder("{");
		for (Map.Entry<String, String> entry : map.entrySet()) {
			mapAsString.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
		}
		mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
		return mapAsString.toString();
	}

	@Override
	public String toString() {
		return "DenseOre{" +
				"configName='" + configName + '\'' +
				", group=" + group +
				", name=" + name +
				", rendertype=" + rendertype +
				", ore={id=" + ore.getResourceLocation() +
				", properties=" + convertWithIteration(ore.getPropertyLookup()) + "}" +
				", container={id=" + container.getResourceLocation() +
				", properties=" + convertWithIteration(container.getPropertyLookup()) + "}" +
				", texBaseOre={id=" + texBaseOre.getResourceLocation() +
				", properties=" + convertWithIteration(texBaseOre.getPropertyLookup()) + "}" +
				", texBackdrop={id=" + texBackdrop.getResourceLocation() +
				", properties=" + convertWithIteration(texBackdrop.getPropertyLookup()) + "}" +
				", texNewBackdrop={id=" + texNewBackdrop.getResourceLocation() +
				", properties=" + convertWithIteration(texNewBackdrop.getPropertyLookup()) + "}" +
				", overrideTexture='" + overrideTexture + '\'' +
				", retroGenId=" + retroGenId +
				", maxColourDist=" + maxColourDist +
				'}';
	}

	public ItemStack getSmeltingRecipe() {
		if (initSmelt)
			return smelt;

		initSmelt = true;
		ItemStack out = FurnaceRecipes.instance().getSmeltingResult(newBaseStack(1));

		if (out.isEmpty()) {
			out = out.copy();
			out.setCount(Math.min(3, out.getMaxStackSize()));
		}

		smelt = out;

		return out;
	}

}
