package com.rwtema.denseores.blocks;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockDenseOre extends ItemBlock {
	BlockDenseOre oreBlock;

	public ItemBlockDenseOre(Block block) {
		super(block);
		oreBlock = (BlockDenseOre) block;
		this.setHasSubtypes(true);
	}

	public int getMetadata(int par1) {
		return par1;
	}

	private String displayName;
	private boolean displayNameCached;

	// Adds the 'dense' qualifier to the base blocks name
	@Nonnull
	public String getItemStackDisplayName(@Nonnull ItemStack stack) {
		if (!oreBlock.isValid())
			return "Invalid Ore";
		else {
			if (!displayNameCached) {
				ItemStack temp = oreBlock.denseOre.texBaseOre.newStack(1);//texBaseOre on purpose
				ItemStack tempContainer = oreBlock.denseOre.container.newStack(1);

				String p = ("" + I18n.translateToLocal( oreBlock.denseOre.dense?"denseores.dense.prefix":"denseores.variant")).trim();
				p =  p.replaceFirst("ORENAME", temp.getDisplayName());
				displayName =  p.replaceFirst("EMBETNAME", tempContainer.getDisplayName());
				displayNameCached = !p.contains("denseores");
				if (!displayNameCached) {
					return "Lang Missing:" + (oreBlock.denseOre.dense?" Dense ":" ") + tempContainer.getDisplayName() + " " + temp.getDisplayName();
				}
			} //todo invalidate if language changed, use LanguageMap.getLastUpdateTimeInMilliseconds NEED AT forgebot currently down
			return displayName;
		}
	}

	@Override
	public boolean hasCustomEntity(ItemStack stack) {
		return !oreBlock.isValid();
	}

	@Override
	public Entity createEntity(World world, Entity location, ItemStack itemstack) {
		return new EntityItem(world, location.posX, location.posY, location.posZ,
				new ItemStack(BlockDenseOre.getNullOverride(world, new BlockPos(location)), itemstack.getCount()));
	}

}
