package com.rwtema.denseores;

import com.google.common.base.Optional;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;

import static com.rwtema.denseores.DenseOresMod.wrap;

public class BlockStateReference extends BlockStateInfo {
    private WeakReference<Block> lastBlockRef;
    private IBlockState lastBlockState;
    private boolean finalised = false;

    public BlockStateReference(ResourceLocation resourceLocation, Object2ObjectMap<String, String> propertyLookup) {
        super(resourceLocation, propertyLookup);
    }

    @Override
    public BlockStateReference create() {
        return this;
    }

    public Block getBlock() {
        Block block;
        if (lastBlockRef == null || (block = lastBlockRef.get()) == null || ((!finalised || !DenseOresMod.finishedStartup) && block != Block.REGISTRY.getObject(getResourceLocation()))) {
            block = Block.REGISTRY.getObject(getResourceLocation());
            lastBlockRef = new WeakReference<>(block);
            lastBlockState = null;
            if (DenseOresMod.finishedStartup && validate(block)) {
                finalised = true;
            }
        }
        return block;
    }


    public IBlockState getBlockState() {
        Block block =getBlock();
        if (lastBlockState == null && validate(block)) {
            lastBlockState = block.getDefaultState();

            Object2ObjectMap<String, IProperty<?>> properties = new Object2ObjectOpenHashMap<>();
            for (IProperty<?> p:lastBlockState.getPropertyKeys()) {
                properties.put(p.getName(), p);
            }

            for (Map.Entry<String, String> entry : getPropertyLookup().entrySet()) {
                IProperty<?> p = properties.get(entry.getKey());
                if (p == null) throw wrap(new IllegalStateException(String.format("There is no property names %s for block %s", entry.getKey(), block)));
                lastBlockState = setProperty(lastBlockState, p, entry.getValue());
            }
        }
        return lastBlockState;
    }

    private static <T extends Comparable<T>> IBlockState setProperty(IBlockState state, IProperty<T> property, String valueStr) {
        Optional<T> valueParsed = property.parseValue(valueStr);
        return state.withProperty(property, valueParsed.toJavaUtil().orElseThrow(() ->
                wrap(new IllegalStateException(String.format("Block %s's property %s does not have the value %s! Allowed: %s", state.getBlock(), property.getName(), valueStr, Arrays.toString(property.getAllowedValues().toArray(new Object[0])))))));
    }

    private static boolean validate(Block block) {
        return block != null && block != Blocks.AIR;
    }

    public boolean isValid() {
        return validate(getBlock());
    }

    public ItemStack newStack(int stacksize) {
        ItemStack result = getBlock().getItem(null, null, getBlockState());
        result.setCount(stacksize);
        return result;
    }

}
