package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Single-line text field in the kit's look. Text editing (cursor, selection, copy/paste) is done by a borderless
 * vanilla {@link EditBox} placed inside the field. Enter confirms and leaves the field.
 */
public class YmTextField extends YmElement {
    protected final EditBox editBox;
    private Consumer<String> onChange;
    private boolean silent;
    @Nullable
    private Consumer<String> onEnter;

    public YmTextField(String value, Consumer<String> onChange) {
        super(100, 18);
        this.onChange = onChange;
        editBox = new EditBox(font(), 0, 0, 100, 8, Component.empty());
        editBox.setBordered(false);
        editBox.setMaxLength(256);
        editBox.setValue(value);
        editBox.setResponder(text -> {
            if (!silent) {
                this.onChange.accept(text);
            }
        });
    }

    public String getValue() {
        return editBox.getValue();
    }

    /** Sets the text without calling the change callback. */
    public void setValue(String value) {
        silent = true;
        try {
            editBox.setValue(value);
        } finally {
            silent = false;
        }
    }

    public YmTextField setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }

    public YmTextField setMaxLength(int length) {
        editBox.setMaxLength(length);
        return this;
    }

    /** Only text accepted by the filter can be typed or pasted. */
    public YmTextField setFilter(Predicate<String> filter) {
        editBox.setFilter(filter);
        return this;
    }

    public YmTextField setHint(Component hint) {
        editBox.setHint(hint);
        return this;
    }

    public YmTextField setOnEnter(@Nullable Consumer<String> onEnter) {
        this.onEnter = onEnter;
        return this;
    }

    /** Places the edit box inside the field. */
    protected void placeEditBox() {
        editBox.setX(getX() + 4);
        editBox.setY(getY() + (height - 8) / 2);
        editBox.setWidth(Math.max(1, width - 8));
        editBox.setEditable(isEnabled());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        style.drawInset(graphics, x, y, width, height);
        if (isFocused()) {
            style.outline(graphics, x, y, width, height, style.accent);
        } else if (isEnabled() && isMouseOver(mouseX, mouseY)) {
            style.outline(graphics, x, y, width, height, style.accentDim);
        }
        placeEditBox();
        editBox.setTextColor(isEnabled() ? style.text : style.textDisabled);
        editBox.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        editBox.setFocused(focused);
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        requestFocus();
        placeEditBox();
        // The edit box is only one text line tall; move the click into it so it places the cursor.
        double clampedX = Math.max(editBox.getX(), Math.min(editBox.getX() + editBox.getWidth() - 1, mouseX));
        editBox.mouseClicked(clampedX, editBox.getY() + 4, button);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (onEnter != null) {
                onEnter.accept(getValue());
            }
            if (screen() != null) {
                screen().setFocusedElement(null);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        // Everything else stays in the field (so typing "e" does not close the screen, for example).
        editBox.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return editBox.charTyped(codePoint, modifiers);
    }
}
