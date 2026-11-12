package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Group title: brushed-metal text followed by a line that fades out to the right. */
public class YmHeading extends YmElement {
    private final Component title;

    public YmHeading(Component title) {
        super(0, 14);
        this.title = title;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        YmStyle style = style();
        int x = getX();
        int y = getY();
        int textY = y + (height - 8) / 2;
        style.drawMetalText(graphics, font(), title, x, textY);
        int lineX = x + font().width(title) + 6;
        int lineY = textY + 4;
        if (lineX < x + width) {
            style.horizontalGradient(graphics, lineX, lineY, x + width, lineY + 1, style.accentDim,
                    YmStyle.withAlpha(style.accentDim, 0));
        }
    }
}
