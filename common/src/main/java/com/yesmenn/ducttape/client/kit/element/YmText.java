package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

/**
 * Text. Single line (cut with "..." when too long) or {@link #wrap() wrapped}, where the height follows the width.
 * The text may come from a supplier to show live values.
 */
public class YmText extends YmElement {
    public enum Align {
        LEFT, CENTER, RIGHT
    }

    private Supplier<Component> text;
    private Align align = Align.LEFT;
    private boolean dim;
    private boolean wrap;
    private List<FormattedCharSequence> lines = List.of();

    public YmText(Component text) {
        this(() -> text);
    }

    public YmText(Supplier<Component> text) {
        super(0, 10);
        this.text = text;
        this.width = font().width(text.get());
    }

    public YmText setText(Component text) {
        this.text = () -> text;
        return this;
    }

    public YmText align(Align align) {
        this.align = align;
        return this;
    }

    /** Secondary text color. */
    public YmText dim() {
        this.dim = true;
        return this;
    }

    /** Wraps into several lines; the height is set from the width. */
    public YmText wrap() {
        this.wrap = true;
        return this;
    }

    @Override
    public YmText setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (wrap) {
            lines = font().split(text.get(), Math.max(1, width));
            this.height = Math.max(10, lines.size() * 10);
        }
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int color = !isEnabled() ? style.textDisabled : dim ? style.textDim : style.text;
        int x = getX();
        int y = getY();
        if (wrap) {
            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);
                graphics.drawString(font(), line, alignX(x, font().width(line)), y + 1 + i * 10, color, false);
            }
            return;
        }
        Component shown = YmStyle.ellipsize(font(), text.get(), width);
        graphics.drawString(font(), shown, alignX(x, font().width(shown)), y + (height - 8) / 2, color, false);
    }

    private int alignX(int x, int textWidth) {
        return switch (align) {
            case LEFT -> x;
            case CENTER -> x + (width - textWidth) / 2;
            case RIGHT -> x + width - textWidth;
        };
    }
}
