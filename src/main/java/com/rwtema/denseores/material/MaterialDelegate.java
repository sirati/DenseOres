package com.rwtema.denseores.material;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

public final class MaterialDelegate extends Material {
    private final Material wrapped;
    
    public MaterialDelegate(Material wrapped) {
        super(wrapped.getMaterialMapColor());
        this.wrapped = wrapped;
    }

    @Override
    public boolean getCanBurn() {
        return wrapped.getCanBurn();
    }

    @Override
    public boolean isLiquid() {
        return wrapped.isLiquid();
    }

    @Override
    public boolean isSolid() {
        return wrapped.isSolid();
    }

    @Override
    public boolean blocksLight() {
        return wrapped.blocksLight();
    }

    @Override
    public boolean blocksMovement() {
        return wrapped.blocksMovement();
    }

    @Override
    public Material setReplaceable() {
        return wrapped.setReplaceable();
    }

    @Override
    public boolean isReplaceable() {
        return wrapped.isReplaceable();
    }

    @Override
    public boolean isOpaque() {
        return wrapped.isOpaque();
    }

    @Override
    public boolean isToolNotRequired() {
        return false;
    }

    @Override
    public EnumPushReaction getPushReaction() {
        return wrapped.getPushReaction();
    }

    @Override
    public MapColor getMaterialMapColor() {
        return wrapped.getMaterialMapColor();
    }
}
