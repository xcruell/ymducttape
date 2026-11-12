package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Editor for a curve over time (e.g. the brightness pattern of a lamp's startup flicker). Time runs from 0 (left) to
 * 1 (right), the value from 0 (bottom) to 1 (top). Drag points to move them (Shift snaps to a 0.05 grid), click on an
 * empty spot to add a point, right-click a point to remove it. The first and last point stay at the start and end.
 * Optional looping playhead to preview the curve.
 */
public class YmCurveEditor extends YmElement {
    /** How the value runs between two points. */
    public enum Mode {
        /** Straight line between the points. */
        LINEAR,
        /** Holds each point's value until the next point (on/off patterns). */
        STEP
    }

    public record Point(float time, float value) {
    }

    private static final int PAD = 4;
    private static final int HANDLE = 5;
    private static final float SNAP = 0.05F;

    private final List<Point> points = new ArrayList<>();
    private final Consumer<List<Point>> onChange;
    private Mode mode;
    private int dragging = -1;
    private long playDuration;

    public YmCurveEditor(List<Point> points, Mode mode, Consumer<List<Point>> onChange) {
        super(0, 70);
        this.mode = mode;
        this.onChange = onChange;
        setPoints(points);
    }

    /** Value of a curve at {@code time} (0..1); use this to apply the curve in game exactly as it is shown. */
    public static float sample(List<Point> points, Mode mode, float time) {
        if (points.isEmpty()) {
            return 0.0F;
        }
        if (time <= points.get(0).time()) {
            return points.get(0).value();
        }
        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i);
            Point b = points.get(i + 1);
            if (time < b.time()) {
                if (mode == Mode.STEP || b.time() <= a.time()) {
                    return a.value();
                }
                float f = (time - a.time()) / (b.time() - a.time());
                return a.value() + (b.value() - a.value()) * f;
            }
        }
        return points.get(points.size() - 1).value();
    }

    public List<Point> getPoints() {
        return List.copyOf(points);
    }

    /** Sets the points without the change callback; adds start and end points if missing. */
    public void setPoints(List<Point> newPoints) {
        points.clear();
        for (Point p : newPoints) {
            points.add(new Point(clamp(p.time()), clamp(p.value())));
        }
        points.sort(Comparator.comparingDouble(Point::time));
        if (points.isEmpty() || points.get(0).time() > 0.0F) {
            points.add(0, new Point(0.0F, points.isEmpty() ? 0.0F : points.get(0).value()));
        }
        if (points.get(points.size() - 1).time() < 1.0F) {
            points.add(new Point(1.0F, points.get(points.size() - 1).value()));
        }
    }

    public YmCurveEditor setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    /** Shows a playhead that runs through the curve once every {@code millis} (0 = off). */
    public YmCurveEditor setPlayback(long millis) {
        this.playDuration = millis;
        return this;
    }

    /** Current playhead time (0..1), or -1 without playback. */
    public float playheadTime() {
        return playDuration <= 0 ? -1.0F : (Util.getMillis() % playDuration) / (float) playDuration;
    }

    /** Curve value at the playhead (0 without playback). */
    public float playheadValue() {
        float t = playheadTime();
        return t < 0.0F ? 0.0F : sample(points, mode, t);
    }

    // ---- geometry ----

    private int left() {
        return getX() + PAD;
    }

    private int top() {
        return getY() + PAD;
    }

    private int plotWidth() {
        return Math.max(1, width - 2 * PAD);
    }

    private int plotHeight() {
        return Math.max(1, height - 2 * PAD);
    }

    private int screenX(float time) {
        return left() + Math.round(time * (plotWidth() - 1));
    }

    private int screenY(float value) {
        return top() + Math.round((1.0F - value) * (plotHeight() - 1));
    }

    private int pointAt(double mouseX, double mouseY) {
        for (int i = points.size() - 1; i >= 0; i--) {
            Point p = points.get(i);
            if (Math.abs(mouseX - screenX(p.time())) <= HANDLE && Math.abs(mouseY - screenY(p.value())) <= HANDLE) {
                return i;
            }
        }
        return -1;
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        style.drawInset(graphics, x, y, width, height);
        int gridColor = YmStyle.withAlpha(style.bevelLight, 0x60);
        for (int i = 1; i < 10; i++) {
            int gx = left() + plotWidth() * i / 10;
            graphics.fill(gx, top(), gx + 1, top() + plotHeight(), gridColor);
        }
        for (int i = 1; i < 4; i++) {
            int gy = top() + plotHeight() * i / 4;
            graphics.fill(left(), gy, left() + plotWidth(), gy + 1, gridColor);
        }

        // Curve: per pixel column, filled below and a 1 px line that bridges jumps between columns.
        int fill = YmStyle.withAlpha(style.accent, 0x30);
        int prevY = screenY(sample(points, mode, 0.0F));
        for (int px = 0; px < plotWidth(); px++) {
            float t = px / (float) (plotWidth() - 1);
            int cy = screenY(sample(points, mode, t));
            int sx = left() + px;
            graphics.fill(sx, cy, sx + 1, top() + plotHeight(), fill);
            graphics.fill(sx, Math.min(prevY, cy), sx + 1, Math.max(prevY, cy) + 1, style.accent);
            prevY = cy;
        }

        float play = playheadTime();
        if (play >= 0.0F) {
            int hx = screenX(play);
            graphics.fill(hx, top(), hx + 1, top() + plotHeight(), YmStyle.withAlpha(style.text, 0x90));
            int hy = screenY(sample(points, mode, play));
            graphics.fill(hx - 2, hy - 2, hx + 3, hy + 3, style.text);
        }

        int hovered = dragging >= 0 ? dragging : isMouseOver(mouseX, mouseY) ? pointAt(mouseX, mouseY) : -1;
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            int px = screenX(p.time());
            int py = screenY(p.value());
            int half = HANDLE / 2;
            graphics.fill(px - half - 1, py - half - 1, px + half + 2, py + half + 2, style.outline);
            graphics.fill(px - half, py - half, px + half + 1, py + half + 1, i == hovered ? style.text : style.accent);
        }
    }

    // ---- input ----

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        int hit = pointAt(mouseX, mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (hit > 0 && hit < points.size() - 1) {
                points.remove(hit);
                playClick();
                onChange.accept(getPoints());
            }
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        requestFocus();
        if (hit < 0) {
            Point clicked = toPoint(mouseX, mouseY);
            // New points stay strictly inside, so the start and end points keep their place.
            Point added = new Point(Math.max(0.001F, Math.min(0.999F, clicked.time())), clicked.value());
            points.add(added);
            points.sort(Comparator.comparingDouble(Point::time));
            hit = points.indexOf(added);
            onChange.accept(getPoints());
        }
        dragging = hit;
        captureMouse();
        return true;
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging < 0) {
            return;
        }
        Point moved = toPoint(mouseX, mouseY);
        float time = moved.time();
        if (dragging == 0) {
            time = 0.0F;
        } else if (dragging == points.size() - 1) {
            time = 1.0F;
        } else {
            // A point stays between its neighbours, so the order (and the dragged index) never changes.
            time = Math.max(points.get(dragging - 1).time(), Math.min(points.get(dragging + 1).time(), time));
        }
        points.set(dragging, new Point(time, moved.value()));
        onChange.accept(getPoints());
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = -1;
    }

    private Point toPoint(double mouseX, double mouseY) {
        float time = clamp((float) ((mouseX - left()) / (plotWidth() - 1)));
        float value = clamp(1.0F - (float) ((mouseY - top()) / (plotHeight() - 1)));
        if (Screen.hasShiftDown()) {
            time = Math.round(time / SNAP) * SNAP;
            value = Math.round(value / SNAP) * SNAP;
        }
        return new Point(clamp(time), clamp(value));
    }

    private static float clamp(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }
}
