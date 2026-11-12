package com.yesmenn.ducttape.client.kit;

import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container of widgets. Children are positioned relative to the content area (inside the padding, shifted by the
 * scroll offset of {@link YmScrollBox}). An optional {@link YmLayout} arranges the children in {@link #layout()}.
 */
public class YmContainer extends YmElement {
    public enum Background {
        NONE, PLATE, INSET, WINDOW
    }

    private final List<YmElement> children = new ArrayList<>();
    private final List<YmElement> childrenView = Collections.unmodifiableList(children);
    @Nullable
    private YmLayout layout;
    private Background background = Background.NONE;
    private boolean clip;
    protected int padLeft;
    protected int padTop;
    protected int padRight;
    protected int padBottom;
    protected int scrollX;
    protected int scrollY;
    /** Set on the root panel of a screen only. */
    @Nullable
    YmScreen owner;

    public YmContainer() {
        super(0, 0);
    }

    public YmContainer(int width, int height) {
        super(width, height);
    }

    // ---- children ----

    public <W extends YmElement> W add(W widget) {
        if (widget.parent != null) {
            widget.parent.remove(widget);
        }
        widget.parent = this;
        children.add(widget);
        return widget;
    }

    public void remove(YmElement widget) {
        if (children.remove(widget)) {
            widget.parent = null;
        }
    }

    public void clear() {
        for (YmElement child : children) {
            child.parent = null;
        }
        children.clear();
        scrollX = 0;
        scrollY = 0;
    }

    public List<YmElement> children() {
        return childrenView;
    }

    // ---- settings ----

    public YmContainer setLayout(@Nullable YmLayout layout) {
        this.layout = layout;
        return this;
    }

    public YmContainer setBackground(Background background) {
        this.background = background;
        return this;
    }

    /** Children outside the content area are cut off (and do not receive input there). */
    public YmContainer setClip(boolean clip) {
        this.clip = clip;
        return this;
    }

    public YmContainer setPadding(int all) {
        return setPadding(all, all, all, all);
    }

    public YmContainer setPadding(int left, int top, int right, int bottom) {
        this.padLeft = left;
        this.padTop = top;
        this.padRight = right;
        this.padBottom = bottom;
        return this;
    }

    // ---- geometry ----

    /** Absolute position of the content origin (inside the padding, minus the scroll offset). */
    public int contentX() {
        return getX() + padLeft - scrollX;
    }

    public int contentY() {
        return getY() + padTop - scrollY;
    }

    /** Size of the visible content area. */
    public int contentWidth() {
        return Math.max(0, width - padLeft - padRight);
    }

    public int contentHeight() {
        return Math.max(0, height - padTop - padBottom);
    }

    /** Height needed by the children (bottom edge of the lowest child). */
    public int childrenHeight() {
        int bottom = 0;
        for (YmElement child : children) {
            if (child.isVisible()) {
                bottom = Math.max(bottom, child.y + child.height);
            }
        }
        return bottom;
    }

    /** True if the point is not cut off by this panel or one of its parents. */
    public boolean isInsideClip(double mouseX, double mouseY) {
        if (clip) {
            int ix = getX() + padLeft;
            int iy = getY() + padTop;
            if (mouseX < ix || mouseY < iy || mouseX >= ix + contentWidth() || mouseY >= iy + contentHeight()) {
                return false;
            }
        }
        return parent == null || parent.isInsideClip(mouseX, mouseY);
    }

    @Override
    @Nullable
    public YmScreen screen() {
        return owner != null ? owner : super.screen();
    }

    /** Arranges the children with the layout, then lets child panels arrange theirs. */
    public void layout() {
        if (layout != null) {
            layout.apply(this);
        }
        for (YmElement child : children) {
            if (child instanceof YmContainer panel) {
                panel.layout();
            }
        }
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        if (clip) {
            graphics.enableScissor(getX() + padLeft, getY() + padTop, getX() + padLeft + contentWidth(),
                    getY() + padTop + contentHeight());
        }
        for (YmElement child : children) {
            if (child.isVisible()) {
                child.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        if (clip) {
            graphics.disableScissor();
        }
        renderForeground(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int ax = getX();
        int ay = getY();
        switch (background) {
            case PLATE -> style.drawPlate(graphics, ax, ay, width, height, style.panelFill);
            case INSET -> style.drawInset(graphics, ax, ay, width, height);
            case WINDOW -> style.drawPlate(graphics, ax, ay, width, height, style.windowFill);
            case NONE -> {
            }
        }
    }

    /** Drawn after (above) the children, outside the clip. */
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    // ---- input ----

    @Override
    @Nullable
    public YmElement elementAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return null;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            YmElement hit = children.get(i).elementAt(mouseX, mouseY);
            if (hit != null) {
                return hit;
            }
        }
        return this;
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            YmElement child = children.get(i);
            if (child.isMouseOver(mouseX, mouseY) && child.isEnabled() && child.mousePressed(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (int i = children.size() - 1; i >= 0; i--) {
            YmElement child = children.get(i);
            if (child.isMouseOver(mouseX, mouseY) && child.isEnabled() && child.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        for (YmElement child : List.copyOf(children)) {
            child.tick();
        }
    }
}
