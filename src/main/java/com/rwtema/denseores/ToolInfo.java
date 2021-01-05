package com.rwtema.denseores;

import joptsimple.internal.Strings;

public class ToolInfo {
    public static final ToolInfo NONE = new ToolInfo();
    public final String tool;
    public final int minToolLevel;
    public final int toolLevelOffset;

    public ToolInfo(String tool, int minToolLevel, int toolLevelOffset) {
        this.tool = tool;
        this.minToolLevel = minToolLevel < 0 ? -1 : minToolLevel;
        this.toolLevelOffset = toolLevelOffset;
    }


    private ToolInfo() {
        this(null, -1, 0);
    }

    public int getMinToolLevel() {
        return minToolLevel;
    }

    public int getToolLevelOffset() {
        return toolLevelOffset;
    }

    public String getTool() {
        return tool;
    }

    public boolean hasReplaceTool() {
        return !Strings.isNullOrEmpty(tool);
    }
}
