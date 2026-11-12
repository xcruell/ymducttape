package com.yesmenn.ducttape.client.kit.settings;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.element.YmButton;
import com.yesmenn.ducttape.client.kit.element.YmDialog;
import com.yesmenn.ducttape.client.kit.element.YmSidebarTabs;
import com.yesmenn.ducttape.client.kit.element.YmWindow;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings of one or several mods ({@link YmSettings.Section}s): the pages of all sections in one sidebar (prefixed
 * with the section's name when there are several). Values change live; closing saves every section. "Reset" puts the
 * section of the open page back to its defaults after a confirmation.
 */
public class YmSettingsScreen extends YmScreen {
    private static final int SIDEBAR_WIDTH = 132;

    private final List<YmSettings.Section> sections;
    /** Section of each tab, by tab index. */
    private final List<YmSettings.Section> tabSections = new ArrayList<>();
    private int tab;

    public YmSettingsScreen(Component title, @Nullable Screen parent, List<YmSettings.Section> sections) {
        super(title, parent);
        this.sections = List.copyOf(sections);
        if (!this.sections.isEmpty()) {
            setAccent(this.sections.get(0).accent());
        }
    }

    @Override
    protected void build(YmContainer root) {
        int w = Math.min(480, width - 16);
        int h = Math.min(320, height - 16);
        YmWindow window = root.add(new YmWindow(getTitle(), w, h)).closable();
        window.centerIn(root);
        window.content().setPadding(4).setLayout(YmLayout.fill());

        YmSidebarTabs tabs = window.content().add(new YmSidebarTabs(SIDEBAR_WIDTH, tab, index -> {
            tab = index;
            // The footer's reset button belongs to the open page's section.
            rebuild();
        }));
        tabSections.clear();
        boolean several = sections.size() > 1;
        for (YmSettings.Section section : sections) {
            for (YmSettings.Page page : section.pages()) {
                Component title = several ? Component.empty().append(section.name()).append(": ").append(page.title())
                        : page.title();
                tabs.addTab(title, page.icon(), page.builder());
                tabSections.add(section);
            }
        }

        YmSettings.Section current = tab >= 0 && tab < tabSections.size() ? tabSections.get(tab) : null;
        if (current != null && current.reset() != null) {
            window.footer().add(new YmButton(Component.translatable("ymducttape.kit.settings.reset"), button ->
                    YmDialog.confirm(this, Component.translatable("ymducttape.kit.settings.reset"),
                            Component.translatable("ymducttape.kit.settings.reset.message", current.name()),
                            Component.translatable("ymducttape.kit.settings.reset"), () -> {
                                current.reset().run();
                                rebuild();
                            })));
        }
        window.footer().add(new YmButton(Component.translatable("gui.done"), button -> onClose()).primary());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // Credits of the sections, small in the bottom left corner (one line each, bottom up).
        float scale = 0.75F;
        int line = 0;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        for (int i = sections.size() - 1; i >= 0; i--) {
            Component credit = sections.get(i).credit();
            if (credit != null) {
                int y = Math.round((height - 8 - line * 9) / scale);
                graphics.drawString(font, credit, Math.round(6 / scale), y, 0x70FFFFFF, false);
                line++;
            }
        }
        graphics.pose().popPose();
    }

    @Override
    public void onClose() {
        sections.forEach(YmSettings.Section::save);
        super.onClose();
    }
}
