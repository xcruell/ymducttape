package com.yesmenn.ducttape.client.kit.style;

import com.yesmenn.ducttape.YMDuctTape;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Core shaders of the YM kit. The loaders register them (NeoForge: RegisterShadersEvent, Fabric:
 * CoreShaderRegistrationCallback) and hand the loaded instance back through the setters; styles fall back to plain
 * drawing while a shader is missing (e.g. it failed to compile).
 */
public final class YmShaders {
    /** {@code assets/ymducttape/shaders/core/ym_portal.json}, vertex format POSITION. */
    public static final ResourceLocation PORTAL = ResourceLocation.fromNamespaceAndPath(YMDuctTape.MODID, "ym_portal");

    @Nullable
    private static ShaderInstance portal;

    private YmShaders() {
    }

    @Nullable
    public static ShaderInstance portal() {
        return portal;
    }

    public static void setPortal(ShaderInstance shader) {
        portal = shader;
    }
}
