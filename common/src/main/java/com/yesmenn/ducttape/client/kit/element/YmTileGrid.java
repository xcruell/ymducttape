package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Gallery of tiles (e.g. color presets): each tile shows a color and/or an item with its name below. Left click
 * selects, right click opens an action menu (e.g. rename, delete) if one is set. An optional "+" tile at the end adds
 * a new entry. The grid wraps to the width; its height follows.
 */
public class YmTileGrid extends YmElement {
    public static final int TILE_WIDTH = 40;
    public static final int TILE_HEIGHT = 36;
    private static final int GAP = 4;

    /** One tile: a color (0xRRGGBB, or -1 for none) and/or an item icon, with a name. */
    public record Tile(Component name, int color, ItemStack icon) {
        public static Tile color(Component name, int rgb) {
            return new Tile(name, rgb, ItemStack.EMPTY);
        }

        public static Tile item(Component name, ItemStack icon) {
            return new Tile(name, -1, icon);
        }
    }

    private final List<Tile> tiles;
    private final IntConsumer onSelect;
    private int selected;
    @Nullable
    private BiConsumer<Integer, YmActionMenu> contextMenu;
    @Nullable
    private Runnable onAdd;

    /**
     * @param selected index of the selected tile, or -1
     * @param onSelect called with the index of a clicked tile
     */
    public YmTileGrid(List<Tile> tiles, int selected, IntConsumer onSelect) {
        super(0, TILE_HEIGHT);
        this.tiles = List.copyOf(tiles);
        this.selected = selected;
        this.onSelect = onSelect;
    }

    /** Right click on a tile opens an action menu, filled by {@code builder} with the tile's index. */
    public YmTileGrid setContextMenu(@Nullable BiConsumer<Integer, YmActionMenu> builder) {
        this.contextMenu = builder;
        return this;
    }

    /** Shows a "+" tile at the end that runs {@code onAdd}. */
    public YmTileGrid setOnAdd(@Nullable Runnable onAdd) {
        this.onAdd = onAdd;
        return this;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    private int tileCount() {
        return tiles.size() + (onAdd != null ? 1 : 0);
    }

    private int columns() {
        return Math.max(1, (width + GAP) / (TILE_WIDTH + GAP));
    }

    @Override
    public YmTileGrid setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        int rows = Math.max(1, (tileCount() + columns() - 1) / columns());
        this.height = rows * TILE_HEIGHT + (rows - 1) * GAP;
        return this;
    }

    private int tileX(int i) {
        return getX() + (i % columns()) * (TILE_WIDTH + GAP);
    }

    private int tileY(int i) {
        return getY() + (i / columns()) * (TILE_HEIGHT + GAP);
    }

    private int tileAt(double mouseX, double mouseY) {
        for (int i = 0; i < tileCount(); i++) {
            int x = tileX(i);
            int y = tileY(i);
            if (mouseX >= x && mouseX < x + TILE_WIDTH && mouseY >= y && mouseY < y + TILE_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int hovered = isEnabled() && isMouseOver(mouseX, mouseY) ? tileAt(mouseX, mouseY) : -1;
        for (int i = 0; i < tileCount(); i++) {
            int x = tileX(i);
            int y = tileY(i);
            boolean isAdd = i >= tiles.size();
            boolean isSelected = i == selected;
            style.drawControl(graphics, x, y, TILE_WIDTH, TILE_HEIGHT, i == hovered, false, isEnabled(), isSelected);
            if (isAdd) {
                YmIconButton.Icon.PLUS.draw(graphics, x + (TILE_WIDTH - 7) / 2, y + (TILE_HEIGHT - 7) / 2,
                        i == hovered ? style.accent : style.textDim);
                continue;
            }
            Tile tile = tiles.get(i);
            int previewX = x + 4;
            int previewY = y + 4;
            int previewW = TILE_WIDTH - 8;
            int previewH = 18;
            if (tile.color() >= 0) {
                style.drawInset(graphics, previewX, previewY, previewW, previewH);
                graphics.fill(previewX + 1, previewY + 1, previewX + previewW - 1, previewY + previewH - 1,
                        0xFF000000 | tile.color());
            }
            if (!tile.icon().isEmpty()) {
                graphics.renderItem(tile.icon(), x + (TILE_WIDTH - 16) / 2, previewY + 1);
            }
            if (isSelected) {
                style.outline(graphics, x, y, TILE_WIDTH, TILE_HEIGHT, style.accent);
            }
            Component name = YmStyle.ellipsize(font(), tile.name(), TILE_WIDTH - 4);
            int color = isSelected ? style.text : style.textDim;
            style.drawCenteredText(graphics, font(), name, x + TILE_WIDTH / 2, y + TILE_HEIGHT - 11, color);
        }
    }

    @Override
    public java.util.List<Component> getTooltip(double mouseX, double mouseY) {
        int i = tileAt(mouseX, mouseY);
        if (i >= 0 && i < tiles.size()) {
            return List.of(tiles.get(i).name());
        }
        return super.getTooltip(mouseX, mouseY);
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        int i = tileAt(mouseX, mouseY);
        if (i < 0) {
            return false;
        }
        if (i >= tiles.size()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && onAdd != null) {
                playClick();
                onAdd.run();
                return true;
            }
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            playClick();
            selected = i;
            onSelect.accept(i);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && contextMenu != null) {
            YmScreen screen = screen();
            if (screen != null) {
                YmActionMenu menu = new YmActionMenu();
                contextMenu.accept(i, menu);
                menu.open(screen, (int) mouseX, (int) mouseY);
            }
            return true;
        }
        return false;
    }
}
