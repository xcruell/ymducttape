package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Wide on/off switch: the knob slides to the right (on, accent-colored track, "I" on the free left side) or to the
 * left (off, "O" on the free right side). Optional label to the right of the switch; without a label it fits into a
 * {@link YmFormRow}. Click, Space or Enter toggles.
 */
public class YmSwitch extends YmElement {
    private static final int TRACK_WIDTH = 28;
    private static final int TRACK_HEIGHT = 12;
    private static final int KNOB = 12;
    /** Knob travel time in milliseconds. */
    private static final float SLIDE_MS = 110.0F;
    private static final String[] GLYPH_ON = {".#.", ".#.", ".#.", ".#.", ".#."};
    private static final String[] GLYPH_OFF = {".#.", "#.#", "#.#", "#.#", ".#."};

    private final Component label;
    private final Consumer<Boolean> onChange;
    private boolean on;
    /** 0 = knob left (off), 1 = knob right (on); follows {@link #on} with a short slide. */
    private float knob;
    private long lastFrame;

    public YmSwitch(boolean on, Consumer<Boolean> onChange) {
        this(Component.empty(), on, onChange);
    }

    public YmSwitch(Component label, boolean on, Consumer<Boolean> onChange) {
        super(0, 14);
        this.label = label;
        this.on = on;
        this.knob = on ? 1.0F : 0.0F;
        this.onChange = onChange;
        this.width = TRACK_WIDTH + (label.getString().isEmpty() ? 0 : 6 + font().width(label));
    }

    public boolean isOn() {
        return on;
    }

    /** Sets the state without the change callback (and without the slide). */
    public void setOn(boolean on) {
        this.on = on;
        this.knob = on ? 1.0F : 0.0F;
    }

    private void toggle() {
        playClick();
        on = !on;
        onChange.accept(on);
    }

    private void animate() {
        long now = Util.getMillis();
        float step = lastFrame == 0 ? 1.0F : Math.min(1.0F, (now - lastFrame) / SLIDE_MS);
        lastFrame = now;
        float target = on ? 1.0F : 0.0F;
        knob = knob < target ? Math.min(target, knob + step) : Math.max(target, knob - step);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        animate();
        YmStyle style = style();
        int x = getX();
        int y = getY() + (height - TRACK_HEIGHT) / 2;
        boolean enabled = isEnabled();
        boolean hovered = enabled && isMouseOver(mouseX, mouseY);

        style.drawInset(graphics, x, y, TRACK_WIDTH, TRACK_HEIGHT);
        if (enabled && knob > 0.0F) {
            // Accent fill grows with the knob, so it fades in while sliding.
            int alpha = Math.round(0xB0 * knob);
            graphics.fill(x + 1, y + 1, x + TRACK_WIDTH - 1, y + TRACK_HEIGHT - 1,
                    YmStyle.withAlpha(style.accentDim, alpha));
        }
        // Glyph on the free side: "I" left while on, "O" right while off.
        int glyphColor = !enabled ? style.textDisabled : on ? style.text : style.textDim;
        int glyphY = y + (TRACK_HEIGHT - 5) / 2;
        int free = TRACK_WIDTH - KNOB;
        if (on) {
            drawGlyph(graphics, GLYPH_ON, x + free / 2 - 1, glyphY, glyphColor);
        } else {
            drawGlyph(graphics, GLYPH_OFF, x + KNOB + free / 2 - 1, glyphY, glyphColor);
        }
        int knobX = x + Math.round(knob * (TRACK_WIDTH - KNOB));
        style.drawControl(graphics, knobX, y, KNOB, TRACK_HEIGHT, hovered || isFocused(), false, enabled, false);
        // Grip lines on the knob.
        int gripColor = enabled ? (on ? style.accent : style.bevelLight) : style.textDisabled;
        graphics.fill(knobX + 4, y + 3, knobX + 5, y + TRACK_HEIGHT - 3, gripColor);
        graphics.fill(knobX + 7, y + 3, knobX + 8, y + TRACK_HEIGHT - 3, gripColor);
        if (hovered) {
            style.outline(graphics, x, y, TRACK_WIDTH, TRACK_HEIGHT, style.accentDim);
        }

        if (!label.getString().isEmpty()) {
            int color = enabled ? style.text : style.textDisabled;
            Component shown = YmStyle.ellipsize(font(), label, width - TRACK_WIDTH - 6);
            style.drawText(graphics, font(), shown, x + TRACK_WIDTH + 6, getY() + (height - 8) / 2, color);
        }
    }

    private static void drawGlyph(GuiGraphics graphics, String[] rows, int x, int y, int color) {
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                if (rows[row].charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
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
