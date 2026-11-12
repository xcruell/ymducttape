package com.yesmenn.ducttape.client.kit.style;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Special style: the window body is a moving starfield (own shader over the End sky and portal star textures) whose
 * layers follow the cursor with parallax. Header and footer stay the normal metal bars; all controls look as in the
 * base style.
 */
public class YmPortalStyle extends YmStyle {
//    private static final ResourceLocation SKY = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    private static final ResourceLocation SKY = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    private static final ResourceLocation STARS = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    /** How fast the parallax follows the cursor (fraction of the remaining distance per millisecond). */
    private static final float FOLLOW_PER_MS = 0.0009F;

    // Smoothed parallax, shared by all portal windows (only one window is visible at a time).
    private static float parallaxX;
    private static float parallaxY;
    private static long lastFrame;

    /** Default layer colors (0xRRGGBB): every star layer mixes these two in its own ratio. */
    public static final int DEFAULT_TINT_A = 0x146B66;
    public static final int DEFAULT_TINT_B = 0x6B38B8;

    private final int tintA;
    private final int tintB;

    public YmPortalStyle(int accent) {
        this(accent, DEFAULT_TINT_A, DEFAULT_TINT_B);
    }

    /** With own layer colors (0xRRGGBB). */
    public YmPortalStyle(int accent, int tintA, int tintB) {
        super(accent);
        this.tintA = tintA;
        this.tintB = tintB;
    }

    @Override
    public YmStyle withAccent(int accent) {
        return new YmPortalStyle(accent, tintA, tintB);
    }

    public YmPortalStyle withTints(int tintA, int tintB) {
        return new YmPortalStyle(accent, tintA, tintB);
    }

    public int tintA() {
        return tintA;
    }

    public int tintB() {
        return tintB;
    }

    @Override
    public void drawWindowBody(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        ShaderInstance shader = YmShaders.portal();
        if (shader == null || w <= 0 || h <= 0) {
            super.drawWindowBody(g, x, y, w, h, mouseX, mouseY, partialTick);
            return;
        }
        updateParallax(x, y, w, h, mouseX, mouseY);
        g.flush();

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, SKY);
        RenderSystem.setShaderTexture(1, STARS);
        shader.safeGetUniform("YmTime").set((Util.getMillis() % 3_600_000L) / 1000.0F);
        shader.safeGetUniform("YmParallax").set(parallaxX, parallaxY);
        shader.safeGetUniform("YmRect").set((float) x, (float) y, (float) w, (float) h);
        shader.safeGetUniform("YmTintA").set(red(tintA), green(tintA), blue(tintA));
        shader.safeGetUniform("YmTintB").set(red(tintB), green(tintB), blue(tintB));

        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(pose, x, y, 0.0F);
        buffer.addVertex(pose, x, y + h, 0.0F);
        buffer.addVertex(pose, x + w, y + h, 0.0F);
        buffer.addVertex(pose, x + w, y, 0.0F);
        // Blended, so the body fades with the rest of the screen (ColorModulator alpha below 1).
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static float red(int rgb) {
        return (rgb >> 16 & 0xFF) / 255.0F;
    }

    private static float green(int rgb) {
        return (rgb >> 8 & 0xFF) / 255.0F;
    }

    private static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    private static void updateParallax(int x, int y, int w, int h, int mouseX, int mouseY) {
        long now = Util.getMillis();
        long elapsed = lastFrame == 0 ? 0 : Math.min(200, now - lastFrame);
        lastFrame = now;
        // The screen passes a far-away mouse position while a popup is open; keep the last view then.
        if (mouseX < -100 || mouseY < -100) {
            return;
        }
        float targetX = Math.max(-1.0F, Math.min(1.0F, (mouseX - (x + w / 2.0F)) / (w / 2.0F)));
        float targetY = Math.max(-1.0F, Math.min(1.0F, (mouseY - (y + h / 2.0F)) / (h / 2.0F)));
        float follow = elapsed == 0 ? 1.0F : 1.0F - (float) Math.pow(1.0F - FOLLOW_PER_MS, elapsed);
        parallaxX += (targetX - parallaxX) * follow;
        parallaxY += (targetY - parallaxY) * follow;
    }
}
