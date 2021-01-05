package com.rwtema.denseores;

import com.rwtema.denseores.utils.Validate;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class BlockStateInfo {

    public static final Pattern alphanumericalPattern = Pattern.compile("^[a-z][a-z0-9_]*$");
    public static final Pattern resourcePattern = Pattern.compile("^([a-z][a-z0-9_]*:)?[a-z][a-z0-9_]*$");
    private final ResourceLocation resourceLocation;
    private final Object2ObjectMap<String, String> propertyLookup;
    private final ObjectSortedSet<String> propertyIndex = new ObjectAVLTreeSet<>(Comparator.comparing((String x) -> x));

    public BlockStateInfo(ResourceLocation resourceLocation) {
        this(resourceLocation, Object2ObjectMaps.emptyMap());
    }
    public BlockStateInfo(ResourceLocation resourceLocation, Object2ObjectMap<String, String> propertyLookup) {
        this.resourceLocation = resourceLocation;
        this.propertyLookup = Object2ObjectMaps.unmodifiable(propertyLookup);
        propertyIndex.addAll(propertyLookup.keySet());
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }

    public Object2ObjectMap<String, String> getPropertyLookup() {
        return propertyLookup;
    }

    /**
     * This is super important, as we create a UNIQUE id from the content of the set, however, sets are not ordered!
     * @return
     */
    public Iterable<Map.Entry<String, String>> getFixedOrderPropertyIterator() {
        return () -> new Iterator<Map.Entry<String, String>>() {
            private final Iterator<String> sortOrderIterator = propertyIndex.iterator();
            private String currentKey;
            private String currentValue;
            private final Map.Entry<String, String> entry = new Map.Entry<String, String>() {
                @Override
                public String getKey() {
                    return currentKey;
                }

                @Override
                public String getValue() {
                    return currentValue;
                }

                @Override
                public String setValue(String value) {
                    throw new NotImplementedException("");
                }
            };

            @Override
            public boolean hasNext() {
                return sortOrderIterator.hasNext();
            }

            @Override
            public Map.Entry<String, String> next() {
                currentKey = sortOrderIterator.next();
                currentValue = propertyLookup.get(currentKey);
                return entry;
            }
        };
    }

    public BlockStateReference create() {
        return new BlockStateReference(resourceLocation, propertyLookup);
    }

    public static void writeAsDefault(Configuration config, String category, String key, BlockStateReference blockStateReference) {
        String longKey = category + "." + key;
        String longKeyProperties = longKey + ".properties";
        IBlockState blockState = blockStateReference.getBlockState();
        
        for (IProperty<?> p:blockState.getPropertyKeys()) {
            if (p instanceof PropertyInteger) {
                config.get(longKeyProperties, p.getName(), blockState.getValue((PropertyInteger)p));
            } else if (p instanceof PropertyBool) {
                config.get(longKeyProperties, p.getName(), blockState.getValue((PropertyBool)p));
            } else {
                config.get(longKeyProperties, p.getName(), blockState.getValue(p).toString());
            }
        }
        config.get(longKey, "id", Objects.toString(blockState.getBlock().getRegistryName()));
    }
    
    public static BlockStateInfo read(Configuration config, String category, String key) {
        String longKey = category + "." + key;
        String id = config.getString("id", longKey, Objects.toString(Blocks.STONE.getRegistryName()), "Minecraft resource id of block", resourcePattern);
        ResourceLocation resourceLocation = new ResourceLocation(id);
        Object2ObjectMap<String, String> properties = new Object2ObjectOpenHashMap<>();
        for(Property p: config.getCategory(longKey + ".properties").getOrderedValues()) {
            if (p.isIntValue()) {
                properties.put(p.getName(), Integer.toString(p.getInt()));
            } else if (p.isBooleanValue()) {
                properties.put(p.getName(), Boolean.toString(p.getBoolean()));
            } else {
                properties.put(p.getName(), p.getString());
            }
        }
        return new BlockStateInfo(resourceLocation, Object2ObjectMaps.unmodifiable(properties));
    }


    public static BlockStateInfo read(ItemStack stack) {
        Object2ObjectMap<String, String> properties = new Object2ObjectOpenHashMap<>();
        ItemBlock itemBlock = (ItemBlock) stack.getItem();
        IBlockState blockState = itemBlock.getBlock().getStateFromMeta(itemBlock.getMetadata(stack));
        ResourceLocation resourceLocation = Block.REGISTRY.getNameForObject(itemBlock.getBlock());

        for (IProperty<?> p:blockState.getPropertyKeys()) {
            if (p instanceof PropertyInteger) {
                properties.put(p.getName(), Integer.toString(blockState.getValue((PropertyInteger)p)));
            } else if (p instanceof PropertyBool) {
                properties.put(p.getName(), Boolean.toString(blockState.getValue((PropertyBool)p)));
            } else {
                properties.put(p.getName(), blockState.getValue(p).toString());
            }
        }

        return new BlockStateInfo(resourceLocation, Object2ObjectMaps.unmodifiable(properties));
    }
    public static BlockStateInfo read(NBTTagCompound nbt) {
        Object2ObjectMap<String, String> properties = new Object2ObjectOpenHashMap<>();
        ResourceLocation resourceLocation = new ResourceLocation(nbt.getString("id"));
        NBTTagCompound propsNbt = nbt.getCompoundTag("properties");
        for(String propKey:propsNbt.getKeySet()) {
            NBTBase propNbt = propsNbt.getTag(propKey);
            if (propNbt instanceof NBTPrimitive) {
                properties.put(propKey, Integer.toString(((NBTPrimitive) propNbt).getInt()));
            } else {
                properties.put(propKey, ((NBTTagString)propNbt).getString());
            }
        }

        return new BlockStateInfo(resourceLocation, Object2ObjectMaps.unmodifiable(properties));
    }

    public static BlockStateInfo createMinecraft(String block) {
        Validate.isTrue(alphanumericalPattern.matcher(block).find(), () -> String.format("%s is not alphanumerical", block));
        return new BlockStateInfo(new ResourceLocation(block), Object2ObjectMaps.emptyMap());
    }
    
}
