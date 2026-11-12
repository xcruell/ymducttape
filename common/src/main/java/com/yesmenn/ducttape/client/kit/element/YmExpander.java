package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Collapsible block: a header row (optional icon, title, optional summary on the right, arrow) and a body that opens
 * below it when the header is clicked. The body is a column; add its elements to {@link #body()}. The height follows
 * the state, and the surrounding layout is redone on every toggle.
 */
public class YmExpander extends YmContainer {
    public static final int HEADER_HEIGHT = 18;
    private static final int BODY_GAP = 4;

    private final Component title;
    private final YmContainer body = new YmContainer();
    private final Consumer<Boolean> onToggle;
    private ItemStack icon = ItemStack.EMPTY;
    @Nullable
    private Supplier<Component> summary;
    private boolean expanded;

    /**
     * @param expanded initial state (keep it in the screen to survive a rebuild)
     * @param onToggle called with the new state
     */
    public YmExpander(Component title, boolean expanded, Consumer<Boolean> onToggle) {
        super(0, HEADER_HEIGHT);
        this.title = title;
        this.expanded = expanded;
        this.onToggle = onToggle;
        add(body);
        body.setPadding(6, 4, 6, 6).setLayout(YmLayout.column(BODY_GAP));
        body.setVisible(expanded);
    }

    public YmContainer body() {
        return body;
    }

    public YmExpander setIcon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    /** Short live text shown right in the header (e.g. the current value while collapsed). */
    public YmExpander setSummary(@Nullable Supplier<Component> summary) {
        this.summary = summary;
        return this;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) {
            return;
        }
        this.expanded = expanded;
        body.setVisible(expanded);
        onToggle.accept(expanded);
        YmScreen screen = screen();
        if (screen != null) {
            screen.relayout();
        }
    }

    @Override
    public YmExpander setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        // The body's height depends on the width (wrapped text), so it is measured here, when the width is known.
        if (expanded) {
            body.setBounds(0, HEADER_HEIGHT, width, 0);
            body.layout();
            int bodyHeight = body.childrenHeight() + 10;
            body.setSize(width, bodyHeight);
            this.height = HEADER_HEIGHT + bodyHeight;
        } else {
            this.height = HEADER_HEIGHT;
        }
        return this;
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        boolean hovered = isEnabled() && mouseX >= x && mouseX < x + width && mouseY >= y
                && mouseY < y + HEADER_HEIGHT && isInsideClip(mouseX, mouseY);
        if (expanded) {
            style.drawInset(graphics, x, y + HEADER_HEIGHT - 1, width, height - HEADER_HEIGHT + 1);
        }
        style.drawControl(graphics, x, y, width, HEADER_HEIGHT, hovered, false, isEnabled(), expanded);
        int textX = x + 6;
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, x + 3, y + 1);
            textX = x + 22;
        }
        int textY = y + (HEADER_HEIGHT - 8) / 2;
        int right = x + width - 16;
        if (summary != null) {
            Component text = summary.get();
            int w = font().width(text);
            style.drawText(graphics, font(), text, right - w, textY, style.textDim);
            right -= w + 8;
        }
        int color = isEnabled() ? style.text : style.textDisabled;
        style.drawText(graphics, font(), YmStyle.ellipsize(font(), title, right - textX), textX, textY, color);
        (expanded ? YmIconButton.Icon.ARROW_UP : YmIconButton.Icon.ARROW_DOWN)
                .draw(graphics, x + width - 12, y + (HEADER_HEIGHT - 7) / 2, hovered ? style.accent : style.textDim);
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseY < getY() + HEADER_HEIGHT) {
            playClick();
            setExpanded(!expanded);
            return true;
        }
        return super.mousePressed(mouseX, mouseY, button);
    }
}
