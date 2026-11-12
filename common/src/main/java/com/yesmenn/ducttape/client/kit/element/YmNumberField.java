package com.yesmenn.ducttape.client.kit.element;

import net.minecraft.client.gui.screens.Screen;

import java.util.function.IntConsumer;

/**
 * Whole-number field with a range. Only digits (and a leading minus if the range allows negatives) can be typed;
 * every valid value is reported at once, and leaving the field puts an out-of-range value back into the range.
 * After a click the scroll wheel steps the value (Shift: ten).
 */
public class YmNumberField extends YmTextField {
    private final int min;
    private final int max;
    private final IntConsumer onValue;
    private int value;

    public YmNumberField(int min, int max, int value, IntConsumer onValue) {
        super(Integer.toString(clamp(value, min, max)), text -> {
        });
        this.min = min;
        this.max = max;
        this.onValue = onValue;
        this.value = clamp(value, min, max);
        this.width = 40;
        setMaxLength(Math.max(Integer.toString(min).length(), Integer.toString(max).length()));
        setFilter(text -> text.isEmpty() || text.matches(min < 0 ? "-?\\d*" : "\\d*"));
        setOnChange(this::parse);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public int getIntValue() {
        return value;
    }

    /** Sets the value without calling the change callback. */
    public void setIntValue(int value) {
        this.value = clamp(value, min, max);
        setValue(Integer.toString(this.value));
    }

    private void parse(String text) {
        try {
            int parsed = Integer.parseInt(text);
            if (parsed >= min && parsed <= max && parsed != value) {
                value = parsed;
                onValue.accept(value);
            }
        } catch (NumberFormatException ignored) {
            // Empty or a lone minus while typing.
        }
    }

    private void change(int newValue) {
        int clamped = clamp(newValue, min, max);
        setIntValue(clamped);
        onValue.accept(clamped);
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        super.onFocusChanged(focused);
        if (!focused) {
            // Puts typed values outside the range (or an empty field) back to the last valid value.
            setIntValue(value);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isFocused()) {
            return false;
        }
        int step = Screen.hasShiftDown() ? 10 : 1;
        change(value + (amount > 0 ? step : -step));
        return true;
    }
}
