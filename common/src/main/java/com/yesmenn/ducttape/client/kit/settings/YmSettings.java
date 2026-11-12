package com.yesmenn.ducttape.client.kit.settings;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registry of the YM mods' settings. Every mod registers one {@link Section} (its pages, how to save, how to reset);
 * {@link #screen} shows one or several sections in a {@link YmSettingsScreen}. Mods that belong together (e.g. VILC and
 * Lights? YesMenn!) open a combined screen with all of their sections, without depending on each other.
 */
public final class YmSettings {
    private static final Map<String, Section> SECTIONS = new LinkedHashMap<>();

    private YmSettings() {
    }

    /** One page (sidebar tab) of a section. */
    public record Page(Component title, ItemStack icon, Consumer<YmContainer> builder) {
    }

    /** The settings of one mod. */
    public static final class Section {
        private final String id;
        private final Component name;
        private final List<Page> pages = new ArrayList<>();
        private int accent = YmStyle.ACCENT_OTHER;
        private Runnable save = () -> {
        };
        @Nullable
        private Runnable reset;
        @Nullable
        private Component credit;

        /**
         * @param id   usually the mod ID
         * @param name short name, shown before the page titles in a combined screen (e.g. "VILC")
         */
        public Section(String id, Component name) {
            this.id = id;
            this.name = name;
        }

        public Section page(Component title, ItemStack icon, Consumer<YmContainer> builder) {
            pages.add(new Page(title, icon, builder));
            return this;
        }

        public Section accent(int accent) {
            this.accent = accent;
            return this;
        }

        /** Writes the settings to disk; runs when the screen closes. */
        public Section onSave(Runnable save) {
            this.save = save;
            return this;
        }

        /** Puts all values back to their defaults (the "Reset" button, after a confirmation). */
        public Section onReset(@Nullable Runnable reset) {
            this.reset = reset;
            return this;
        }

        /** Small line in the bottom left corner of the screen (e.g. author and version). */
        public Section credit(@Nullable Component credit) {
            this.credit = credit;
            return this;
        }

        @Nullable
        public Component credit() {
            return credit;
        }

        public String id() {
            return id;
        }

        public Component name() {
            return name;
        }

        public List<Page> pages() {
            return List.copyOf(pages);
        }

        public int accent() {
            return accent;
        }

        void save() {
            save.run();
        }

        @Nullable
        Runnable reset() {
            return reset;
        }
    }

    /** Registers (or replaces) a section; call it once during client setup. */
    public static void register(Section section) {
        SECTIONS.put(section.id(), section);
    }

    public static boolean has(String id) {
        return SECTIONS.containsKey(id);
    }

    /** The registered sections among {@code ids}, in the given order. */
    public static List<Section> sections(String... ids) {
        List<Section> result = new ArrayList<>();
        for (String id : ids) {
            Section section = SECTIONS.get(id);
            if (section != null) {
                result.add(section);
            }
        }
        return result;
    }

    /**
     * Settings screen with every registered section among {@code ids} (missing ones are skipped, so an optional mod
     * just adds its pages when it is installed).
     */
    public static Screen screen(@Nullable Screen parent, Component title, String... ids) {
        return new YmSettingsScreen(title, parent, sections(ids));
    }
}
