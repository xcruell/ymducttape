package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Window with three parts: a header bar (title, optional close button), the body with the {@link #content()} panel and
 * an optional footer bar ({@link #footer()}, buttons right-aligned). Each part is drawn by the style, so a style can
 * e.g. animate the body while header and footer stay plain bars.
 */
public class YmWindow extends YmContainer {
    public static final int HEADER_HEIGHT = 18;
    public static final int FOOTER_HEIGHT = 26;

    private final Component title;
    private final YmContainer content = new YmContainer();
    @Nullable
    private YmContainer footer;
    @Nullable
    private YmIconButton closeButton;

    public YmWindow(Component title, int width, int height) {
        super(width, height);
        this.title = title;
        add(content);
        content.setPadding(6);
    }

    /** YmContainer for the window's widgets (the body). */
    public YmContainer content() {
        return content;
    }

    /** Footer bar for dialog buttons, created on first use; buttons keep their width and line up on the right. */
    public YmContainer footer() {
        if (footer == null) {
            footer = add(new YmContainer());
            footer.setPadding(5, 4, 5, 3).setLayout(YmLayout.rowRight(4));
        }
        return footer;
    }

    /** Adds the close button (x) to the header bar; it closes the screen. */
    public YmWindow closable() {
        return closable(() -> {
            YmScreen screen = screen();
            if (screen != null) {
                screen.onClose();
            }
        });
    }

    /** Adds the close button (x) to the header bar with its own action (e.g. closing a dialog). */
    public YmWindow closable(Runnable onClose) {
        if (closeButton == null) {
            closeButton = add(new YmIconButton(YmIconButton.Icon.CLOSE, button -> onClose.run()));
            closeButton.setTooltip(Component.translatable("gui.cancel"));
        }
        return this;
    }

    /** Centers the frame in its parent's content area (call after adding it). */
    public YmWindow centerIn(YmContainer panel) {
        setPos((panel.contentWidth() - width) / 2, (panel.contentHeight() - height) / 2);
        return this;
    }

    private int footerHeight() {
        return footer == null ? 0 : FOOTER_HEIGHT;
    }

    @Override
    public void layout() {
        content.setBounds(1, HEADER_HEIGHT, width - 2, height - HEADER_HEIGHT - footerHeight() - 1);
        if (footer != null) {
            footer.setBounds(1, height - FOOTER_HEIGHT - 1, width - 2, FOOTER_HEIGHT);
        }
        if (closeButton != null) {
            closeButton.setBounds(width - 16, 3, 12, 12);
        }
        super.layout();
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        int bodyTop = y + HEADER_HEIGHT;
        int bodyHeight = height - HEADER_HEIGHT - footerHeight() - 1;
        style.drawWindowBody(graphics, x + 1, bodyTop, width - 2, bodyHeight, mouseX, mouseY, partialTick);
        int titleWidth = width - 12 - (closeButton != null ? 16 : 0);
        style.drawHeaderBar(graphics, font(), title, x + 1, y + 1, width - 2, HEADER_HEIGHT - 1, titleWidth);
        if (footer != null) {
            style.drawFooterBar(graphics, x + 1, y + height - FOOTER_HEIGHT - 1, width - 2, FOOTER_HEIGHT);
        }
        style.drawWindowFrame(graphics, x, y, width, height);
    }
}
