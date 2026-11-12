package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * YmButton that steps through a list of values: left click = next, right click = previous; after a click also the
 * scroll wheel and the arrow keys. Shows "{@code label}: value", with small arrows on both sides.
 */
public class YmCycler<T> extends YmButton {
    private final Component label;
    private final List<T> values;
    private final Function<T, Component> names;
    private final Consumer<T> onChange;
    private int index;

    public YmCycler(Component label, List<T> values, T value, Function<T, Component> names, Consumer<T> onChange) {
        super(Component.empty(), button -> {
        });
        if (values.isEmpty()) {
            throw new IllegalArgumentException("YmCycler needs at least one value");
        }
        this.label = label;
        this.values = List.copyOf(values);
        this.names = names;
        this.onChange = onChange;
        this.index = Math.max(0, this.values.indexOf(value));
        updateText();
        int widest = 0;
        for (T v : this.values) {
            widest = Math.max(widest, font().width(composeText(v)));
        }
        this.width = widest + 28;
    }

    public T getValue() {
        return values.get(index);
    }

    public void setValue(T value) {
        int i = values.indexOf(value);
        if (i >= 0) {
            index = i;
            updateText();
        }
    }

    private Component composeText(T value) {
        Component name = names.apply(value);
        return label.getString().isEmpty() ? name : Component.empty().append(label).append(": ").append(name);
    }

    private void updateText() {
        setText(composeText(getValue()));
    }

    private void step(int delta) {
        index = Math.floorMod(index + delta, values.size());
        updateText();
        onChange.accept(getValue());
    }

    @Override
    protected void onPress(int mouseButton) {
        playClick();
        requestFocus();
        step(mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? -1 : 1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Only after a click, so that scrolling a long list does not change values by accident.
        if (!isFocused()) {
            return false;
        }
        step(amount > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            step(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            step(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        YmStyle style = style();
        int color = isEnabled() ? style.textDim : style.textDisabled;
        int iconY = getY() + (height - 7) / 2;
        YmIconButton.Icon.ARROW_LEFT.draw(graphics, getX() + 2, iconY, color);
        YmIconButton.Icon.ARROW_RIGHT.draw(graphics, getX() + width - 9, iconY, color);
    }
}
