package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Value slider with "{@code label}: value" on top of the track. Drag or click to set; after a click the scroll wheel
 * and the arrow keys step by {@code step} (Shift: ten steps).
 */
public class YmSlider extends YmElement {
    public static final int DEFAULT_HEIGHT = 14;
    private static final int HANDLE = 5;

    private final Component label;
    private final double min;
    private final double max;
    private final double step;
    private double value;
    private DoubleFunction<String> format;
    private final DoubleConsumer onChange;
    private boolean dragging;
    private double[] snapPoints = new double[0];
    private double snapRange;

    public YmSlider(Component label, double min, double max, double step, double value, DoubleConsumer onChange) {
        super(150, DEFAULT_HEIGHT);
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.onChange = onChange;
        this.format = step >= 1.0 ? v -> Long.toString(Math.round(v)) : v -> String.format("%.2f", v);
        this.value = snap(value);
    }

    /** Integer slider. */
    public static YmSlider ofInt(Component label, int min, int max, int value, java.util.function.IntConsumer onChange) {
        return new YmSlider(label, min, max, 1, value, v -> onChange.accept((int) Math.round(v)));
    }

    public YmSlider setFormat(DoubleFunction<String> format) {
        this.format = format;
        return this;
    }

    /**
     * Values the handle snaps to while dragging when it comes within {@code range} of them (e.g. 0, 45 and 90 degrees).
     */
    public YmSlider setSnapPoints(double range, double... points) {
        this.snapRange = range;
        this.snapPoints = points.clone();
        return this;
    }

    public double getValue() {
        return value;
    }

    /** Sets the value without calling the change callback. */
    public void setValue(double value) {
        this.value = snap(value);
    }

    private double snap(double v) {
        double clamped = Math.max(min, Math.min(max, v));
        if (step > 0) {
            clamped = min + Math.round((clamped - min) / step) * step;
        }
        return Math.max(min, Math.min(max, clamped));
    }

    private void change(double v) {
        double snapped = snap(v);
        if (snapped != value) {
            value = snapped;
            onChange.accept(value);
        }
    }

    private double fraction() {
        return max > min ? (value - min) / (max - min) : 0.0;
    }

    private void setFromMouse(double mouseX) {
        double f = (mouseX - getX() - HANDLE / 2.0) / (width - HANDLE);
        double target = min + Math.max(0.0, Math.min(1.0, f)) * (max - min);
        for (double point : snapPoints) {
            if (Math.abs(target - point) <= snapRange) {
                target = point;
                break;
            }
        }
        change(target);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
        style.drawInset(graphics, x, y, width, height);
        int handleX = x + (int) Math.round(fraction() * (width - HANDLE));
        if (isEnabled()) {
            graphics.fill(x + 1, y + 1, handleX + HANDLE / 2, y + height - 1, YmStyle.withAlpha(style.accentDim, 0x90));
        }
        style.drawControl(graphics, handleX, y, HANDLE, height, hovered || dragging || isFocused(), dragging,
                isEnabled(), false);
        Component text = label.getString().isEmpty() ? Component.literal(format.apply(value))
                : Component.empty().append(label).append(": " + format.apply(value));
        int color = isEnabled() ? style.text : style.textDisabled;
        Component shown = YmStyle.ellipsize(font(), text, width - 8);
        graphics.drawString(font(), shown, x + (width - font().width(shown)) / 2, y + (height - 7) / 2, color, true);
    }

    @Override
    protected boolean fadesScreenWhileDragged() {
        return true;
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        dragging = true;
        requestFocus();
        captureMouse();
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            setFromMouse(mouseX);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            playClick();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Only after a click, so that scrolling a long list does not change values by accident.
        if (!isFocused()) {
            return false;
        }
        change(value + Math.signum(amount) * stepSize());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            change(value - stepSize());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            change(value + stepSize());
            return true;
        }
        return false;
    }

    private double stepSize() {
        double s = step > 0 ? step : (max - min) / 100.0;
        return Screen.hasShiftDown() ? s * 10 : s;
    }
}
