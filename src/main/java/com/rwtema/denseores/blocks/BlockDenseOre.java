package com.rwtema.denseores.blocks;


import com.google.common.base.Throwables;
import com.rwtema.denseores.DenseOreInfo;
import com.rwtema.denseores.DenseOresRegistry;
import com.rwtema.denseores.blockaccess.BlockAccessDelegate;
import com.rwtema.denseores.blockaccess.BlockAccessSingleOverride;
import com.rwtema.denseores.material.MaterialDelegate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;

/*  I'm using the MAX_METADATA metadata values to store each ore block.
 *  (We don't really need to worry about block ids in 1.7
 *   but that's no reason to be wasteful)
 */

public class BlockDenseOre extends Block {
	DenseOreInfo denseOre;

	public BlockDenseOre(DenseOreInfo denseOre) {
		super(Material.ROCK);
		this.denseOre = denseOre;
		setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
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
		return denseOre.texBackdrop.getBlock();
	}

	public boolean isValid() {
		return denseOre.texBaseOre.isValid();
	}



	@Nonnull
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this);
	}

	public Block getBaseOreBlock() {
		return denseOre.ore.getBlock();
	}

	public IBlockState getBaseOreBlockState() {
		return denseOre.ore.getBlockState();
	}
	
	public Block getContainerBlock() {
		return denseOre.container.getBlock();
	}

	public IBlockState getContainerBlockState() {
		return denseOre.container.getBlockState();
	}

	@Override
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
		if (!isValid()) {
			return;
		}

		try {
			world.setBlockState(pos, getBaseOreBlockState(), 0);
			for (int i = 0; i < 1 + rand.nextInt(3); i++) {
				getBaseOreBlock().randomDisplayTick(getBaseOreBlockState(), world, pos, rand);
			}
			world.setBlockState(pos, getContainerBlockState(), 0);
			for (int i = 0; i < 1 + rand.nextInt(3); i++) {
				getContainerBlock().randomDisplayTick(getContainerBlockState(), world, pos, rand);
			}
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
			IBlockState base = getBaseOreBlockState();

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
			Block base = getBaseOreBlock();

			if (base == null)
				return list;

			IBlockState m = getBaseOreBlockState();

			BlockAccessSingleOverride delegate = new BlockAccessSingleOverride(world, m, pos);



			if (denseOre.dense) {
				Random rand = world instanceof World ? ((World) world).rand : RANDOM;
				int i = rand.nextInt(fortune + 2) - 1;

				if (i < 0)
				{
					i = 0;
				}

				i += 1;
				i*=3;
				// get base drops 3 times
				for (int j = 0; j < i; j++) {
					list.addAll(base.getDrops(delegate, pos, m, fortune));
				}
			} else {
				list.addAll(base.getDrops(delegate, pos, m, fortune));
			}
			if (DenseOresRegistry.dropContainer && !(world instanceof BlockAccessDelegate)) {
				m = getContainerBlockState();
				delegate = new BlockAccessSingleOverride(world, m, pos);
				list.addAll(getContainerBlock().getDrops(delegate, pos, m, fortune));
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
			IBlockState baseBlockState = getBaseOreBlockState();
			world.setBlockState(pos, baseBlockState, 0);
			float blockHardness1 = getBaseOreBlock().getBlockHardness(baseBlockState, world, pos);
			IBlockState containerBlockState = getContainerBlockState();
			world.setBlockState(pos, containerBlockState, 0);
			float blockHardness2 = getBaseOreBlock().getBlockHardness(containerBlockState, world, pos);
			world.setBlockState(pos, state, 0);
			
			
			

			return Math.max(blockHardness1, blockHardness2);
		} catch (Throwable throwable) {
			world.setBlockState(pos, state, 0);

			throw Throwables.propagate(throwable);
		}
	}

	@Override
	public int getExpDrop(IBlockState state, IBlockAccess iBlockAccess, BlockPos pos, int fortune) {
		if (!(iBlockAccess instanceof World) || !isValid()) return 0;

		World world = ((World) iBlockAccess);

		IBlockState baseState = getBaseOreBlockState();
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
		IBlockState baseState = getBaseOreBlockState();
		if (denseOre.tool.minToolLevel < 0) {
			IBlockState containerState = getContainerBlockState();
			return Math.max(baseState.getBlock().getHarvestLevel(baseState), containerState.getBlock().getHarvestLevel(containerState)) + denseOre.tool.toolLevelOffset;
		} else {

			return Math.max(baseState.getBlock().getHarvestLevel(baseState) + denseOre.tool.toolLevelOffset, denseOre.tool.minToolLevel);
		}
	}

	@Override
	public String getHarvestTool(@Nonnull IBlockState state) {
		if (denseOre.tool.hasReplaceTool())return denseOre.tool.getTool();
		IBlockState containerState = getContainerBlockState();
		return containerState.getBlock().getHarvestTool(containerState);
	}

	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
		getBaseOreBlock().onBlockHarvested(worldIn, pos, getBaseOreBlockState(), player);
		getContainerBlock().onBlockHarvested(worldIn, pos, getContainerBlockState(), player);
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		return getBaseOreBlock().removedByPlayer(getBaseOreBlockState(), world, pos, player, willHarvest);
	}

	@Override
	public boolean canProvidePower(IBlockState state) {
		return getBaseOreBlock().canProvidePower(getBaseOreBlockState()) ||
		 getContainerBlock().canProvidePower(getContainerBlockState());
	}

	@Override
	public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
		return getBaseOreBlock().isFlammable(new BlockAccessSingleOverride(world, getBaseOreBlockState(), pos), pos, face) ||
		 getContainerBlock().isFlammable(new BlockAccessSingleOverride(world, getContainerBlockState(), pos), pos, face);
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
		return Math.max(getBaseOreBlock().getFireSpreadSpeed(new BlockAccessSingleOverride(world, getBaseOreBlockState(), pos), pos, face),
		  getContainerBlock().getFireSpreadSpeed(new BlockAccessSingleOverride(world, getContainerBlockState(), pos), pos, face));
	}

	@Override
	public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
		return Math.max(getBaseOreBlock().getFlammability(new BlockAccessSingleOverride(world, getBaseOreBlockState(), pos), pos, face),
				getContainerBlock().getFlammability(new BlockAccessSingleOverride(world, getContainerBlockState(), pos), pos, face));
	}

	@Override
	public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		return getBaseOreBlock().getWeakPower(getBaseOreBlockState(), new BlockAccessSingleOverride(blockAccess, getBaseOreBlockState(), pos), pos, side);
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		return Math.max(getBaseOreBlock().getLightValue(getBaseOreBlockState(), world, pos),
		  getContainerBlock().getLightValue(getContainerBlockState(), world, pos));
	}

	private MaterialDelegate material;
	@Override
	public Material getMaterial(IBlockState state) {
		if (material == null || !denseOre.container.isValid()) {
			material = new MaterialDelegate(getContainerBlock().getMaterial(getContainerBlockState()));
		}
		return material;
	}

	@Override
	public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
		return getContainerBlock().getSoundType(getContainerBlockState(), world, pos, entity);
	}

	@Override
	public SoundType getSoundType() {
		return getContainerBlock().getSoundType();
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		getBaseOreBlock().onBlockAdded(worldIn, pos, getBaseOreBlockState());
		if (getBaseOreBlock() instanceof BlockFalling) {
			worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
		}
		getContainerBlock().onBlockAdded(worldIn, pos, getContainerBlockState());
		if (getContainerBlock() instanceof BlockFalling) {
			worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
		}
	}

	@Override
	public void observedNeighborChange(IBlockState observerState, World world, BlockPos observerPos, Block changedBlock, BlockPos changedBlockPos) {
		getBaseOreBlock().observedNeighborChange(getBaseOreBlockState(), world, observerPos, changedBlock, changedBlockPos);
		if (getBaseOreBlock() instanceof BlockFalling) {
			world.scheduleUpdate(observerPos, this, this.tickRate(world));
		}
		getContainerBlock().observedNeighborChange(getContainerBlockState(), world, observerPos, changedBlock, changedBlockPos);
		if (getContainerBlock() instanceof BlockFalling) {
			world.scheduleUpdate(observerPos, this, this.tickRate(world));
		}
	}

	@Override
	public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		getBaseOreBlock().updateTick(worldIn, pos, getBaseOreBlockState(), rand);
		getContainerBlock().updateTick(worldIn, pos, getContainerBlockState(), rand);
	}

	private WeakReference<World> lastTickRateWorldIn;
	private int tickRateCache;
	@Override
	public int tickRate(World worldIn) { //todo ensure base classes are only ticked at their multiples
		if (lastTickRateWorldIn == null || lastTickRateWorldIn.get() != worldIn) {
			lastTickRateWorldIn = new WeakReference<>(worldIn);
			tickRateCache = gcd(getBaseOreBlock().tickRate(worldIn), getContainerBlock().tickRate(worldIn));
		}
		return tickRateCache;
	}

	static int gcd(int a, int b) {
		while (b != 0) {
			int t = a;
			a = b;
			b = t % b;
		}
		return a;
	}
}
