package com.yesmenn.ducttape.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.yesmenn.ducttape.client.kit.style.YmShaders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;

/** Client-only setup: YM kit shaders. */
public final class YMDuctTapeFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(YmShaders.PORTAL, DefaultVertexFormat.POSITION, YmShaders::setPortal));
    }
}
