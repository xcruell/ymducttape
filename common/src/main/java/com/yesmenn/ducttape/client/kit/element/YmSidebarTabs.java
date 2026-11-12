package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmScrollBox;
import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Sidebar with categories on the left and the selected category's page on the right (a scrolling column). Each tab
 * builds its page when selected, so pages always show current values.
 */
public class YmSidebarTabs extends YmContainer {
    public static final int TAB_HEIGHT = 20;

    private record Tab(Component title, ItemStack icon, Consumer<YmContainer> builder) {
    }

    private final List<Tab> tabs = new ArrayList<>();
    private final YmScrollBox sidebar = new YmScrollBox();
    private final YmScrollBox page = new YmScrollBox();
    private final int sidebarWidth;
    private final IntConsumer onSelect;
    private int selected;

    /**
     * @param sidebarWidth width of the category list
     * @param selected     initially selected tab (keep it in the screen to survive a resize)
     * @param onSelect     called with the new index when the user picks a tab
     */
    public YmSidebarTabs(int sidebarWidth, int selected, IntConsumer onSelect) {
        this.sidebarWidth = sidebarWidth;
        this.selected = selected;
        this.onSelect = onSelect;
        add(sidebar);
        add(page);
        // Scrolls when there are more tabs than fit; the bar then lies over the tabs' right edge.
        sidebar.setBackground(Background.INSET).setPadding(1).setLayout(YmLayout.column(0));
        page.setLayout(YmLayout.column(4));
        page.setPadding(6, 4, 6 + YmScrollBox.BAR_WIDTH + 2, 4);
    }

    public YmSidebarTabs addTab(Component title, Consumer<YmContainer> builder) {
        return addTab(title, ItemStack.EMPTY, builder);
    }

    public YmSidebarTabs addTab(Component title, ItemStack icon, Consumer<YmContainer> builder) {
        int index = tabs.size();
        tabs.add(new Tab(title, icon, builder));
        sidebar.add(new SidebarTab(index));
        if (index == selected) {
            buildPage();
        }
        return this;
    }

    public int getSelected() {
        return selected;
    }

    public void select(int index) {
        if (index < 0 || index >= tabs.size() || index == selected) {
            return;
        }
        selected = index;
        buildPage();
        page.layout();
        onSelect.accept(index);
    }

    /** The page panel of the selected tab (a column). */
    public YmScrollBox page() {
        return page;
    }

    private void buildPage() {
        page.clear();
        if (selected >= 0 && selected < tabs.size()) {
            tabs.get(selected).builder().accept(page);
        }
    }

    @Override
    public void layout() {
        sidebar.setBounds(0, 0, sidebarWidth, contentHeight());
        page.setBounds(sidebarWidth + 4, 0, contentWidth() - sidebarWidth - 4, contentHeight());
        super.layout();
        // After a (re)build, show the selected tab even if the list is scrolled.
        if (!shownSelected && selected >= 0 && selected < sidebar.children().size()) {
            sidebar.scrollTo(sidebar.children().get(selected));
            shownSelected = true;
        }
    }

    private boolean shownSelected;

    private final class SidebarTab extends YmElement {
        private final int index;

        private SidebarTab(int index) {
            super(0, TAB_HEIGHT);
            this.index = index;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            YmStyle style = style();
            Tab tab = tabs.get(index);
            int x = getX();
            int y = getY();
            boolean active = index == selected;
            boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
            if (active) {
                style.horizontalGradient(graphics, x, y, x + width, y + height,
                        YmStyle.withAlpha(style.accent, 0x60), YmStyle.withAlpha(style.accent, 0x10));
                graphics.fill(x, y, x + 2, y + height, style.accent);
            } else if (hovered) {
                graphics.fill(x, y, x + width, y + height, style.controlHover);
            }
            int textX = x + 6;
            if (!tab.icon().isEmpty()) {
                graphics.renderItem(tab.icon(), x + 4, y + (height - 16) / 2);
                textX = x + 23;
            }
            int color = active ? style.text : hovered ? style.text : style.textDim;
            Component shown = YmStyle.ellipsize(font(), tab.title(), x + width - textX - 3);
            style.drawText(graphics, font(), shown, textX, y + (height - 8) / 2, color);
        }

        @Override
        public boolean mousePressed(double mouseX, double mouseY, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return false;
            }
            if (index != selected) {
                playClick();
                select(index);
            }
            return true;
        }
    }
}
