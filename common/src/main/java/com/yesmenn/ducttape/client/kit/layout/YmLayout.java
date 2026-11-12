package com.yesmenn.ducttape.client.kit.layout;

import com.yesmenn.ducttape.client.kit.YmContainer;

/** Arranges the children of a panel. Called from {@link YmContainer#layout()} after the panel got its size. */
@FunctionalInterface
public interface YmLayout {
    void apply(YmContainer panel);

    /** Children below each other, each as wide as the panel's content area. */
    static YmLayout column(int gap) {
        return panel -> {
            int y = 0;
            for (var child : panel.children()) {
                if (!child.isVisible()) {
                    continue;
                }
                child.setBounds(0, y, panel.contentWidth(), child.getHeight());
                y += child.getHeight() + gap;
            }
        };
    }

    /** Children next to each other, sharing the content width equally, each as tall as the content area. */
    static YmLayout rowEqual(int gap) {
        return panel -> {
            var visible = panel.children().stream().filter(c -> c.isVisible()).toList();
            if (visible.isEmpty()) {
                return;
            }
            int total = panel.contentWidth() - gap * (visible.size() - 1);
            int x = 0;
            for (int i = 0; i < visible.size(); i++) {
                // Spread the rounding remainder over the first children.
                int w = total / visible.size() + (i < total % visible.size() ? 1 : 0);
                visible.get(i).setBounds(x, 0, w, panel.contentHeight());
                x += w + gap;
            }
        };
    }

    /** Children next to each other with their own widths, right-aligned, each as tall as the content area. */
    static YmLayout rowRight(int gap) {
        return panel -> {
            int x = panel.contentWidth();
            var children = panel.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                var child = children.get(i);
                if (!child.isVisible()) {
                    continue;
                }
                x -= child.getWidth();
                child.setBounds(x, 0, child.getWidth(), panel.contentHeight());
                x -= gap;
            }
        };
    }

    /** Fills the content area with the single (first) child. */
    static YmLayout fill() {
        return panel -> panel.children().stream().findFirst()
                .ifPresent(child -> child.setBounds(0, 0, panel.contentWidth(), panel.contentHeight()));
    }
}
