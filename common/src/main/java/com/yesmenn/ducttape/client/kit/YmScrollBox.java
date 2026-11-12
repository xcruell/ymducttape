package com.yesmenn.ducttape.client.kit;

import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;

/**
 * YmContainer with vertical scrolling. Children may be taller than the panel; a scroll bar appears on the right when needed.
 * The content area leaves room for the bar, so a column layout fits the children next to it.
 */
public class YmScrollBox extends YmContainer {
    public static final int BAR_WIDTH = 6;
    private static final int SCROLL_STEP = 14;

    private boolean draggingBar;
    private double dragOffset;

    public YmScrollBox() {
        setClip(true);
        setPadding(2, 2, 2 + BAR_WIDTH + 2, 2);
    }

    public int maxScroll() {
        return Math.max(0, childrenHeight() - contentHeight());
    }

    public int getScroll() {
        return scrollY;
    }

    public void setScroll(int scroll) {
        scrollY = Math.max(0, Math.min(maxScroll(), scroll));
    }

    /** Scrolls the least amount so that the widget (a direct or deeper child) is fully visible. */
    public void scrollTo(YmElement widget) {
        int top = widget.getY() - contentY();
        int bottom = top + widget.getHeight();
        if (top < scrollY) {
            setScroll(top);
        } else if (bottom > scrollY + contentHeight()) {
            setScroll(bottom - contentHeight());
        }
    }

    @Override
    public void layout() {
        super.layout();
        setScroll(scrollY);
    }

    /** The bar sits at the right edge; with a small right padding it lies over the content (e.g. a sidebar). */
    private int barX() {
        return getX() + width - BAR_WIDTH - 2;
    }

    private int barY() {
        return getY() + padTop;
    }

    private int thumbHeight() {
        int track = contentHeight();
        int total = childrenHeight();
        return total <= 0 ? track : Math.max(12, track * track / total);
    }

    private int thumbY() {
        int max = maxScroll();
        return barY() + (max == 0 ? 0 : (contentHeight() - thumbHeight()) * scrollY / max);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (maxScroll() <= 0) {
            return;
        }
        YmStyle style = style();
        int bx = barX();
        style.drawInset(graphics, bx, barY(), BAR_WIDTH, contentHeight());
        boolean hovered = mouseX >= bx && mouseX < bx + BAR_WIDTH && mouseY >= thumbY()
                && mouseY < thumbY() + thumbHeight();
        int color = draggingBar ? style.accent : hovered ? style.accentDim : style.scrollThumb;
        graphics.fill(bx + 1, thumbY() + 1, bx + BAR_WIDTH - 1, thumbY() + thumbHeight() - 1, color);
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        int bx = barX();
        if (maxScroll() > 0 && button == 0 && mouseX >= bx && mouseX < bx + BAR_WIDTH
                && mouseY >= barY() && mouseY < barY() + contentHeight()) {
            int thumbTop = thumbY();
            if (mouseY < thumbTop || mouseY >= thumbTop + thumbHeight()) {
                // Click on the track: jump so that the thumb centers on the mouse.
                dragOffset = thumbHeight() / 2.0;
                scrollToThumb(mouseY);
            } else {
                dragOffset = mouseY - thumbTop;
            }
            draggingBar = true;
            captureMouse();
            return true;
        }
        return super.mousePressed(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar) {
            scrollToThumb(mouseY);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
    }

    private void scrollToThumb(double mouseY) {
        int free = contentHeight() - thumbHeight();
        if (free > 0) {
            setScroll((int) Math.round((mouseY - dragOffset - barY()) * maxScroll() / free));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (super.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        if (maxScroll() <= 0) {
            return false;
        }
        int before = scrollY;
        setScroll(scrollY - (int) Math.round(amount * SCROLL_STEP));
        return before != scrollY;
    }
}
