package com.yesmenn.ducttape.client.kit.module;

import com.yesmenn.ducttape.client.kit.YmContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * One building block of a modular settings page (e.g. "Color", "Redstone", "Startup flicker" of a lamp). A
 * {@link YmModuleSet} shows every module that {@link #appliesTo applies} to the edited object as its own collapsible
 * block, so a screen grows with the features of the object instead of being one fixed layout.
 *
 * @param <T> the edited object (e.g. a lamp's block entity or a description of its features)
 */
public interface YmModule<T> {
    /** Stable ID, used to remember whether the block is open. */
    String id();

    Component title();

    default ItemStack icon() {
        return ItemStack.EMPTY;
    }

    /** Whether this module is shown for the given object. */
    boolean appliesTo(T target);

    /** Adds the module's elements to {@code body} (a column). */
    void build(YmContainer body, T target);

    /** Short live text in the header (e.g. the current value), or null. */
    @Nullable
    default Supplier<Component> summary(T target) {
        return null;
    }

    default boolean openByDefault() {
        return false;
    }

    /** Position on the page; lower comes first. Base modules use 0-99, add-ons 100 and up. */
    default int order() {
        return 100;
    }
}
