package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.YmScrollBox;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Modal item / block chooser: a search field and a scrolling grid of all matching items. The search matches the
 * item name and ID; "@mod" limits it to a mod (e.g. "@minecraft glass"). Picking an item closes the dialog.
 */
public class YmItemPicker extends YmDialog {
    private static final int COLUMNS = 10;
    private static final int CELL = 18;

    /** Only items that place a block. */
    public static final Predicate<Item> BLOCKS = item -> item instanceof BlockItem;
    public static final Predicate<Item> ALL = item -> true;

    private record Entry(ItemStack stack, String name, String id, String namespace) {
    }

    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private final Consumer<ItemStack> onPick;
    private final YmScrollBox list = new YmScrollBox();

    private YmItemPicker(Component title, Predicate<Item> filter, Consumer<ItemStack> onPick) {
        super(title, COLUMNS * CELL + YmScrollBox.BAR_WIDTH + 30);
        this.onPick = onPick;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || !filter.test(item)) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            all.add(new Entry(stack, stack.getHoverName().getString().toLowerCase(Locale.ROOT), id.toString(),
                    id.getNamespace()));
        }
        YmTextField search = content().add(new YmTextField("", this::search));
        search.setHint(Component.translatable("ymducttape.kit.search"));
        list.setSize(0, CELL * 8 + 4);
        list.setBackground(YmContainer.Background.INSET);
        list.setLayout(YmLayout.column(0));
        list.add(new Grid());
        content().add(list);
        addButton(Component.translatable("gui.cancel"), () -> {
        });
        search("");
    }

    /**
     * Opens the picker.
     *
     * @param filter which items to list (e.g. {@link #BLOCKS})
     */
    public static YmItemPicker open(YmScreen screen, Component title, Predicate<Item> filter,
                                    Consumer<ItemStack> onPick) {
        YmItemPicker picker = new YmItemPicker(title, filter, onPick);
        picker.open(screen);
        picker.content().children().get(0).requestFocus();
        return picker;
    }

    private void search(String query) {
        shown.clear();
        String namespace = null;
        List<String> words = new ArrayList<>();
        for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (word.startsWith("@") && word.length() > 1) {
                namespace = word.substring(1);
            } else if (!word.isEmpty()) {
                words.add(word);
            }
        }
        for (Entry entry : all) {
            if (namespace != null && !entry.namespace().startsWith(namespace)) {
                continue;
            }
            boolean matches = true;
            for (String word : words) {
                if (!entry.name().contains(word) && !entry.id().contains(word)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                shown.add(entry);
            }
        }
        list.setScroll(0);
        if (screen() != null) {
            screen().relayout();
        }
    }

    /** The item cells; only the rows inside the visible part of the list are drawn. */
    private final class Grid extends YmElement {
        private Grid() {
            super(COLUMNS * CELL, CELL);
        }

        @Override
        public YmElement setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, COLUMNS * CELL, Math.max(CELL, (shown.size() + COLUMNS - 1) / COLUMNS * CELL));
            return this;
        }

        private int indexAt(double mouseX, double mouseY) {
            int col = (int) Math.floor((mouseX - getX()) / CELL);
            int row = (int) Math.floor((mouseY - getY()) / CELL);
            if (col < 0 || col >= COLUMNS || row < 0) {
                return -1;
            }
            int index = row * COLUMNS + col;
            return index < shown.size() ? index : -1;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            YmStyle style = style();
            int x = getX();
            int y = getY();
            int visibleTop = list.getY();
            int visibleBottom = visibleTop + list.getHeight();
            int firstRow = Math.max(0, (visibleTop - y) / CELL);
            int lastRow = Math.max(0, (visibleBottom - y) / CELL + 1);
            int hovered = isMouseOver(mouseX, mouseY) ? indexAt(mouseX, mouseY) : -1;
            for (int row = firstRow; row <= lastRow; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    int index = row * COLUMNS + col;
                    if (index >= shown.size()) {
                        return;
                    }
                    int cx = x + col * CELL;
                    int cy = y + row * CELL;
                    if (index == hovered) {
                        graphics.fill(cx, cy, cx + CELL, cy + CELL, YmStyle.withAlpha(style.accent, 0x70));
                    }
                    graphics.renderItem(shown.get(index).stack(), cx + 1, cy + 1);
                }
            }
            if (shown.isEmpty()) {
                style.drawText(graphics, font(), Component.translatable("ymducttape.kit.no_results"), x + 4,
                        y + 5, style.textDim);
            }
        }

        @Override
        public List<Component> getTooltip(double mouseX, double mouseY) {
            int index = indexAt(mouseX, mouseY);
            if (index < 0) {
                return List.of();
            }
            Entry entry = shown.get(index);
            return List.of(entry.stack().getHoverName(), Component.literal(entry.id()).withStyle(s -> s.withColor(0x808890)));
        }

        @Override
        public boolean mousePressed(double mouseX, double mouseY, int button) {
            int index = indexAt(mouseX, mouseY);
            if (index < 0 || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return false;
            }
            playClick();
            close();
            onPick.accept(shown.get(index).stack().copy());
            return true;
        }
    }
}
