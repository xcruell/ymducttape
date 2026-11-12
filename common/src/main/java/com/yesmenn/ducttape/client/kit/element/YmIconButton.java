package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/** Small square button with a pixel icon (close, arrows, plus/minus). Drawn flat until hovered. */
public class YmIconButton extends YmElement {
    /** Icons as 7x7 pixel masks, one string per row ('#' = pixel). */
    public enum Icon {
        CLOSE("#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#"),
        PLUS("...#...", "...#...", "...#...", "#######", "...#...", "...#...", "...#..."),
        MINUS(".......", ".......", ".......", "#######", ".......", ".......", "......."),
        ARROW_DOWN(".......", ".......", "#######", ".#####.", "..###..", "...#...", "......."),
        ARROW_UP(".......", "...#...", "..###..", ".#####.", "#######", ".......", "......."),
        ARROW_LEFT("....#..", "...##..", "..###..", ".####..", "..###..", "...##..", "....#.."),
        ARROW_RIGHT("..#....", "..##...", "..###..", "..####.", "..###..", "..##...", "..#...."),
        RESET("..###.#", ".#...##", "#...###", "#......", "#.....#", ".#...#.", "..###.."),
        EDIT("....##.", "...#..#", "..#..#.", ".#..#..", "#..#...", "##.....", "###...."),
        DELETE(".......", "#######", ".#...#.", ".#.#.#.", ".#.#.#.", ".#...#.", "..###..");

        private final String[] rows;

        Icon(String... rows) {
            this.rows = rows;
        }

        public void draw(GuiGraphics graphics, int x, int y, int color) {
            for (int row = 0; row < rows.length; row++) {
                String line = rows[row];
                for (int col = 0; col < line.length(); col++) {
                    if (line.charAt(col) == '#') {
                        graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                    }
                }
            }
        }
    }

    private Icon icon;
    private final Consumer<YmIconButton> action;

    public YmIconButton(Icon icon, Consumer<YmIconButton> action) {
        super(12, 12);
        this.icon = icon;
        this.action = action;
    }

    public YmIconButton setIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
        if (hovered) {
            style.drawPlate(graphics, x, y, width, height, style.controlHover);
        }
        int color = !isEnabled() ? style.textDisabled : hovered ? style.accent : style.textDim;
        icon.draw(graphics, x + (width - 7) / 2, y + (height - 7) / 2, color);
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        playClick();
        action.accept(this);
        return true;
    }
}
