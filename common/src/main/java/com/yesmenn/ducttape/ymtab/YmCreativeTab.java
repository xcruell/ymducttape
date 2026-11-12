package com.yesmenn.ducttape.ymtab;

import com.yesmenn.ducttape.platform.Platform;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The shared "YesMenn!" creative tab of all YM mods. See {@code package-info.java} for how a mod takes part.
 * <p>
 * Registered once by YM Duct Tape and filled from the item tags {@code yesmenn:creative/<section>}, so a YM mod only
 * adds tag files.
 */
public final class YmCreativeTab {
    /** Mod property marking a mod that takes part in the shared tab (enables the untagged-items safety net). */
    public static final String MARKER = "yesmenn_tab";
    /** Shared namespace of the section tags, banner textures and translations. */
    public static final String NAMESPACE = "yesmenn";

    /** Sections in display order. */
    public enum Section {
        LIGHTS("lights", 0xFFE7A93C),
        TECHNICALS("technicals", 0xFF5C8FD6),
        DECOR("decor", 0xFF6FB04E),
        OTHER("other", 0xFF9A9A9A);

        private final String id;
        private final int fallbackColor;
        private final TagKey<Item> tag;

        Section(String id, int fallbackColor) {
            this.id = id;
            this.fallbackColor = fallbackColor;
            this.tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NAMESPACE, "creative/" + id));
        }

        public String id() {
            return id;
        }

        public TagKey<Item> tag() {
            return tag;
        }

        /** ARGB color of the plain banner drawn while no banner texture exists. */
        public int fallbackColor() {
            return fallbackColor;
        }

        public Component title() {
            return Component.translatable(NAMESPACE + ".creative_section." + id);
        }

        public ResourceLocation banner() {
            return ResourceLocation.fromNamespaceAndPath(NAMESPACE, "textures/gui/creative_sections/" + id + ".png");
        }

        /** First section whose tag holds the stack's item, or null. */
        @Nullable
        public static Section of(ItemStack stack) {
            for (Section section : values()) {
                if (stack.is(section.tag)) {
                    return section;
                }
            }
            return null;
        }
    }

    private static Supplier<CreativeModeTab> tab;

    private YmCreativeTab() {
    }

    /** Registers the shared tab. Called once from {@code YMDuctTape.init()}. */
    public static void register() {
        tab = Platform.register(BuiltInRegistries.CREATIVE_MODE_TAB, "yesmenn", () -> Platform.creativeTabBuilder()
                .title(Component.translatable("itemGroup." + NAMESPACE))
                .icon(YmCreativeTab::icon)
                .displayItems((parameters, output) -> fill(parameters.holders(), output))
                .build());
    }

    /** True for the shared tab. */
    public static boolean isTab(@Nullable CreativeModeTab creativeTab) {
        // Only asked from the creative screen, long after registration, so the tab is bound.
        return creativeTab != null && tab != null && tab.get() == creativeTab;
    }

    /**
     * The first item of the first non-empty section. Minecraft asks for the icon once, when the tab button is first
     * drawn in a world (tags are bound by then), and keeps it.
     */
    private static ItemStack icon() {
        for (Section section : Section.values()) {
            Optional<HolderSet.Named<Item>> tagged = BuiltInRegistries.ITEM.getTag(section.tag());
            if (tagged.isPresent() && tagged.get().size() > 0) {
                return new ItemStack(tagged.get().get(0));
            }
        }
        return new ItemStack(Items.PAPER);
    }

    private static void fill(HolderLookup.Provider holders, CreativeModeTab.Output output) {
        Optional<? extends HolderLookup.RegistryLookup<Item>> items = holders.lookup(Registries.ITEM);
        if (items.isEmpty()) {
            return;
        }
        // An item in several section tags shows up in the first one only (the tab refuses duplicates).
        Set<Item> added = new HashSet<>();
        Set<String> ymMods = new HashSet<>(Platform.modsWithProperty(MARKER));
        for (Section section : Section.values()) {
            Optional<HolderSet.Named<Item>> tagged = items.get().get(section.tag());
            if (tagged.isEmpty()) {
                continue;
            }
            for (Holder<Item> holder : tagged.get()) {
                if (added.add(holder.value())) {
                    output.accept(new ItemStack(holder));
                }
            }
        }
        // Safety net: items of YM mods that are in no section tag still show up (under Other, see the layout).
        items.get().listElements()
                .filter(holder -> ymMods.contains(holder.key().location().getNamespace()))
                .filter(holder -> added.add(holder.value()))
                .forEach(holder -> output.accept(new ItemStack(holder)));
    }

    /** Sections in display order, as a list. */
    public static List<Section> sections() {
        return List.of(Section.values());
    }
}
