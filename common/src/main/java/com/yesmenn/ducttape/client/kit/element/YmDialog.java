package com.yesmenn.ducttape.client.kit.element;

import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Modal dialog window (darkened screen behind it; only its buttons, the close button or Escape close it). Fill
 * {@link #content()} (a column) and {@link #footer()}, then {@link #open}; the height follows the content. Ready-made:
 * {@link #confirm} and {@link #prompt}.
 */
public class YmDialog extends YmWindow {
    public static final int DEFAULT_WIDTH = 220;

    public YmDialog(Component title) {
        this(title, DEFAULT_WIDTH);
    }

    public YmDialog(Component title, int width) {
        super(title, width, 100);
        content().setPadding(8, 8, 8, 8).setLayout(YmLayout.column(6));
        closable(this::close);
    }

    /** Opens the dialog centered on the screen. */
    public YmDialog open(YmScreen screen) {
        // Measure the content at the final width (wrapped text) to get the height.
        content().setBounds(1, HEADER_HEIGHT, width - 2, 1000);
        content().layout();
        int contentHeight = content().childrenHeight() + 16;
        footer();
        setSize(width, HEADER_HEIGHT + contentHeight + FOOTER_HEIGHT + 1);
        screen.openModal(this);
        return this;
    }

    public void close() {
        YmScreen screen = screen();
        if (screen != null) {
            screen.closePopup(this);
        }
    }

    /** Adds a footer button that runs {@code action} and closes the dialog. */
    public YmButton addButton(Component text, Runnable action) {
        return footer().add(new YmButton(text, button -> {
            close();
            action.run();
        }));
    }

    /** "Are you sure?" dialog: Cancel and a primary confirm button. */
    public static YmDialog confirm(YmScreen screen, Component title, Component message, Component confirmText,
                                   Runnable onConfirm) {
        YmDialog dialog = new YmDialog(title);
        dialog.content().add(new YmText(message).wrap());
        dialog.addButton(Component.translatable("gui.cancel"), () -> {
        });
        dialog.addButton(confirmText, onConfirm).primary();
        return dialog.open(screen);
    }

    /** Color picker dialog; OK reports the chosen color (0xRRGGBB). */
    public static YmDialog pickColor(YmScreen screen, Component title, int rgb, IntConsumer onOk) {
        YmDialog dialog = new YmDialog(title, 260);
        int[] chosen = {rgb};
        dialog.content().add(new YmColorPicker(rgb, color -> chosen[0] = color));
        dialog.addButton(Component.translatable("gui.cancel"), () -> {
        });
        dialog.addButton(Component.translatable("gui.ok"), () -> onOk.accept(chosen[0])).primary();
        return dialog.open(screen);
    }

    /** Asks for a text (e.g. a new name). Enter or OK confirms. */
    public static YmDialog prompt(YmScreen screen, Component title, Component label, String value,
                                  Consumer<String> onOk) {
        YmDialog dialog = new YmDialog(title);
        dialog.content().add(new YmText(label).dim());
        YmTextField field = dialog.content().add(new YmTextField(value, text -> {
        }));
        field.setOnEnter(text -> {
            dialog.close();
            onOk.accept(text);
        });
        dialog.addButton(Component.translatable("gui.cancel"), () -> {
        });
        dialog.addButton(Component.translatable("gui.ok"), () -> onOk.accept(field.getValue())).primary();
        dialog.open(screen);
        field.requestFocus();
        return dialog;
    }
}
