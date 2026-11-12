package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Item slot that shows the chosen item and its name; a click opens a {@link YmItemPicker}, a right click clears it
 * (if clearing is allowed).
 */
public class YmItemSlot extends YmElement {
    private final Component pickerTitle;
    private final Predicate<Item> filter;
    private final Consumer<ItemStack> onChange;
    private ItemStack stack;
    private boolean clearable = true;

    public YmItemSlot(Component pickerTitle, ItemStack stack, Predicate<Item> filter, Consumer<ItemStack> onChange) {
        super(120, 20);
        this.pickerTitle = pickerTitle;
        this.stack = stack;
        this.filter = filter;
        this.onChange = onChange;
    }

    public YmItemSlot setClearable(boolean clearable) {
        this.clearable = clearable;
        return this;
    }

    public ItemStack getStack() {
        return stack;
    }

    /** Sets the item without the change callback. */
    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        boolean hovered = isEnabled() && isMouseOver(mouseX, mouseY);
        style.drawControl(graphics, x, y, width, height, hovered, false, isEnabled(), false);
        style.drawInset(graphics, x + 2, y + (height - 18) / 2, 18, 18);
        Component name;
        int color;
        if (stack.isEmpty()) {
            name = Component.translatable("ymducttape.kit.choose_item");
            color = style.textDim;
        } else {
            graphics.renderItem(stack, x + 3, y + (height - 16) / 2);
            name = stack.getHoverName();
            color = isEnabled() ? style.text : style.textDisabled;
        }
        style.drawText(graphics, font(), YmStyle.ellipsize(font(), name, width - 30), x + 24, y + (height - 8) / 2,
                color);
    }

    @Override
    public List<Component> getTooltip(double mouseX, double mouseY) {
        List<Component> tooltip = super.getTooltip(mouseX, mouseY);
        if (tooltip.isEmpty() && clearable && !stack.isEmpty()) {
            return List.of(Component.translatable("ymducttape.kit.slot_hint"));
        }
        return tooltip;
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && clearable && !stack.isEmpty()) {
            playClick();
            stack = ItemStack.EMPTY;
            onChange.accept(stack);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        playClick();
        YmScreen screen = screen();
        if (screen != null) {
            YmItemPicker.open(screen, pickerTitle, filter, picked -> {
                stack = picked;
                onChange.accept(picked);
            });
        }
        return true;
    }
}
