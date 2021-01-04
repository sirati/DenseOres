package com.rwtema.denseores.blocks;


import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.rwtema.denseores.DenseOre;
import com.rwtema.denseores.blockaccess.BlockAccessSingleOverride;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/*  I'm using the MAX_METADATA metadata values to store each ore block.
 *  (We don't really need to worry about block ids in 1.7
 *   but that's no reason to be wasteful)
 */

public class BlockDenseOre extends Block {
	public boolean init = false;

	DenseOre denseOre;
	// Ore Entry stuff
	IBlockState baseBlockState;
	private boolean isValid;
	private Block baseBlock;

	public BlockDenseOre(DenseOre denseOre) {
		super(Material.ROCK);
		this.denseOre = denseOre;
		setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
	}

	public static Block getBaseBlock(DenseOre ore) {
		return ore != null ? Block.REGISTRY.getObject(ore.baseBlock) : null;
	}

	public static Block getNullOverride(IBlockAccess world, BlockPos pos) {
		if (world == null)
			return Blocks.STONE;

		Biome biome = world.getBiome(pos);
		if (biome == Biomes.HELL)
			return Blocks.NETHERRACK;

		if (biome == Biomes.SKY)
			return Blocks.END_STONE;

		return getNullOverride(world);
	}

	public static Block getNullOverride(IBlockAccess blockAccess) {
		if (!(blockAccess instanceof World))
			return Blocks.STONE;

		World world = (World) blockAccess;

		return getBaseBlock(world);
	}

	public static Block getBaseBlock(World world) {
		if (world.provider == null)
			return Blocks.STONE;

		if (world.provider.getDimension() == -1)
			return Blocks.NETHERRACK;

		if (world.provider.getDimension() == 1)
			return Blocks.END_STONE;

		return Blocks.STONE;
	}

	public Block getUnderlyingBlock(IBlockAccess world, BlockPos pos) {
		if ("blocks/stone".equals(denseOre.underlyingBlockTexture)) {
			return Blocks.STONE;
		}
		if ("blocks/netherrack".equals(denseOre.underlyingBlockTexture)) {
			return Blocks.NETHERRACK;
		}
		if ("blocks/end_stone".equals(denseOre.underlyingBlockTexture)) {
			return Blocks.END_STONE;
		}

		return getNullOverride(world, pos);
	}

	public boolean isValid() {
		if (!init) init();
		return isValid;
	}

	public void init() {
		init = true;

		baseBlock = denseOre.getBaseBlock();
		baseBlockState = baseBlock.getDefaultState();

		Object2ObjectMap<String, IProperty<?>> properties = new Object2ObjectOpenHashMap<>();
		for (IProperty<?> p:baseBlockState.getPropertyKeys()) {
			properties.put(p.getName(), p);
		}

		for (Map.Entry<String, String> entry : denseOre.propertyLookup.entrySet()) {
			IProperty<?> p = properties.get(entry.getKey());
			if (p == null) throw new IllegalStateException(String.format("There is no property names %s for block %s", entry.getKey(), baseBlock));
			baseBlockState = setProperty(baseBlockState, p, entry.getValue());
		}

		isValid = baseBlock != null && baseBlock != Blocks.AIR;
	}

	private static <T extends Comparable<T>> IBlockState setProperty(IBlockState state, IProperty<T> property, String valueStr) {
		Optional<T> valueParsed = property.parseValue(valueStr);
		return state.withProperty(property, valueParsed.toJavaUtil().orElseThrow(() ->
				new IllegalStateException(String.format("Block %s's property %s does not have the value %s! Allowed: %s", state.getBlock(), property.getName(), valueStr, Arrays.toString(property.getAllowedValues().toArray(new Object[0]))))));
	}

	@Nonnull
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this);
	}

	public Block getBaseBlock() {
		if (!init) init();
		return baseBlock;
	}

	public IBlockState getBaseBlockState() {
		if (!init) init();
		return baseBlockState;
	}

	@Override
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
		if (!isValid())
			return;

		try {
			world.setBlockState(pos, getBaseBlockState(), 0);
			for (int i = 0; i < 1 + rand.nextInt(3); i++)
				getBaseBlock().randomDisplayTick(getBaseBlockState(), world, pos, rand);
		} finally {
			world.setBlockState(pos, state, 0);
		}
	}

	// drop the block with a predefined chance
	@Override
	public void dropBlockAsItemWithChance(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, float chance, int fortune) {
		if (worldIn.isRemote || worldIn.restoringBlockSnapshots)// do not drop items while restoring blockstates, prevents item dupe
			return;

		List<ItemStack> items = getDrops(worldIn, pos, state, fortune);
		chance = net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, state, fortune, chance, false, harvesters.get());

		if (chance == 0) return;

		// now call the forge events to see if our base ore block should be dropped
		if (isValid()) {
			IBlockState base = getBaseBlockState();

			if (base != null) {
				chance = net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, base, fortune, chance, false, harvesters.get());
			}
		}

		if (chance == 0) return;

		for (ItemStack item : items) {
			if (worldIn.rand.nextFloat() <= chance) {
				spawnAsEntity(worldIn, pos, item);
			}
		}
	}


	// get drops
	@Nonnull
	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune) {
		ArrayList<ItemStack> list = new ArrayList<>();

		if (isValid()) {
			Block base = getBaseBlock();

			if (base == null)
				return list;

			IBlockState m = getBaseBlockState();

			BlockAccessSingleOverride delegate = new BlockAccessSingleOverride(world, m, pos);

			Random rand = world instanceof World ? ((World) world).rand : RANDOM;

			// get base drops 3 times
			for (int j = 0; j < 3; j++) {
				list.addAll(base.getDrops(delegate, pos, m, fortune));
			}
		} else {
			Block block = getNullOverride(world, pos);
			BlockAccessSingleOverride delegate = new BlockAccessSingleOverride(world, block.getDefaultState(), pos);
			return block.getDrops(delegate, pos, block.getDefaultState(), fortune);
		}
		return list;
	}

	// get hardness
	@Override
	public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
		if (state.getBlock() != this) return 1;


		if (!isValid())
			return 1;


		TileEntity tile = world.getTileEntity(pos);
		try {
			IBlockState baseBlockState = getBaseBlockState();
			world.setBlockState(pos, baseBlockState, 0);
			float blockHardness = getBaseBlock().getBlockHardness(baseBlockState, world, pos);
			world.setBlockState(pos, state, 0);
			if (tile != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tile.writeToNBT(tag);
				TileEntity newTile = world.getTileEntity(pos);
				newTile.readFromNBT(tag);
			}

			return blockHardness;
		} catch (Throwable throwable) {
			world.setBlockState(pos, state, 0);
			if (tile != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tile.writeToNBT(tag);
				TileEntity newTile = world.getTileEntity(pos);
				newTile.readFromNBT(tag);
			}

			throw Throwables.propagate(throwable);
		}
	}

	@Override
	public int getExpDrop(IBlockState state, IBlockAccess iBlockAccess, BlockPos pos, int fortune) {
		if (!(iBlockAccess instanceof World) || !isValid()) return 0;

		World world = ((World) iBlockAccess);

		IBlockState baseState = getBaseBlockState();
		BlockAccessSingleOverride delegate = new BlockAccessSingleOverride(iBlockAccess, baseState, pos);
		return baseState.getBlock().getExpDrop(
				baseState,
				delegate,
				pos, fortune);
	}

	@Override
	public int damageDropped(IBlockState state) {
		return getMetaFromState(state);
	}

	@Override
	public boolean canSilkHarvest(World world, BlockPos pos, @Nonnull IBlockState state, EntityPlayer player) {
		return state.getBlock() == this && isValid();
	}


	@Nonnull
	@Override
	public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos) {

		return state;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return false;
	}

	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		if (world.isRemote) return null;
		return null;
	}

	@Override
	public int getHarvestLevel(@Nonnull IBlockState state) {
		IBlockState baseState = getBaseBlockState();
		return baseState.getBlock().getHarvestLevel(baseState);
	}

	@Override
	public String getHarvestTool(@Nonnull IBlockState state) {
		IBlockState baseState = getBaseBlockState();
		return baseState.getBlock().getHarvestTool(baseState);
	}

	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
		baseBlock.onBlockHarvested(worldIn, pos, baseBlockState, player);
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		return baseBlock.removedByPlayer(baseBlockState, world, pos, player, willHarvest);
	}

	@Override
	public boolean canProvidePower(IBlockState state) {
		return baseBlock.canProvidePower(baseBlockState);
	}

	@Override
	public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
		return baseBlock.isFlammable(new FakeWorld(world, pos, baseBlockState), pos, face);
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
		return baseBlock.getFireSpreadSpeed(new FakeWorld(world, pos, baseBlockState), pos, face);
	}

	@Override
	public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
		return baseBlock.getFlammability(new FakeWorld(world, pos, baseBlockState), pos, face);
	}

	@Override
	public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		return baseBlock.getWeakPower(baseBlockState, new FakeWorld(blockAccess, pos, baseBlockState), pos, side);
	}

	@Override
	public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
		return baseBlock.canHarvestBlock(new FakeWorld(world, pos, baseBlockState), pos, player);
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		return baseBlock.getLightValue(baseBlockState, world, pos);
	}

	@Override
	public Material getMaterial(IBlockState state) {
		return baseBlock.getMaterial(baseBlockState);
	}

	@Override
	public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
		return baseBlock.getSoundType(baseBlockState, world, pos, entity);
	}

	@Override
	public SoundType getSoundType() {
		return baseBlock.getSoundType();
	}

	private static class FakeWorld implements IBlockAccess {
		private final IBlockAccess wrapped;
		private final BlockPos pos;
		private final IBlockState replaceWith;

		private FakeWorld(IBlockAccess wrapped, BlockPos pos, IBlockState replaceWith) {
			this.wrapped = wrapped;
			this.pos = pos;
			this.replaceWith = replaceWith;
		}

		@Nullable
		@Override
		public TileEntity getTileEntity(BlockPos pos) {
			return wrapped.getTileEntity(pos);
		}

		@Override
		public int getCombinedLight(BlockPos pos, int lightValue) {
			return wrapped.getCombinedLight(pos, lightValue);
		}

		@Override
		public IBlockState getBlockState(BlockPos pos) {
			if (pos.equals(this.pos)) return replaceWith;
			return wrapped.getBlockState(pos);
		}

		@Override
		public boolean isAirBlock(BlockPos pos) {
			return wrapped.isAirBlock(pos);
		}

		@Override
		public Biome getBiome(BlockPos pos) {
			return wrapped.getBiome(pos);
		}

		@Override
		public int getStrongPower(BlockPos pos, EnumFacing direction) {
			return wrapped.getStrongPower(pos, direction);
		}

		@Override
		public WorldType getWorldType() {
			return wrapped.getWorldType();
		}

		@Override
		public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
			return wrapped.isSideSolid(pos, side, _default);
		}
	}
}
