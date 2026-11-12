package com.yesmenn.ducttape.client.kit.module;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.element.YmExpander;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of {@link YmModule}s for one kind of object (e.g. all lamp settings of ymCL). Other mods can add modules
 * to the same set, e.g. as an add-on for their own lamp types. {@link #build} adds a collapsible block per applicable
 * module to a page.
 */
public final class YmModuleSet<T> {
    private final List<YmModule<T>> modules = new ArrayList<>();
    /** Open/closed per module ID; kept across screens so a block stays as the player left it. */
    private final Map<String, Boolean> open = new HashMap<>();

    public YmModuleSet<T> register(YmModule<T> module) {
        for (YmModule<T> existing : modules) {
            if (existing.id().equals(module.id())) {
                throw new IllegalArgumentException("Duplicate module ID: " + module.id());
            }
        }
        modules.add(module);
        modules.sort(Comparator.comparingInt(YmModule::order));
        return this;
    }

    public List<YmModule<T>> applicable(T target) {
        return modules.stream().filter(module -> module.appliesTo(target)).toList();
    }

    /** Adds one expander per applicable module to {@code page} (a column). */
    public void build(YmContainer page, T target) {
        for (YmModule<T> module : applicable(target)) {
            boolean isOpen = open.getOrDefault(module.id(), module.openByDefault());
            YmExpander expander = page.add(new YmExpander(module.title(), isOpen,
                    state -> open.put(module.id(), state)));
            expander.setIcon(module.icon()).setSummary(module.summary(target));
            module.build(expander.body(), target);
        }
    }
}
