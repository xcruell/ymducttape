package com.yesmenn.ducttape.client.kit.style;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * Colors and drawing primitives of the YM GUI kit. This base class is the "YesMenn metal" style (dark steel plates
 * with a bevel, brushed-metal titles, one accent color for active and focused parts).
 * <p>
 * Other styles extend this class and override the drawing methods, e.g. {@link #drawWindowBody} for an animated body
 * background while header and footer stay normal bars. Widgets draw only through the style, so a style changes every
 * screen at once. Themes are immutable; {@link #withAccent(int)} gives a variant with another accent color (subclasses
 * override it to keep their style).
 */
public class YmStyle {
    /** Section accent colors (same as the creative tab banners). */
    public static final int ACCENT_LIGHTS = 0xFFE7A93C;
    public static final int ACCENT_TECHNICALS = 0xFF5C8FD6;
    public static final int ACCENT_DECOR = 0xFF6FB04E;
    public static final int ACCENT_OTHER = 0xFF9A9A9A;

    public static final YmStyle DEFAULT = new YmStyle(ACCENT_TECHNICALS);

    // Plates
    public final int windowFill = 0xF0181C22;
    public final int titleFill = 0xFF101418;
    public final int panelFill = 0xFF1E232A;
    public final int insetFill = 0xFF0E1115;
    public final int outline = 0xFF07090B;
    public final int bevelLight = 0xFF3B434E;
    public final int bevelDark = 0xFF12161B;
    // Controls
    public final int controlFill = 0xFF2A313A;
    public final int controlHover = 0xFF353E49;
    public final int controlPressed = 0xFF20262D;
    public final int controlDisabled = 0xFF1C2026;
    public final int scrollThumb = 0xFF4A5461;
    // Text
    public final int text = 0xFFE6E9EE;
    public final int textDim = 0xFF9AA3AE;
    public final int textDisabled = 0xFF5D646E;
    public final int textShadow = 0xFF2B3038;
    /** Brushed-metal title colors per band from the top (glyphs are 8 px tall). */
    private static final int[] METAL_BANDS = {0xFFFFFFFF, 0xFFE8ECF1, 0xFFC9D0D9, 0xFF9EA8B6};
    private static final int[] METAL_BAND_HEIGHTS = {2, 2, 2};

    public final int accent;
    public final int accentDim;

    public YmStyle(int accent) {
        this.accent = accent;
        this.accentDim = scale(accent, 0.55F);
    }

    public YmStyle withAccent(int accent) {
        return new YmStyle(accent);
    }

    // ---- window parts (override these for a different window style) ----

    /**
     * Body of a window (behind the content, between header and footer bar). Gets the mouse position so a style can
     * react to it (e.g. a parallax background).
     */
    public void drawWindowBody(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        g.fill(x, y, x + w, y + h, windowFill);
    }

    /** Outer frame of a window, drawn around header, body and footer. */
    public void drawWindowFrame(GuiGraphics g, int x, int y, int w, int h) {
        outline(g, x, y, w, h, outline);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, bevelLight);
        g.fill(x + 1, y + 2, x + 2, y + h - 1, bevelLight);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, bevelDark);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, bevelDark);
    }

    /** Header bar with the window title (and room for the close button on the right). */
    public void drawHeaderBar(GuiGraphics g, Font font, Component title, int x, int y, int w, int h, int titleWidth) {
        g.fill(x, y, x + w, y + h, titleFill);
        horizontalGradient(g, x, y, x + w, y + 1, 0x50FFFFFF, 0x00FFFFFF);
        horizontalGradient(g, x, y + h - 1, x + w, y + h, accent, withAlpha(accent, 0x20));
        drawMetalText(g, font, ellipsize(font, title, titleWidth), x + 5, y + (h - 8) / 2);
    }

    /** Footer bar (holds the dialog buttons). */
    public void drawFooterBar(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, titleFill);
        g.fill(x, y, x + w, y + 1, bevelDark);
        horizontalGradient(g, x, y + 1, x + w, y + 2, 0x30FFFFFF, 0x00FFFFFF);
    }

    // ---- plates ----

    /** Raised steel plate: outline, light top/left and dark bottom/right edge. */
    public void drawPlate(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, outline);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, bevelLight);
        g.fill(x + 1, y + 2, x + 2, y + h - 1, bevelLight);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, bevelDark);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, bevelDark);
    }

    /** Sunken area (text fields, slider tracks, lists). */
    public void drawInset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, bevelLight);
        g.fill(x, y, x + w - 1, y + h - 1, outline);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, insetFill);
    }

    /** YmButton / control body for the given state. */
    public void drawControl(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean pressed,
                            boolean enabled, boolean accented) {
        int fill = !enabled ? controlDisabled : pressed ? controlPressed : hovered ? controlHover : controlFill;
        drawPlate(g, x, y, w, h, fill);
        if (enabled && accented) {
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, accent);
        }
        if (enabled && hovered && !pressed) {
            outline(g, x, y, w, h, accentDim);
        }
    }

    /** 1 px frame. */
    public void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Fill with a gradient from left to right. */
    public void horizontalGradient(GuiGraphics g, int x0, int y0, int x1, int y1, int left, int right) {
        Matrix4f pose = g.pose().last().pose();
        VertexConsumer consumer = g.bufferSource().getBuffer(RenderType.gui());
        consumer.addVertex(pose, x0, y0, 0.0F).setColor(left);
        consumer.addVertex(pose, x0, y1, 0.0F).setColor(left);
        consumer.addVertex(pose, x1, y1, 0.0F).setColor(right);
        consumer.addVertex(pose, x1, y0, 0.0F).setColor(right);
        g.flush();
    }

    // ---- text ----

    public void drawText(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public void drawCenteredText(GuiGraphics g, Font font, Component text, int centerX, int y, int color) {
        g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    /**
     * Brushed-metal title: shadow first, then the text in horizontal bands from bright at the top to darker steel at
     * the bottom (each band clipped with a scissor), like an engraved metal label.
     */
    public void drawMetalText(GuiGraphics g, Font font, Component text, int x, int y) {
        int width = font.width(text) + 1;
        g.drawString(font, text, x + 1, y + 1, textShadow, false);
        g.flush();
        int top = y;
        for (int i = 0; i < METAL_BANDS.length; i++) {
            int bottom = i == METAL_BANDS.length - 1 ? y + font.lineHeight : top + METAL_BAND_HEIGHTS[i];
            g.enableScissor(x, top, x + width, bottom);
            g.drawString(font, text, x, y, METAL_BANDS[i], false);
            g.flush();
            g.disableScissor();
            top = bottom;
        }
    }

    /** Text cut to {@code maxWidth} with "..." if needed. */
    public static Component ellipsize(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("...")));
        return Component.literal(cut + "...").withStyle(text.getStyle());
    }

    // ---- colors ----

    public static int scale(int argb, float factor) {
        int r = Math.min(255, (int) ((argb >> 16 & 0xFF) * factor));
        int gr = Math.min(255, (int) ((argb >> 8 & 0xFF) * factor));
        int b = Math.min(255, (int) ((argb & 0xFF) * factor));
        return argb & 0xFF000000 | r << 16 | gr << 8 | b;
    }

    public static int withAlpha(int argb, int alpha) {
        return (alpha & 0xFF) << 24 | argb & 0x00FFFFFF;
    }
}
