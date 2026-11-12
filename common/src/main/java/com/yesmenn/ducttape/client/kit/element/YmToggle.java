package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/** On/off box with a label on its right. The whole row is clickable. */
public class YmToggle extends YmElement {
    private static final int BOX = 11;

    private final Component text;
    private boolean checked;
    private final Consumer<Boolean> onChange;

    public YmToggle(Component text, boolean checked, Consumer<Boolean> onChange) {
        super(0, 14);
        this.text = text;
        this.checked = checked;
        this.onChange = onChange;
        this.width = BOX + 5 + font().width(text);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    private void toggle() {
        playClick();
        checked = !checked;
        onChange.accept(checked);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int boxY = getY() + (height - BOX) / 2;
        boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
        style.drawInset(graphics, x, boxY, BOX, BOX);
        if (hovered) {
            style.outline(graphics, x, boxY, BOX, BOX, style.accentDim);
        }
        if (checked) {
            graphics.fill(x + 3, boxY + 3, x + BOX - 3, boxY + BOX - 3, isEnabled() ? style.accent : style.textDisabled);
        }
        int color = !isEnabled() ? style.textDisabled : style.text;
        Component shown = YmStyle.ellipsize(font(), text, width - BOX - 5);
        style.drawText(graphics, font(), shown, x + BOX + 5, getY() + (height - 8) / 2, color);
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        toggle();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            toggle();
            return true;
        }
        return false;
    }
}
