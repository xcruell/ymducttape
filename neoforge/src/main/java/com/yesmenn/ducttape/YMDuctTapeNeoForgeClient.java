package com.yesmenn.ducttape;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.yesmenn.ducttape.client.KitDemoScreen;
import com.yesmenn.ducttape.client.kit.style.YmShaders;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;

/** Client-only setup: YM kit shaders, and the mod list's config button opens the YM kit demo. */
@Mod(value = YMDuctTape.MODID, dist = Dist.CLIENT)
public final class YMDuctTapeNeoForgeClient {

    public YMDuctTapeNeoForgeClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new KitDemoScreen(parent));
        modEventBus.addListener(YMDuctTapeNeoForgeClient::registerShaders);
    }

    private static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), YmShaders.PORTAL,
                    DefaultVertexFormat.POSITION), YmShaders::setPortal);
        } catch (IOException e) {
            YMDuctTape.LOGGER.error("Could not load the YM kit portal shader", e);
        }
    }
}
