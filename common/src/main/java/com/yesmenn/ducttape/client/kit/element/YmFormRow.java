package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * One line of a settings form: a label on the left (fixed width, so the controls of several rows line up) and one or
 * more controls on the right. With several controls, the first ones keep their width and the last one fills the rest.
 * {@link #resettable} adds a reset icon at the right end that shows only while the value differs from its default.
 */
public class YmFormRow extends YmContainer {
    public static final int DEFAULT_LABEL_WIDTH = 90;
    private static final int GAP = 4;

    private final YmText label;
    private final YmElement[] controls;
    private final int labelWidth;
    @Nullable
    private YmIconButton resetButton;
    @Nullable
    private BooleanSupplier changed;

    public YmFormRow(Component label, YmElement... controls) {
        this(label, DEFAULT_LABEL_WIDTH, controls);
    }

    public YmFormRow(Component label, int labelWidth, YmElement... controls) {
        super(0, 20);
        this.labelWidth = labelWidth;
        this.label = add(new YmText(label).dim());
        this.controls = controls;
        for (YmElement control : controls) {
            add(control);
            height = Math.max(height, control.getHeight());
        }
    }

    public YmText label() {
        return label;
    }

    /**
     * Adds the reset icon: visible while {@code isChanged} is true; a click runs {@code reset} (which sets the default
     * value; rebuild or update the control there so it shows the value).
     */
    public YmFormRow resettable(BooleanSupplier isChanged, Runnable reset) {
        this.changed = isChanged;
        if (resetButton == null) {
            resetButton = add(new YmIconButton(YmIconButton.Icon.RESET, button -> reset.run()));
            resetButton.setTooltip(Component.translatable("ymducttape.kit.reset"));
        }
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (resetButton != null && changed != null) {
            resetButton.setVisible(changed.getAsBoolean());
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void layout() {
        label.setBounds(0, 0, labelWidth, height);
        int x = labelWidth + GAP;
        // The reset icon keeps its place even while hidden, so the controls do not jump.
        int right = contentWidth() - (resetButton != null ? 12 + GAP : 0);
        if (resetButton != null) {
            resetButton.setBounds(contentWidth() - 12, (height - 12) / 2, 12, 12);
        }
        for (int i = 0; i < controls.length; i++) {
            YmElement control = controls[i];
            boolean last = i == controls.length - 1;
            int w = last ? Math.max(10, right - x) : control.getWidth();
            control.setBounds(x, (height - control.getHeight()) / 2, w, control.getHeight());
            x += w + GAP;
        }
        super.layout();
    }
}
