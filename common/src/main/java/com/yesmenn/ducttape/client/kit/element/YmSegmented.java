package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Row of joined buttons, one per choice (e.g. Low | Medium | High); the chosen one is lit in the accent color. All
 * choices are visible at once, unlike {@link YmCycler}. The segments share the width equally.
 */
public class YmSegmented<T> extends YmElement {
    private final List<T> values;
    private final Function<T, Component> names;
    private final Consumer<T> onChange;
    private int index;

    public YmSegmented(List<T> values, T value, Function<T, Component> names, Consumer<T> onChange) {
        super(0, 16);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("YmSegmented needs at least one value");
        }
        this.values = List.copyOf(values);
        this.names = names;
        this.onChange = onChange;
        this.index = Math.max(0, this.values.indexOf(value));
        int widest = 0;
        for (T v : this.values) {
            widest = Math.max(widest, font().width(names.apply(v)));
        }
        this.width = (widest + 12) * this.values.size();
    }

    public T getValue() {
        return values.get(index);
    }

    /** Sets the choice without the change callback. */
    public void setValue(T value) {
        int i = values.indexOf(value);
        if (i >= 0) {
            index = i;
        }
    }

    private int segmentX(int i) {
        return getX() + width * i / values.size();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int y = getY();
        style.drawInset(graphics, getX(), y, width, height);
        for (int i = 0; i < values.size(); i++) {
            int x0 = segmentX(i);
            int x1 = segmentX(i + 1);
            boolean selected = i == index;
            boolean hovered = isEnabled() && mouseX >= x0 && mouseX < x1 && isMouseOver(mouseX, mouseY);
            if (selected) {
                style.drawControl(graphics, x0, y, x1 - x0, height, hovered, false, isEnabled(), true);
            } else if (hovered) {
                graphics.fill(x0 + 1, y + 1, x1 - 1, y + height - 1, style.controlHover);
            }
            if (i > 0 && !selected && i - 1 != index) {
                graphics.fill(x0, y + 3, x0 + 1, y + height - 3, style.bevelLight);
            }
            int color = !isEnabled() ? style.textDisabled : selected ? style.text : style.textDim;
            Component name = YmStyle.ellipsize(font(), names.apply(values.get(i)), x1 - x0 - 4);
            style.drawCenteredText(graphics, font(), name, (x0 + x1) / 2, y + (height - 8) / 2, color);
        }
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        for (int i = 0; i < values.size(); i++) {
            if (mouseX >= segmentX(i) && mouseX < segmentX(i + 1)) {
                select(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && index > 0) {
            select(index - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && index < values.size() - 1) {
            select(index + 1);
            return true;
        }
        return false;
    }

    private void select(int i) {
        requestFocus();
        if (i != index) {
            playClick();
            index = i;
            onChange.accept(getValue());
        }
    }
}
