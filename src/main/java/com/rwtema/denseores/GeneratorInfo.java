package com.rwtema.denseores;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class GeneratorInfo {
    public final ObjectSet<BlockStateInfo> newBackdrops;
    public final ToolInfo tool;
    public final boolean dense;
    public final int retroGenId;

    public GeneratorInfo(ObjectSet<BlockStateInfo> newBackdrops, ToolInfo tool, boolean dense, int retroGenId) {
        this.newBackdrops = newBackdrops;
        this.tool = tool;
        this.dense = dense;
        this.retroGenId = retroGenId;
    }
    
    public static GeneratorInfo read(Configuration config, String category, String key) {
        String longKey = category + "." + key;
        String longKeyNewBackdrop = longKey +"." + "new backdrop";
        ObjectSet<BlockStateInfo> newBackdrop;
        if (config.hasCategory(longKeyNewBackdrop)) {
            if (config.hasKey(longKeyNewBackdrop, "id")) {
                newBackdrop = ObjectSets.singleton(BlockStateInfo.read(config, longKey, "new backdrop"));
            } else {
                newBackdrop = new ObjectOpenHashSet<>();
                //why the fuck is forges config format so retarded
                for (String cat:config.getCategoryNames()) {
                    if (cat.length()>longKeyNewBackdrop.length() &&  cat.startsWith(longKeyNewBackdrop)) {
                        cat = cat.substring(longKeyNewBackdrop.length()+1);
                         if (cat.split("\\.").length == 1) {
                             newBackdrop.add(BlockStateInfo.read(config, longKeyNewBackdrop, cat));
                         }
                    }
                 }
            }
        } else {
            newBackdrop = ObjectSets.emptySet();
        }
        ToolInfo tool = null;
        if (config.hasKey(longKey, "tool") || config.hasKey(longKey, "minToolLevel") || config.hasKey(longKey, "toolLevelOffset")) {
            String toolStr = config.hasKey(longKey, "tool") ? config.get(longKey, "tool", "").getString():null;
            int minToolLevel = config.hasKey(longKey, "minToolLevel") ? config.get(longKey, "minToolLevel", -1).getInt():-1;
            int toolLevelOffset = config.hasKey(longKey, "toolLevelOffset") ? config.get(longKey, "toolLevelOffset", 0).getInt():0;
            tool = new ToolInfo(toolStr, minToolLevel, toolLevelOffset);
        }
        boolean dense = config.hasKey(longKey, "dense") && config.get(longKey, "dense", false).getBoolean();
        int retroGenId = config.hasKey(longKey, "toolLevelOffset") ? config.get(longKey, "toolLevelOffset", 0).getInt():0;
        return new GeneratorInfo(newBackdrop, tool, dense, retroGenId);
    }
}
