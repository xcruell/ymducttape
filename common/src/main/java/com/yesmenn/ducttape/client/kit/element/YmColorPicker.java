package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.style.YmColors;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

/**
 * Color picker: saturation/brightness field, hue bar below it, and on the right a preview (new color left, the color
 * it started with right) and a hex field. Hue is kept while the color is grey or black, so dragging back to a
 * colored area continues with the same hue. Below the hex field, R/G/B fields (0-255) show and take exact values.
 * Drawn with gradients only (no textures).
 */
public class YmColorPicker extends YmContainer {
    public static final int DEFAULT_HEIGHT = 81;
    private static final int SIDE_WIDTH = 84;
    private static final int CHANNEL_GAP = 3;
    /** Clicks this far outside the field still count, so the edges (full saturation / brightness) are easy to hit. */
    private static final int EDGE_TOLERANCE = 3;
    private static final int GAP = 6;
    private static final int HUE_HEIGHT = 8;
    private static final int[] HUE_KEYS = {0xFF0000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0xFF00FF, 0xFF0000};

    private enum Drag {
        NONE, FIELD, HUE
    }

    private final IntConsumer onChange;
    private final int original;
    private final YmTextField hexField;
    private final YmNumberField[] channels = new YmNumberField[3];
    private float hue;
    private float saturation;
    private float value;
    private Drag drag = Drag.NONE;

    public YmColorPicker(int rgb, IntConsumer onChange) {
        super(0, DEFAULT_HEIGHT);
        this.onChange = onChange;
        this.original = rgb & 0xFFFFFF;
        // Room for the edge tolerance above and left of the field (clicks there must still reach the picker).
        setPadding(EDGE_TOLERANCE, EDGE_TOLERANCE, 0, 0);
        setHsvFrom(rgb);
        hexField = add(new YmTextField(YmColors.toHex(rgb), this::onHexTyped));
        hexField.setMaxLength(7).setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        for (int i = 0; i < 3; i++) {
            int shift = 16 - i * 8;
            YmNumberField field = add(new YmNumberField(0, 255, rgb >> shift & 0xFF,
                    v -> onChannelTyped(shift, v)));
            field.setTooltip(Component.literal(i == 0 ? "Red" : i == 1 ? "Green" : "Blue"));
            channels[i] = field;
        }
    }

    private void onChannelTyped(int shift, int channelValue) {
        int rgb = getRgb() & ~(0xFF << shift) | channelValue << shift;
        setHsvFrom(rgb);
        hexField.setValue(YmColors.toHex(rgb));
        // The typed field keeps its text; only the others follow (they already match).
        onChange.accept(rgb);
    }

    private void syncChannels(int rgb) {
        for (int i = 0; i < 3; i++) {
            channels[i].setIntValue(rgb >> (16 - i * 8) & 0xFF);
        }
    }

    public int getRgb() {
        return YmColors.hsvToRgb(hue, saturation, value);
    }

    /** Sets the color without calling the change callback. */
    public void setRgb(int rgb) {
        setHsvFrom(rgb);
        hexField.setValue(YmColors.toHex(rgb));
        syncChannels(rgb);
    }

    private void setHsvFrom(int rgb) {
        float[] hsv = YmColors.rgbToHsv(rgb);
        if (hsv[1] > 0.0F && hsv[2] > 0.0F) {
            hue = hsv[0];
        }
        saturation = hsv[1];
        value = hsv[2];
    }

    private void onHexTyped(String text) {
        int rgb = YmColors.parseHex(text);
        if (rgb >= 0 && rgb != getRgb()) {
            setHsvFrom(rgb);
            syncChannels(rgb);
            onChange.accept(rgb);
        }
    }

    private void changed() {
        int rgb = getRgb();
        hexField.setValue(YmColors.toHex(rgb));
        syncChannels(rgb);
        onChange.accept(rgb);
    }

    // ---- geometry (absolute) ----

    private int fieldX() {
        return getX() + padLeft;
    }

    private int fieldY() {
        return getY() + padTop;
    }

    private int fieldWidth() {
        return Math.max(20, contentWidth() - SIDE_WIDTH - GAP);
    }

    private int fieldHeight() {
        return contentHeight() - HUE_HEIGHT - 4;
    }

    private int hueY() {
        return fieldY() + fieldHeight() + 4;
    }

    private int sideX() {
        return fieldX() + fieldWidth() + GAP;
    }

    @Override
    public void layout() {
        int sideX = fieldWidth() + GAP;
        hexField.setBounds(sideX, 32, SIDE_WIDTH, 16);
        int channelWidth = (SIDE_WIDTH - 2 * CHANNEL_GAP) / 3;
        for (int i = 0; i < 3; i++) {
            channels[i].setBounds(sideX + i * (channelWidth + CHANNEL_GAP), 52, channelWidth, 16);
        }
        super.layout();
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int fx = fieldX();
        int fy = fieldY();
        int fw = fieldWidth();
        int fh = fieldHeight();

        // Saturation (left to right) and brightness (top to bottom) for the current hue.
        style.drawInset(graphics, fx - 1, fy - 1, fw + 2, fh + 2);
        int pure = 0xFF000000 | YmColors.hsvToRgb(hue, 1.0F, 1.0F);
        style.horizontalGradient(graphics, fx, fy, fx + fw, fy + fh, 0xFFFFFFFF, pure);
        graphics.fillGradient(fx, fy, fx + fw, fy + fh, 0x00000000, 0xFF000000);
        int mx = fx + Math.round(saturation * (fw - 1));
        int my = fy + Math.round((1.0F - value) * (fh - 1));
        style.outline(graphics, mx - 2, my - 2, 5, 5, 0xFF000000);
        style.outline(graphics, mx - 1, my - 1, 3, 3, 0xFFFFFFFF);

        // Hue bar.
        int hy = hueY();
        style.drawInset(graphics, fx - 1, hy - 1, fw + 2, HUE_HEIGHT + 2);
        for (int i = 0; i < HUE_KEYS.length - 1; i++) {
            int x0 = fx + fw * i / (HUE_KEYS.length - 1);
            int x1 = fx + fw * (i + 1) / (HUE_KEYS.length - 1);
            style.horizontalGradient(graphics, x0, hy, x1, hy + HUE_HEIGHT, 0xFF000000 | HUE_KEYS[i],
                    0xFF000000 | HUE_KEYS[i + 1]);
        }
        int hx = fx + Math.round(hue / 360.0F * (fw - 1));
        graphics.fill(hx - 2, hy - 2, hx + 3, hy + HUE_HEIGHT + 2, 0xFF000000);
        graphics.fill(hx - 1, hy - 1, hx + 2, hy + HUE_HEIGHT + 1, 0xFFFFFFFF);

        // Preview: new | original.
        int sx = sideX();
        style.drawInset(graphics, sx, fy, SIDE_WIDTH, 28);
        int half = (SIDE_WIDTH - 4) / 2;
        graphics.fill(sx + 2, fy + 2, sx + 2 + half, fy + 26, 0xFF000000 | getRgb());
        graphics.fill(sx + 2 + half, fy + 2, sx + SIDE_WIDTH - 2, fy + 26, 0xFF000000 | original);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ---- input ----

    @Override
    protected boolean fadesScreenWhileDragged() {
        return drag != Drag.NONE;
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int fx = fieldX();
            int fw = fieldWidth();
            int t = EDGE_TOLERANCE;
            if (mouseX >= fx - t && mouseX < fx + fw + t) {
                if (mouseY >= fieldY() - t && mouseY < fieldY() + fieldHeight() + 2) {
                    drag = Drag.FIELD;
                } else if (mouseY >= hueY() - 2 && mouseY < hueY() + HUE_HEIGHT + t) {
                    drag = Drag.HUE;
                }
            }
            if (drag != Drag.NONE) {
                captureMouse();
                dragTo(mouseX, mouseY);
                return true;
            }
        }
        return super.mousePressed(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        dragTo(mouseX, mouseY);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        drag = Drag.NONE;
    }

    private void dragTo(double mouseX, double mouseY) {
        float fx = (float) ((mouseX - fieldX()) / Math.max(1, fieldWidth() - 1));
        fx = Math.max(0.0F, Math.min(1.0F, fx));
        switch (drag) {
            case FIELD -> {
                float fy = (float) ((mouseY - fieldY()) / Math.max(1, fieldHeight() - 1));
                saturation = fx;
                value = 1.0F - Math.max(0.0F, Math.min(1.0F, fy));
                changed();
            }
            case HUE -> {
                hue = fx * 360.0F;
                changed();
            }
            case NONE -> {
            }
        }
    }
}
