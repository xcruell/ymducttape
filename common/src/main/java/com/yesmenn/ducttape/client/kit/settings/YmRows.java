package com.yesmenn.ducttape.client.kit.settings;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.element.YmFormRow;
import com.yesmenn.ducttape.client.kit.element.YmSegmented;
import com.yesmenn.ducttape.client.kit.element.YmSelect;
import com.yesmenn.ducttape.client.kit.element.YmSlider;
import com.yesmenn.ducttape.client.kit.element.YmSwitch;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Ready-made settings rows: label, control and the reset icon (shown while the value differs from its default). The
 * values are read and written through getter/setter, so they work with any config object.
 */
public final class YmRows {
    public static final int LABEL_WIDTH = 120;

    private YmRows() {
    }

    /** Decimal slider. {@code format} may be null (two decimals). */
    public static YmFormRow slider(YmContainer page, Component label, double min, double max, double step,
                                   DoubleSupplier get, DoubleConsumer set, double defaultValue,
                                   DoubleFunction<String> format) {
        YmSlider slider = new YmSlider(Component.literal(""), min, max, step, get.getAsDouble(), set);
        if (format != null) {
            slider.setFormat(format);
        }
        YmFormRow row = page.add(new YmFormRow(label, LABEL_WIDTH, slider));
        row.resettable(() -> Math.abs(get.getAsDouble() - defaultValue) > step / 2.0, () -> {
            set.accept(defaultValue);
            slider.setValue(defaultValue);
        });
        return row;
    }

    public static YmFormRow intSlider(YmContainer page, Component label, int min, int max, IntSupplier get,
                                      IntConsumer set, int defaultValue) {
        YmSlider slider = YmSlider.ofInt(Component.literal(""), min, max, get.getAsInt(), set);
        YmFormRow row = page.add(new YmFormRow(label, LABEL_WIDTH, slider));
        row.resettable(() -> get.getAsInt() != defaultValue, () -> {
            set.accept(defaultValue);
            slider.setValue(defaultValue);
        });
        return row;
    }

    public static YmFormRow toggle(YmContainer page, Component label, BooleanSupplier get, Consumer<Boolean> set,
                                   boolean defaultValue) {
        YmSwitch control = new YmSwitch(get.getAsBoolean(), set);
        YmFormRow row = page.add(new YmFormRow(label, LABEL_WIDTH, control));
        row.resettable(() -> get.getAsBoolean() != defaultValue, () -> {
            set.accept(defaultValue);
            control.setOn(defaultValue);
        });
        return row;
    }

    /** One of a few values: a segmented control for up to four, a dropdown for more. */
    public static <T> YmFormRow choice(YmContainer page, Component label, List<T> values, Supplier<T> get,
                                       Consumer<T> set, Function<T, Component> names, T defaultValue) {
        YmFormRow row;
        Runnable show;
        if (values.size() <= 4) {
            YmSegmented<T> control = new YmSegmented<>(values, get.get(), names, set);
            row = page.add(new YmFormRow(label, LABEL_WIDTH, control));
            show = () -> control.setValue(defaultValue);
        } else {
            YmSelect<T> control = new YmSelect<>(values, get.get(), names, set);
            row = page.add(new YmFormRow(label, LABEL_WIDTH, control));
            show = () -> control.setValue(defaultValue);
        }
        row.resettable(() -> !Objects.equals(get.get(), defaultValue), () -> {
            set.accept(defaultValue);
            show.run();
        });
        return row;
    }
}
