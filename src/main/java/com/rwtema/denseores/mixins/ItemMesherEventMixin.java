package com.rwtema.denseores.mixins;

import com.rwtema.denseores.client.ModelGen;
import net.minecraft.client.renderer.RenderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public class ItemMesherEventMixin {

    @Inject(at = @At("HEAD"), method = "onResourceManagerReload(Lnet/minecraft/client/resources/IResourceManager;)V")
    private void _onResourceManagerReload(CallbackInfo info) {
        ModelGen.registerMesh();
    }
}

