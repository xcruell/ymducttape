package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Push button with a text and an optional item icon. {@link #primary()} marks the main action of a dialog with the
 * accent color. Fires on left click, or Enter/Space while focused.
 */
public class YmButton extends YmElement {
    public static final int DEFAULT_HEIGHT = 20;

    protected Component text;
    protected ItemStack icon = ItemStack.EMPTY;
    protected final Consumer<YmButton> action;
    private boolean primary;
    private boolean pressed;

    public YmButton(Component text, Consumer<YmButton> action) {
        super(0, DEFAULT_HEIGHT);
        this.text = text;
        this.action = action;
        this.width = font().width(text) + 16;
    }

    public YmButton setText(Component text) {
        this.text = text;
        return this;
    }

    public Component getText() {
        return text;
    }

    public YmButton setIcon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    /** Main action: accent line under the button. */
    public YmButton primary() {
        this.primary = true;
        return this;
    }

    protected void onPress(int mouseButton) {
        playClick();
        action.accept(this);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
        style.drawControl(graphics, x, y, width, height, hovered || isFocused(), pressed && hovered, isEnabled(),
                primary);
        int color = isEnabled() ? style.text : style.textDisabled;
        int textY = y + (height - 8) / 2;
        if (!icon.isEmpty()) {
            int iconX = x + 3;
            int iconY = y + (height - 16) / 2;
            graphics.renderItem(icon, iconX, iconY);
            if (!text.getString().isEmpty()) {
                Component shown = YmStyle.ellipsize(font(), text, width - 26);
                style.drawText(graphics, font(), shown, x + 22, textY, color);
            }
        } else {
            Component shown = YmStyle.ellipsize(font(), text, width - 8);
            style.drawCenteredText(graphics, font(), shown, x + width / 2, textY, color);
        }
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }
        pressed = true;
        captureMouse();
        onPress(button);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        pressed = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isEnabled() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE
                || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            onPress(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            return true;
        }
        return false;
    }
}
