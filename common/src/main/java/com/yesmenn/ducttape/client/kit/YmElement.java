package com.yesmenn.ducttape.client.kit;

import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base of every element of the YM GUI kit. Positions are relative to the parent panel's content area; {@link #getX()}
 * / {@link #getY()} give the absolute screen position. Input arrives with absolute mouse coordinates.
 */
public abstract class YmElement {
    @Nullable
    YmContainer parent;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    private boolean visible = true;
    private boolean enabled = true;
    @Nullable
    private Supplier<List<Component>> tooltip;

    protected YmElement(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // ---- geometry ----

    public YmElement setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public YmElement setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public YmElement setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /** Position relative to the parent's content area. */
    public int getRelX() {
        return x;
    }

    public int getRelY() {
        return y;
    }

    /** Absolute screen position. */
    public int getX() {
        return parent == null ? x : parent.contentX() + x;
    }

    public int getY() {
        return parent == null ? y : parent.contentY() + y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean contains(double mouseX, double mouseY) {
        int ax = getX();
        int ay = getY();
        return mouseX >= ax && mouseY >= ay && mouseX < ax + width && mouseY < ay + height;
    }

    /** True if the mouse is over this widget and the widget is not clipped away by a parent at that point. */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isVisible() && contains(mouseX, mouseY) && (parent == null || parent.isInsideClip(mouseX, mouseY));
    }

    // ---- state ----

    public boolean isVisible() {
        return visible;
    }

    public YmElement setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean isEnabled() {
        return enabled && (parent == null || parent.isEnabled());
    }

    public YmElement setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Nullable
    public YmContainer getParent() {
        return parent;
    }

    @Nullable
    public YmScreen screen() {
        return parent == null ? null : parent.screen();
    }

    public YmStyle style() {
        YmScreen screen = screen();
        return screen == null ? YmStyle.DEFAULT : screen.style();
    }

    protected Font font() {
        return Minecraft.getInstance().font;
    }

    // ---- tooltip ----

    public YmElement setTooltip(Component... lines) {
        List<Component> list = List.of(lines);
        this.tooltip = list.isEmpty() ? null : () -> list;
        return this;
    }

    public YmElement setTooltip(@Nullable Supplier<List<Component>> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    /** Tooltip lines for the mouse position, or an empty list. */
    public List<Component> getTooltip(double mouseX, double mouseY) {
        return tooltip == null ? List.of() : new ArrayList<>(tooltip.get());
    }

    // ---- focus / capture ----

    public boolean isFocused() {
        YmScreen screen = screen();
        return screen != null && screen.getFocusedElement() == this;
    }

    public void requestFocus() {
        YmScreen screen = screen();
        if (screen != null) {
            screen.setFocusedElement(this);
        }
    }

    /** Called when the widget gains or loses keyboard focus. */
    protected void onFocusChanged(boolean focused) {
    }

    /** Sends the following drag and release events to this widget, even when the mouse leaves it. */
    protected void captureMouse() {
        YmScreen screen = screen();
        if (screen != null) {
            screen.capture(this);
        }
    }

    /** Whether the screen fades out while this element is dragged (see {@link YmScreen#setFadeWhileDragging}). */
    protected boolean fadesScreenWhileDragged() {
        return false;
    }

    protected static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ---- rendering / input ----

    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /** Deepest visible widget under the mouse (this one for leaves). */
    @Nullable
    public YmElement elementAt(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY) ? this : null;
    }

    public boolean mousePressed(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    /** Called every client tick while the screen is open. */
    public void tick() {
    }
}
