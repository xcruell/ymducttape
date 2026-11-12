package com.yesmenn.ducttape.client.kit.config;

import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.YmScrollBox;
import com.yesmenn.ducttape.client.kit.element.YmButton;
import com.yesmenn.ducttape.client.kit.element.YmColorSwatch;
import com.yesmenn.ducttape.client.kit.element.YmDialog;
import com.yesmenn.ducttape.client.kit.element.YmFormRow;
import com.yesmenn.ducttape.client.kit.element.YmNumberField;
import com.yesmenn.ducttape.client.kit.element.YmSegmented;
import com.yesmenn.ducttape.client.kit.element.YmSelect;
import com.yesmenn.ducttape.client.kit.element.YmSidebarTabs;
import com.yesmenn.ducttape.client.kit.element.YmSlider;
import com.yesmenn.ducttape.client.kit.element.YmSwitch;
import com.yesmenn.ducttape.client.kit.element.YmText;
import com.yesmenn.ducttape.client.kit.element.YmTextField;
import com.yesmenn.ducttape.client.kit.element.YmWindow;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmColors;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Settings screen built from a config object: every public field with {@link YmOption} becomes a row (label,
 * control, reset icon), grouped into sidebar tabs by {@link YmOption#category()}. Changes go into the object at once;
 * Save calls {@code onSave} (e.g. {@link YmConfigFile#save}), Cancel / Escape puts the old values back.
 */
public class YmConfigScreen extends YmScreen {
    private final Object config;
    private final Object defaults;
    private final Runnable onSave;
    private final Map<String, List<Field>> categories = new LinkedHashMap<>();
    private final Map<Field, Object> snapshot = new LinkedHashMap<>();
    private int tab;
    private boolean saved;

    /**
     * @param config the live config object (edited in place)
     * @param onSave called when the player saves
     */
    public YmConfigScreen(Component title, @Nullable Screen parent, Object config, Runnable onSave) {
        super(title, parent);
        this.config = config;
        this.defaults = YmConfigFile.newInstance(config.getClass());
        this.onSave = onSave;
        for (Field field : config.getClass().getFields()) {
            YmOption option = field.getAnnotation(YmOption.class);
            if (option == null || Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            categories.computeIfAbsent(option.category(), key -> new ArrayList<>()).add(field);
            snapshot.put(field, get(config, field));
        }
    }

    @Override
    protected void build(YmContainer root) {
        int w = Math.min(420, width - 20);
        int h = Math.min(280, height - 20);
        YmWindow window = root.add(new YmWindow(getTitle(), w, h)).closable();
        window.centerIn(root);
        window.content().setPadding(4).setLayout(YmLayout.fill());
        window.footer().add(new YmButton(Component.translatable("gui.cancel"), b -> onClose()));
        window.footer().add(new YmButton(Component.translatable("ymducttape.kit.save"), b -> {
            saved = true;
            onSave.run();
            onClose();
        }).primary());

        if (categories.size() <= 1) {
            YmScrollBox page = window.content().add(new YmScrollBox());
            page.setLayout(YmLayout.column(4));
            categories.values().forEach(fields -> buildPage(page, fields));
            return;
        }
        YmSidebarTabs tabs = window.content().add(new YmSidebarTabs(100, tab, index -> tab = index));
        categories.forEach((category, fields) -> tabs.addTab(text(category), page -> buildPage(page, fields)));
    }

    private void buildPage(YmContainer page, List<Field> fields) {
        for (Field field : fields) {
            YmOption option = field.getAnnotation(YmOption.class);
            Component label = option.name().isEmpty() ? Component.literal(words(field.getName()))
                    : Component.translatable(option.name());
            YmElement control = control(field, option);
            YmFormRow row = page.add(new YmFormRow(label, 110, control));
            row.resettable(() -> !Objects.equals(get(config, field), get(defaults, field)), () -> {
                set(field, get(defaults, field));
                rebuild();
            });
            if (!option.tooltip().isEmpty()) {
                row.label().setTooltip(text(option.tooltip()));
            }
        }
    }

    // ---- controls per type ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private YmElement control(Field field, YmOption option) {
        Class<?> type = field.getType();
        Object value = get(config, field);
        boolean ranged = !Double.isNaN(option.min()) && !Double.isNaN(option.max());

        if (type == boolean.class || type == Boolean.class) {
            return new YmSwitch((Boolean) value, v -> set(field, v));
        }
        if ((type == int.class || type == Integer.class) && option.color()) {
            return colorControl(field, (Integer) value);
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            boolean isLong = type == long.class || type == Long.class;
            long current = ((Number) value).longValue();
            if (ranged && option.max() - option.min() <= 10_000) {
                double step = option.step() > 0 ? option.step() : 1.0;
                return new YmSlider(Component.literal(""), option.min(), option.max(), step, current,
                        v -> set(field, isLong ? (Object) Math.round(v) : (Object) (int) Math.round(v)))
                        .setFormat(v -> Long.toString(Math.round(v)));
            }
            int min = ranged ? (int) option.min() : -1_000_000_000;
            int max = ranged ? (int) option.max() : 1_000_000_000;
            return new YmNumberField(min, max, (int) current,
                    v -> set(field, isLong ? (Object) (long) v : (Object) v)).setSize(80, 18);
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            boolean isFloat = type == float.class || type == Float.class;
            double current = ((Number) value).doubleValue();
            if (ranged) {
                double step = option.step() > 0 ? option.step() : 0.01;
                return new YmSlider(Component.literal(""), option.min(), option.max(), step, current,
                        v -> set(field, isFloat ? (Object) (float) v : (Object) v));
            }
            return new YmTextField(Double.toString(current), text -> {
                try {
                    double parsed = Double.parseDouble(text);
                    set(field, isFloat ? (Object) (float) parsed : (Object) parsed);
                } catch (NumberFormatException ignored) {
                    // Keep the last valid value while typing.
                }
            }).setFilter(text -> text.matches("-?\\d*\\.?\\d*"));
        }
        if (type == String.class) {
            return new YmTextField((String) value, text -> set(field, text));
        }
        if (type.isEnum()) {
            List<Enum> values = List.of((Enum[]) type.getEnumConstants());
            Enum current = (Enum) value;
            if (values.size() <= 4) {
                return new YmSegmented<Enum>(values, current, e -> Component.literal(words(e.name())),
                        e -> set(field, e));
            }
            return new YmSelect<Enum>(values, current, e -> Component.literal(words(e.name())), e -> set(field, e));
        }
        return new YmText(Component.literal("Unsupported type: " + type.getSimpleName())).dim();
    }

    private YmElement colorControl(Field field, int rgb) {
        YmContainer box = new YmContainer(0, 16);
        box.add(new YmColorSwatch(() -> (Integer) get(config, field), swatch ->
                YmDialog.pickColor(this, Component.translatable("ymducttape.kit.pick_color"),
                        (Integer) get(config, field), color -> set(field, color))).setBounds(0, 0, 40, 16));
        box.add(new YmText(() -> Component.literal(YmColors.toHex((Integer) get(config, field)))).dim()
                .setBounds(46, 0, 60, 16));
        return box;
    }

    // ---- closing ----

    @Override
    public void onClose() {
        if (!saved) {
            snapshot.forEach(this::set);
        }
        super.onClose();
    }

    // ---- reflection helpers ----

    private static Object get(Object target, Field field) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Config field not accessible: " + field, e);
        }
    }

    private void set(Field field, Object value) {
        try {
            field.set(config, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Config field not accessible: " + field, e);
        }
    }

    private static Component text(String keyOrText) {
        return keyOrText.contains(".") ? Component.translatable(keyOrText) : Component.literal(keyOrText);
    }

    /** "maxDistance" / "MAX_DISTANCE" -> "Max distance". */
    static String words(String name) {
        String spaced = name.contains("_") ? name.toLowerCase(Locale.ROOT).replace('_', ' ')
                : name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
