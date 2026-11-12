package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** YmButton showing the current choice; a click opens the list of all choices below it. */
public class YmSelect<T> extends YmButton {
    private final List<T> values;
    private final Function<T, Component> names;
    private final Consumer<T> onChange;
    private T value;

    public YmSelect(List<T> values, T value, Function<T, Component> names, Consumer<T> onChange) {
        super(Component.empty(), button -> {
        });
        this.values = List.copyOf(values);
        this.names = names;
        this.onChange = onChange;
        this.value = value;
        setText(names.apply(value));
        int widest = 0;
        for (T v : this.values) {
            widest = Math.max(widest, font().width(names.apply(v)));
        }
        this.width = widest + 24;
    }

    public T getValue() {
        return value;
    }

    /** Sets the choice without calling the change callback. */
    public void setValue(T value) {
        this.value = value;
        setText(names.apply(value));
    }

    @Override
    protected void onPress(int mouseButton) {
        playClick();
        YmScreen screen = screen();
        if (screen == null) {
            return;
        }
        YmActionMenu menu = new YmActionMenu().minWidth(width - 4);
        for (T v : values) {
            menu.entry(names.apply(v), true, v.equals(value), () -> {
                if (!v.equals(value)) {
                    setValue(v);
                    onChange.accept(v);
                }
            });
        }
        menu.open(screen, getX(), getY() + height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
        style.drawControl(graphics, x, y, width, height, hovered, false, isEnabled(), false);
        int color = isEnabled() ? style.text : style.textDisabled;
        Component shown = YmStyle.ellipsize(font(), text, width - 20);
        style.drawText(graphics, font(), shown, x + 6, y + (height - 8) / 2, color);
        YmIconButton.Icon.ARROW_DOWN.draw(graphics, x + width - 11, y + (height - 7) / 2,
                isEnabled() ? style.textDim : style.textDisabled);
    }
}
