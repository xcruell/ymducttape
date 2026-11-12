package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Popup list of actions (right-click menus, dropdown lists). Build it with {@link #entry} / {@link #separator}, then
 * {@link #open} it; picking an entry closes it, as does a click outside or Escape.
 */
public class YmActionMenu extends YmContainer {
    private static final int ENTRY_HEIGHT = 14;
    private static final int SEPARATOR_HEIGHT = 5;

    private final List<YmElement> entries = new ArrayList<>();
    private int minWidth;

    public YmActionMenu() {
        setBackground(Background.WINDOW);
        setPadding(2);
        setLayout(YmLayout.column(0));
    }

    public YmActionMenu entry(Component text, Runnable action) {
        return entry(text, true, false, action);
    }

    /**
     * @param enabled false shows the entry greyed out
     * @param checked shows a mark in front (current choice in a list)
     */
    public YmActionMenu entry(Component text, boolean enabled, boolean checked, Runnable action) {
        Entry entry = new Entry(text, checked, action);
        entry.setEnabled(enabled);
        entries.add(add(entry));
        return this;
    }

    public YmActionMenu separator() {
        entries.add(add(new Separator()));
        return this;
    }

    /** At least this wide (e.g. as wide as the dropdown button it belongs to). */
    public YmActionMenu minWidth(int width) {
        this.minWidth = width;
        return this;
    }

    /** Opens the menu with its top left corner at the given screen position. */
    public void open(YmScreen screen, int x, int y) {
        int w = minWidth;
        int h = 0;
        for (YmElement entry : entries) {
            if (entry instanceof Entry e) {
                w = Math.max(w, Minecraft.getInstance().font.width(e.text) + 24);
            }
            h += entry.getHeight();
        }
        setBounds(x, y, w + padLeft + padRight, h + padTop + padBottom);
        screen.openPopup(this);
    }

    private void close() {
        YmScreen screen = screen();
        if (screen != null) {
            screen.closePopup(this);
        }
    }

    private final class Entry extends YmElement {
        private final Component text;
        private final boolean checked;
        private final Runnable action;

        private Entry(Component text, boolean checked, Runnable action) {
            super(0, ENTRY_HEIGHT);
            this.text = text;
            this.checked = checked;
            this.action = action;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            YmStyle style = style();
            int x = getX();
            int y = getY();
            boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
            if (hovered) {
                style.horizontalGradient(graphics, x, y, x + width, y + height,
                        YmStyle.withAlpha(style.accent, 0x70), YmStyle.withAlpha(style.accent, 0x20));
            }
            if (checked) {
                graphics.fill(x + 5, y + 5, x + 9, y + 9, style.accent);
            }
            int color = !isEnabled() ? style.textDisabled : style.text;
            style.drawText(graphics, font(), text, x + 14, y + (height - 8) / 2, color);
        }

        @Override
        public boolean mousePressed(double mouseX, double mouseY, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return false;
            }
            playClick();
            close();
            action.run();
            return true;
        }
    }

    private static final class Separator extends YmElement {
        private Separator() {
            super(0, SEPARATOR_HEIGHT);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            YmStyle style = style();
            int y = getY() + 2;
            graphics.fill(getX() + 3, y, getX() + width - 3, y + 1, style.bevelLight);
        }
    }
}
