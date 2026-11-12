package com.yesmenn.ducttape.client.kit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yesmenn.ducttape.client.kit.style.YmKitSettings;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.Util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * Screen of the YM GUI kit. Holds a root {@link YmContainer} filling the screen and a stack of popups (context menus,
 * dropdowns, dialogs) drawn above it. Subclasses build their widgets in {@link #build(YmContainer)}; it runs again on every
 * resize, so keep state in fields of the screen, not only in widgets.
 */
public abstract class YmScreen extends Screen {
    private static final float POPUP_Z = 300.0F;
    /** Opacity of the windows while a value is dragged (see {@link #setFadeWhileDragging}). */
    private static final float FADED_ALPHA = 0.25F;

    private final YmContainer root = new YmContainer();
    private final List<YmContainer> popups = new ArrayList<>();
    private final Set<YmContainer> modals = new HashSet<>();
    private final List<Toast> toasts = new ArrayList<>();
    @Nullable
    private final Screen parent;
    private int accent = YmStyle.ACCENT_TECHNICALS;
    private YmStyle style = YmKitSettings.createStyle(accent);
    @Nullable
    private YmElement focused;
    @Nullable
    private YmElement captured;
    private boolean focusRequested;
    private boolean showWorld;
    private boolean fadeWhileDragging;

    protected YmScreen(Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
        root.owner = this;
    }

    /** Adds the widgets to {@code root} (which fills the screen). */
    protected abstract void build(YmContainer root);

    public YmStyle style() {
        return style;
    }

    /**
     * Uses the player's chosen style (kit settings) with this accent color; the usual way for a screen to pick its look
     * (e.g. {@link YmStyle#ACCENT_LIGHTS} for a lamp screen). Applied again on every rebuild.
     */
    public void setAccent(int accent) {
        this.accent = accent;
        this.style = YmKitSettings.createStyle(accent);
    }

    /** Uses a fixed style instead of the player's choice (until the next rebuild). */
    public void setStyle(YmStyle style) {
        this.style = style;
    }

    /**
     * Draws no darkened/blurred background, so the world stays visible behind the windows (e.g. a lamp screen whose
     * changes the player watches live).
     */
    public void setShowWorld(boolean showWorld) {
        this.showWorld = showWorld;
    }

    /**
     * While the player drags a value (slider, color field), the windows turn almost transparent so the effect on the
     * world can be seen; they come back when the mouse is released.
     */
    public void setFadeWhileDragging(boolean fade) {
        this.fadeWhileDragging = fade;
    }

    /** Whether the windows are faded right now (see {@link #setFadeWhileDragging}). */
    public boolean isFaded() {
        return fadeWhileDragging && captured != null && captured.fadesScreenWhileDragged();
    }

    public YmContainer root() {
        return root;
    }

    @Override
    protected void init() {
        root.clear();
        popups.clear();
        focused = null;
        captured = null;
        root.setBounds(0, 0, width, height);
        style = YmKitSettings.createStyle(accent);
        build(root);
        root.layout();
    }

    /** Builds the widgets again (e.g. after a change that adds or removes widgets). */
    public void rebuild() {
        init();
    }

    /** Re-runs the layouts without rebuilding (e.g. after showing or hiding widgets). */
    public void relayout() {
        root.layout();
        popups.forEach(YmContainer::layout);
    }

    // ---- popups ----

    /**
     * Opens a popup at its own (absolute) position, moved inside the screen if needed. Clicking outside closes it.
     */
    public void openPopup(YmContainer popup) {
        popup.owner = this;
        int px = Math.max(2, Math.min(popup.getRelX(), width - popup.getWidth() - 2));
        int py = Math.max(2, Math.min(popup.getRelY(), height - popup.getHeight() - 2));
        popup.setPos(px, py);
        popup.layout();
        popups.add(popup);
    }

    /**
     * Opens a modal popup (dialog) centered on the screen: the screen behind it is darkened, and clicks outside do not
     * close it (only its own buttons or Escape).
     */
    public void openModal(YmContainer popup) {
        popup.setPos((width - popup.getWidth()) / 2, (height - popup.getHeight()) / 2);
        modals.add(popup);
        openPopup(popup);
    }

    public void closePopup(YmContainer popup) {
        popups.remove(popup);
        modals.remove(popup);
        if (focused != null && isInside(focused, popup)) {
            setFocusedElement(null);
        }
    }

    public void closeAllPopups() {
        List.copyOf(popups).forEach(this::closePopup);
    }

    public boolean hasPopup() {
        return !popups.isEmpty();
    }

    private static boolean isInside(YmElement widget, YmContainer panel) {
        for (YmElement w = widget; w != null; w = w.getParent()) {
            if (w == panel) {
                return true;
            }
        }
        return false;
    }

    // ---- focus / capture ----

    @Nullable
    public YmElement getFocusedElement() {
        return focused;
    }

    public void setFocusedElement(@Nullable YmElement widget) {
        focusRequested = true;
        if (focused == widget) {
            return;
        }
        YmElement old = focused;
        focused = widget;
        if (old != null) {
            old.onFocusChanged(false);
        }
        if (widget != null) {
            widget.onFocusChanged(true);
        }
    }

    void capture(YmElement widget) {
        captured = widget;
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        boolean popupOpen = !popups.isEmpty();
        // Widgets below an open popup do not show hover effects.
        int rootMouseX = popupOpen ? -1000 : mouseX;
        int rootMouseY = popupOpen ? -1000 : mouseY;
        boolean faded = isFaded();
        if (faded) {
            graphics.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, FADED_ALPHA);
        }
        root.render(graphics, rootMouseX, rootMouseY, partialTick);
        if (faded) {
            graphics.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        for (int i = 0; i < popups.size(); i++) {
            YmContainer popup = popups.get(i);
            boolean top = i == popups.size() - 1;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, POPUP_Z + i * 10.0F);
            if (modals.contains(popup)) {
                graphics.fill(0, 0, width, height, 0x90000000);
            }
            popup.render(graphics, top ? mouseX : -1000, top ? mouseY : -1000, partialTick);
            graphics.pose().popPose();
        }
        renderToasts(graphics);
        if (captured == null) {
            YmElement hovered = elementAt(mouseX, mouseY);
            if (hovered != null) {
                List<Component> tooltip = hovered.getTooltip(mouseX, mouseY);
                if (!tooltip.isEmpty()) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0.0F, 0.0F, POPUP_Z + 100.0F);
                    graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                    graphics.pose().popPose();
                }
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!showWorld) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Topmost widget under the mouse (popups first). */
    @Nullable
    public YmElement elementAt(double mouseX, double mouseY) {
        if (!popups.isEmpty()) {
            return popups.get(popups.size() - 1).elementAt(mouseX, mouseY);
        }
        return root.elementAt(mouseX, mouseY);
    }

    // ---- toasts ----

    private record Toast(Component text, int color, long start) {
    }

    private static final long TOAST_MS = 3000;
    private static final long TOAST_FADE_MS = 400;

    /** Short message in the bottom right corner that fades out after a few seconds (e.g. "Preset saved"). */
    public void toast(Component text) {
        toast(text, style.accent);
    }

    /** Toast with its own marker color (e.g. red for an error). */
    public void toast(Component text, int color) {
        toasts.add(new Toast(text, color, Util.getMillis()));
        if (toasts.size() > 5) {
            toasts.remove(0);
        }
    }

    private void renderToasts(GuiGraphics graphics) {
        long now = Util.getMillis();
        toasts.removeIf(toast -> now - toast.start() > TOAST_MS);
        int y = height - 6;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z + 150.0F);
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast toast = toasts.get(i);
            long age = now - toast.start();
            float alpha = age > TOAST_MS - TOAST_FADE_MS ? (TOAST_MS - age) / (float) TOAST_FADE_MS : 1.0F;
            int a = Math.max(8, Math.round(alpha * 255));
            int w = font.width(toast.text()) + 16;
            int h = 18;
            int x = width - w - 6;
            y -= h;
            graphics.fill(x, y, x + w, y + h, YmStyle.withAlpha(style.outline, Math.round(a * 0.95F)));
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, YmStyle.withAlpha(style.titleFill, Math.round(a * 0.95F)));
            graphics.fill(x + 1, y + 1, x + 3, y + h - 1, YmStyle.withAlpha(toast.color(), a));
            graphics.drawString(font, toast.text(), x + 9, y + 5, YmStyle.withAlpha(style.text, a), false);
            y -= 3;
        }
        graphics.pose().popPose();
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        focusRequested = false;
        boolean handled;
        if (!popups.isEmpty()) {
            YmContainer top = popups.get(popups.size() - 1);
            if (top.isMouseOver(mouseX, mouseY)) {
                top.mousePressed(mouseX, mouseY, button);
            } else if (!modals.contains(top)) {
                closePopup(top);
            }
            handled = true;
        } else {
            handled = root.mousePressed(mouseX, mouseY, button);
        }
        if (!focusRequested) {
            setFocusedElement(null);
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured != null) {
            YmElement widget = captured;
            captured = null;
            widget.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (captured != null) {
            captured.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!popups.isEmpty()) {
            YmContainer top = popups.get(popups.size() - 1);
            return top.isMouseOver(mouseX, mouseY) && top.mouseScrolled(mouseX, mouseY, scrollY);
        }
        return root.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focused != null && focused.isVisible() && focused.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!popups.isEmpty()) {
                closePopup(popups.get(popups.size() - 1));
                return true;
            }
            if (focused != null) {
                setFocusedElement(null);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return focused != null && focused.isVisible() && focused.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        root.tick();
        List.copyOf(popups).forEach(YmContainer::tick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        YmKitSettings.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
