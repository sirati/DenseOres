package com.rwtema.denseores;

import com.rwtema.denseores.utils.Validate;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class OreGroup {
    private final String name;
    private ObjectSet<OreGroup> parents = new ObjectOpenHashSet<>();
    private ObjectSet<DenseOreInfo> ores = new ObjectOpenHashSet<>();
    private ObjectSet<DenseOreInfo> oresRO = ObjectSets.unmodifiable(ores);
    private String overrideBackdrop;
    private boolean init = false;
    public GeneratorInfo generator;
    public boolean isProvided;
    private int retroGenId;

    public OreGroup(String name) {
        this.name = name;
    }


    public void setOverrideBackdrop(String overrideBackdrop) {
        this.overrideBackdrop = overrideBackdrop;
    }

    public String getOverrideBackdrop() {
        return overrideBackdrop;
    }

    public ObjectSet<OreGroup> getParents() {
        return parents;
    }

    public void addParent(OreGroup parent) {
        Validate.isFalse(init, () -> String.format("Cannot add parent %s to %s after initialization", parent.name, name));
        parents.add(parent);
    }

    public void addOre(DenseOreInfo ore) {
        if (ore == null)return;
        ores.add(ore);
    }


    public String getName() {
        return name;
    }

    public void init() {
        if (!init) {
            init = true;
            for (OreGroup parent:parents) {
                Validate.isFalse(isProvided && generator != null,
                        () -> String.format("Generator group %s cannot be provided.", getName()));
                Validate.isTrue(generator != null || (parent.isProvided == isProvided),
                        () -> String.format("You cannot inherit from a provided group if the group is not marked as provided as well.\n" +
                                "\n This group %s{provided=%b} inherits from parent %s{provided=%b}" +
                                "You can however use it as a base for a generator. (generate tag)\n" +
                                "Are you missing or misspelled a 'generate' tag?", getName(), isProvided, parent.getName(), parent.isProvided));


                parent.init();
                if (generator == null) {
                    ores.addAll(parent.getOres());
                } else {
                    for (DenseOreInfo ore: parent.getOres()) {
                        BlockStateInfo parentOre = parent.isProvided? ore.ore : new BlockStateInfo(ore.name);
                        ToolInfo tool = generator.tool == null ? ore.tool : generator.tool;
                        retroGenId = generator.retroGenId < 0 ? ore.retroGenId : generator.retroGenId;
                        if (generator.newBackdrops.size() == 0) {
                            ores.add(DenseOresRegistry.createOreInfo(name, name + " " + ore.configName,
                                    parentOre,
                                    ore.container,
                                    ore.texBaseOre,
                                    ore.texBackdrop,
                                    ore.texNewBackdrop,
                                    ore.overrideTexture,
                                    tool, generator.dense, retroGenId, ore.rendertype, ore.maxColourDist));
                        } else {
                            for (BlockStateInfo backdrop: generator.newBackdrops) {
                                ores.add(DenseOresRegistry.createOreInfo(name, name + " " + ore.configName,
                                        parentOre,
                                        backdrop,
                                        ore.texBaseOre,
                                        ore.texBackdrop,
                                        backdrop,
                                        ore.overrideTexture,
                                        tool, generator.dense, retroGenId, ore.rendertype, ore.maxColourDist));
                            }
                        }



                    }
                }
            }

        }
    }

    public ObjectSet<DenseOreInfo> getOres() {
        return oresRO;
    }
}
