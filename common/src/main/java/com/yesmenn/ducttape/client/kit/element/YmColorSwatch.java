package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Color field. Shows an RGB color (from a supplier, so it follows live changes); clickable if an action is given
 * (e.g. to open a color picker or apply a preset). {@link #selected} draws an accent frame.
 */
public class YmColorSwatch extends YmElement {
    private final IntSupplier color;
    @Nullable
    private final Consumer<YmColorSwatch> action;
    private boolean selected;

    public YmColorSwatch(IntSupplier rgb, @Nullable Consumer<YmColorSwatch> action) {
        super(16, 16);
        this.color = rgb;
        this.action = action;
    }

    public YmColorSwatch setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        style.drawInset(graphics, x, y, width, height);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF000000 | color.getAsInt());
        boolean hovered = action != null && isEnabled() && isMouseOver(mouseX, mouseY);
        if (selected) {
            style.outline(graphics, x, y, width, height, style.accent);
            style.outline(graphics, x + 1, y + 1, width - 2, height - 2, style.outline);
        } else if (hovered) {
            style.outline(graphics, x, y, width, height, style.accentDim);
        }
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (action == null || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        playClick();
        action.accept(this);
        return true;
    }
}
