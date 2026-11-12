package com.yesmenn.ducttape.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.config.YmConfigScreen;
import com.yesmenn.ducttape.client.kit.element.YmActionMenu;
import com.yesmenn.ducttape.client.kit.element.YmButton;
import com.yesmenn.ducttape.client.kit.element.YmColorPicker;
import com.yesmenn.ducttape.client.kit.element.YmColorSwatch;
import com.yesmenn.ducttape.client.kit.element.YmCurveEditor;
import com.yesmenn.ducttape.client.kit.element.YmCycler;
import com.yesmenn.ducttape.client.kit.element.YmDialog;
import com.yesmenn.ducttape.client.kit.element.YmExpander;
import com.yesmenn.ducttape.client.kit.element.YmFormRow;
import com.yesmenn.ducttape.client.kit.element.YmHeading;
import com.yesmenn.ducttape.client.kit.element.YmItemPicker;
import com.yesmenn.ducttape.client.kit.element.YmItemSlot;
import com.yesmenn.ducttape.client.kit.element.YmNumberField;
import com.yesmenn.ducttape.client.kit.element.YmPreview;
import com.yesmenn.ducttape.client.kit.element.YmSegmented;
import com.yesmenn.ducttape.client.kit.element.YmSelect;
import com.yesmenn.ducttape.client.kit.element.YmSidebarTabs;
import com.yesmenn.ducttape.client.kit.element.YmSlider;
import com.yesmenn.ducttape.client.kit.element.YmSwitch;
import com.yesmenn.ducttape.client.kit.element.YmText;
import com.yesmenn.ducttape.client.kit.element.YmTextField;
import com.yesmenn.ducttape.client.kit.element.YmTileGrid;
import com.yesmenn.ducttape.client.kit.element.YmToggle;
import com.yesmenn.ducttape.client.kit.element.YmWindow;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.module.YmModule;
import com.yesmenn.ducttape.client.kit.module.YmModuleSet;
import com.yesmenn.ducttape.client.kit.style.YmColors;
import com.yesmenn.ducttape.client.kit.style.YmKitSettings;
import com.yesmenn.ducttape.client.kit.style.YmPortalStyle;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Shows every element of the YM kit (opened from the mod list's config button). Only for trying the kit out; apart
 * from the kit settings (style, portal colors) and the demo config, nothing is saved.
 */
public class KitDemoScreen extends YmScreen {
    private enum Accent {
        TECHNICALS("technicals", YmStyle.ACCENT_TECHNICALS),
        LIGHTS("lights", YmStyle.ACCENT_LIGHTS),
        DECOR("decor", YmStyle.ACCENT_DECOR),
        OTHER("other", YmStyle.ACCENT_OTHER);

        final String key;
        final int color;

        Accent(String key, int color) {
            this.key = key;
            this.color = color;
        }
    }

    private enum Mode {
        OFF, ON, REDSTONE, INVERTED
    }

    private enum Quality {
        LOW, MEDIUM, HIGH, ULTRA
    }

    private record Preset(String name, int color) {
    }

    private static final int[] SWATCHES = {0xFFAA33, 0xFF4040, 0x40FF60, 0x4080FF, 0xFFFFFF, 0xC040FF, 0x40FFFF,
            0xFFE070};
    private static final int DEFAULT_BRIGHTNESS = 12;
    private static final Mode DEFAULT_MODE = Mode.REDSTONE;
    private static final boolean DEFAULT_POWER = true;

    // Demo state; lives in the screen so it survives the rebuild on resize.
    private int tab;
    private Accent accent = Accent.TECHNICALS;
    private boolean toggle = true;
    private boolean power = DEFAULT_POWER;
    private boolean sound;
    private Mode mode = DEFAULT_MODE;
    private Quality quality = Quality.MEDIUM;
    private int brightness = DEFAULT_BRIGHTNESS;
    private double strength = 0.8;
    private int radius = 8;
    private String name = "Lamp";
    private int red = 255;
    private int green = 170;
    private int blue = 51;
    private int swatchIndex = -1;
    private String lastAction = "-";
    private int pickedColor = 0x40A0FF;
    private boolean spotOpen;
    private boolean advancedOpen = true;
    private boolean tintAOpen;
    private boolean tintBOpen;
    private LampType lampType = LampType.SPOTLIGHT;
    private final Lamp lamp = new Lamp();
    private final List<Preset> presets = new ArrayList<>(List.of(new Preset("Warm", 0xFFAA33),
            new Preset("Cold", 0x9FD8FF), new Preset("Party", 0xC040FF), new Preset("Alarm", 0xFF3030)));
    private int selectedPreset;
    private ItemStack previewBlock = new ItemStack(Items.REDSTONE_LAMP);
    private boolean previewSpin = true;
    private ItemStack pickedItem = new ItemStack(Items.DIAMOND_PICKAXE);
    private ItemStack pickedBlock = ItemStack.EMPTY;
    private List<YmCurveEditor.Point> flicker = new ArrayList<>(List.of(new YmCurveEditor.Point(0.0F, 0.0F),
            new YmCurveEditor.Point(0.15F, 1.0F), new YmCurveEditor.Point(0.25F, 0.1F),
            new YmCurveEditor.Point(0.45F, 0.9F), new YmCurveEditor.Point(0.55F, 0.3F),
            new YmCurveEditor.Point(0.7F, 1.0F), new YmCurveEditor.Point(1.0F, 1.0F)));
    private YmCurveEditor.Mode flickerMode = YmCurveEditor.Mode.STEP;
    private int flickerMillis = 1500;

    /** What the demo lamp can do; a real mod would read this from the block. */
    private enum LampType {
        BASIC("basic", false, false),
        FLUORESCENT("fluorescent", true, false),
        SPOTLIGHT("spotlight", false, true),
        FLICKER_SPOT("flicker_spot", true, true);

        final String key;
        final boolean flicker;
        final boolean spot;

        LampType(String key, boolean flicker, boolean spot) {
            this.key = key;
            this.flicker = flicker;
            this.spot = spot;
        }
    }

    /** Settings of the demo lamp (shared by all lamp types). */
    private static final class Lamp {
        int color = 0xFFAA33;
        int brightness = 12;
        int radius = 10;
        Mode redstone = Mode.ON;
        boolean flicker = true;
        int flickerTicks = 20;
        int angle = 45;
        double focus = 0.6;
        boolean beam = true;
    }

    private interface PageBuilder {
        void build(YmContainer body, KitDemoScreen screen);
    }

    /**
     * Modules of the demo lamp screen: base modules (color, light, redstone) for every lamp, add-ons only for lamp
     * types that have the feature. A mod keeps such a set static and lets add-ons register more modules.
     */
    private static final YmModuleSet<KitDemoScreen> LAMP_MODULES = new YmModuleSet<KitDemoScreen>()
            .register(module("color", Items.RED_DYE, 0, true, s -> true,
                    (body, s) -> body.add(new YmColorPicker(s.lamp.color, c -> s.lamp.color = c)),
                    s -> () -> Component.literal(YmColors.toHex(s.lamp.color))))
            .register(module("light", Items.GLOWSTONE_DUST, 10, false, s -> true, (body, s) -> {
                body.add(YmSlider.ofInt(tr("brightness"), 0, 15, s.lamp.brightness, v -> s.lamp.brightness = v));
                body.add(YmSlider.ofInt(tr("radius"), 1, 32, s.lamp.radius, v -> s.lamp.radius = v));
            }, s -> () -> tr("module.light.summary", s.lamp.brightness, s.lamp.radius)))
            .register(module("redstone", Items.REDSTONE, 20, false, s -> true,
                    (body, s) -> body.add(new YmFormRow(tr("mode"),
                            new YmCycler<>(Component.empty(), List.of(Mode.values()), s.lamp.redstone,
                                    KitDemoScreen::modeName, m -> s.lamp.redstone = m))),
                    s -> () -> modeName(s.lamp.redstone)))
            .register(module("flicker", Items.LIGHTNING_ROD, 100, false, s -> s.lampType.flicker, (body, s) -> {
                body.add(new YmSwitch(tr("module.flicker.on"), s.lamp.flicker, v -> s.lamp.flicker = v));
                body.add(YmSlider.ofInt(tr("module.flicker.duration"), 0, 100, s.lamp.flickerTicks,
                        v -> s.lamp.flickerTicks = v));
            }, s -> () -> s.lamp.flicker ? tr("module.flicker.summary", s.lamp.flickerTicks) : tr("off")))
            .register(module("spot", Items.SPYGLASS, 110, false, s -> s.lampType.spot, (body, s) -> {
                body.add(YmSlider.ofInt(tr("cone_angle"), 5, 90, s.lamp.angle, v -> s.lamp.angle = v));
                body.add(new YmSlider(tr("focus"), 0.0, 1.0, 0.05, s.lamp.focus, v -> s.lamp.focus = v));
                body.add(new YmSwitch(tr("light_beam"), s.lamp.beam, v -> s.lamp.beam = v));
            }, s -> () -> Component.literal(s.lamp.angle + "°")));

    public KitDemoScreen(@Nullable Screen parent) {
        super(tr("title"), parent);
    }

    // ---- helpers ----

    private static Component tr(String key, Object... args) {
        return Component.translatable("ymducttape.demo." + key, args);
    }

    private static Component modeName(Mode mode) {
        return tr("mode." + mode.name().toLowerCase());
    }

    private static YmModule<KitDemoScreen> module(String id, Item icon, int order, boolean open,
                                                  Predicate<KitDemoScreen> applies, PageBuilder builder,
                                                  Function<KitDemoScreen, Supplier<Component>> summary) {
        return new YmModule<>() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Component title() {
                return tr("module." + id);
            }

            @Override
            public ItemStack icon() {
                return new ItemStack(icon);
            }

            @Override
            public boolean appliesTo(KitDemoScreen target) {
                return applies.test(target);
            }

            @Override
            public void build(YmContainer body, KitDemoScreen target) {
                builder.build(body, target);
            }

            @Override
            public Supplier<Component> summary(KitDemoScreen target) {
                return summary.apply(target);
            }

            @Override
            public boolean openByDefault() {
                return open;
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    private void applyStyle() {
        setStyle(YmKitSettings.createStyle(accent.color));
    }

    // ---- screen ----

    @Override
    protected void build(YmContainer root) {
        applyStyle();
        int w = Math.min(440, width - 20);
        int h = Math.min(290, height - 20);
        YmWindow window = root.add(new YmWindow(tr("window"), w, h)).closable();
        window.centerIn(root);
        window.content().setPadding(4).setLayout(YmLayout.fill());

        YmSidebarTabs tabs = window.content().add(new YmSidebarTabs(104, tab, index -> tab = index));
        tabs.addTab(tr("tab.controls"), new ItemStack(Items.LEVER), this::controlsPage);
        tabs.addTab(tr("tab.color"), new ItemStack(Items.RED_DYE), this::colorPage);
        tabs.addTab(tr("tab.text"), new ItemStack(Items.BOOK), this::textPage);
        tabs.addTab(tr("tab.list"), new ItemStack(Items.CHEST), this::listPage);
        tabs.addTab(tr("tab.picker"), new ItemStack(Items.MAGENTA_DYE), this::pickerPage);
        tabs.addTab(tr("tab.expandable"), new ItemStack(Items.SPYGLASS), this::expanderPage);
        tabs.addTab(tr("tab.lamp"), new ItemStack(Items.REDSTONE_LAMP), this::lampPage);
        tabs.addTab(tr("tab.style"), new ItemStack(Items.ENDER_EYE), this::stylePage);
        tabs.addTab(tr("tab.dialogs"), new ItemStack(Items.WRITABLE_BOOK), this::dialogsPage);
        tabs.addTab(tr("tab.segments"), new ItemStack(Items.COMPARATOR), this::segmentsPage);
        tabs.addTab(tr("tab.presets"), new ItemStack(Items.PAINTING), this::presetsPage);
        tabs.addTab(tr("tab.preview"), new ItemStack(Items.ARMOR_STAND), this::previewPage);
        tabs.addTab(tr("tab.items"), new ItemStack(Items.COMPASS), this::itemsPage);
        tabs.addTab(tr("tab.curve"), new ItemStack(Items.LIGHTNING_ROD), this::curvePage);
        tabs.addTab(tr("tab.config"), new ItemStack(Items.KNOWLEDGE_BOOK), this::configPage);
        tabs.addTab(tr("tab.config"), new ItemStack(Items.GLASS), this::emptyPage);



        window.footer().add(new YmButton(Component.translatable("gui.cancel"), b -> onClose()));
        window.footer().add(new YmButton(Component.translatable("gui.done"), b -> onClose()).primary());
    }

    // ---- pages ----

    private void controlsPage(YmContainer page) {
        page.add(new YmHeading(tr("controls.buttons")));
        page.add(new YmFormRow(tr("accent"),
                new YmCycler<>(Component.empty(), List.of(Accent.values()), accent,
                        a -> tr("accent." + a.key), a -> {
                    accent = a;
                    rebuild();
                })));
        page.add(new YmFormRow(tr("mode"),
                new YmCycler<>(Component.empty(), List.of(Mode.values()), mode, KitDemoScreen::modeName,
                        m -> mode = m)));
        page.add(new YmFormRow(tr("quality"),
                new YmSelect<>(List.of(Quality.values()), quality, q -> tr("quality." + q.name().toLowerCase()),
                        q -> quality = q)));
        page.add(new YmToggle(tr("enabled"), toggle, v -> toggle = v));
        page.add(new YmSwitch(tr("power"), power, v -> power = v));
        page.add(new YmFormRow(tr("sound"), new YmSwitch(sound, v -> sound = v)));

        page.add(new YmHeading(tr("controls.values")));
        page.add(YmSlider.ofInt(tr("brightness"), 0, 15, brightness, v -> brightness = v));
        page.add(new YmSlider(tr("strength"), 0.0, 1.0, 0.05, strength, v -> strength = v));
        page.add(new YmFormRow(tr("radius"), new YmNumberField(1, 64, radius, v -> radius = v)));
        page.add(new YmFormRow(tr("name"), new YmTextField(name, v -> name = v).setHint(tr("name.hint"))));
        YmButton disabled = new YmButton(tr("disabled_button"), b -> {
        });
        disabled.setEnabled(false);
        page.add(disabled);
    }

    private void colorPage(YmContainer page) {
        page.add(new YmHeading(tr("color.presets")));
        YmContainer swatches = page.add(new YmContainer(0, 18));
        swatches.setLayout(YmLayout.rowEqual(3));
        for (int i = 0; i < SWATCHES.length; i++) {
            int index = i;
            int rgb = SWATCHES[i];
            swatches.add(new YmColorSwatch(() -> rgb, s -> {
                swatchIndex = index;
                red = rgb >> 16 & 0xFF;
                green = rgb >> 8 & 0xFF;
                blue = rgb & 0xFF;
                rebuild();
            }).setSelected(index == swatchIndex));
        }
        page.add(new YmHeading(tr("color.custom")));
        page.add(new YmFormRow(tr("color.result"),
                new YmColorSwatch(() -> red << 16 | green << 8 | blue, null).setSize(40, 16),
                new YmText(() -> Component.literal(YmColors.toHex(red << 16 | green << 8 | blue))).dim()));
        page.add(YmSlider.ofInt(tr("color.red"), 0, 255, red, v -> {
            red = v;
            swatchIndex = -1;
        }));
        page.add(YmSlider.ofInt(tr("color.green"), 0, 255, green, v -> {
            green = v;
            swatchIndex = -1;
        }));
        page.add(YmSlider.ofInt(tr("color.blue"), 0, 255, blue, v -> {
            blue = v;
            swatchIndex = -1;
        }));
    }

    private void textPage(YmContainer page) {
        page.add(new YmHeading(tr("text.heading")));
        page.add(new YmText(tr("text.wrapped")).wrap());
        page.add(new YmText(tr("text.dim")).dim());
        page.add(new YmHeading(tr("text.menus")));
        page.add(new YmButton(tr("text.open_menu"), b -> {
        }) {
            @Override
            protected void onPress(int mouseButton) {
                playClick();
                new YmActionMenu()
                        .entry(tr("action.copy"), () -> lastAction = tr("action.copy").getString())
                        .entry(tr("action.paste"), () -> lastAction = tr("action.paste").getString())
                        .separator()
                        .entry(tr("action.unavailable"), false, false, () -> {
                        })
                        .entry(tr("action.reset"), () -> lastAction = tr("action.reset").getString())
                        .open(KitDemoScreen.this, getX(), getY() + getHeight());
            }
        });
        page.add(new YmText(() -> tr("text.last_action", lastAction)).dim());
        YmButton tooltip = new YmButton(tr("text.hover"), b -> {
        });
        tooltip.setTooltip(tr("text.tooltip1"), tr("text.tooltip2"));
        page.add(tooltip);
        page.add(new YmButton(tr("text.icon_button"), b -> {
        }).setIcon(new ItemStack(Items.GLOWSTONE)));
    }

    private void listPage(YmContainer page) {
        page.add(new YmHeading(tr("list.heading")));
        for (int i = 1; i <= 30; i++) {
            int row = i;
            page.add(new YmFormRow(tr("list.entry", row),
                    new YmButton(tr("list.action", row), b -> toast(tr("list.clicked", row)))));
        }
    }

    private void pickerPage(YmContainer page) {
        page.add(new YmHeading(tr("tab.picker")));
        page.add(new YmColorPicker(pickedColor, c -> pickedColor = c));
        page.add(new YmText(tr("picker.help")).wrap().dim());
        page.add(new YmFormRow(tr("picker.picked"),
                new YmColorSwatch(() -> pickedColor, null).setSize(40, 16),
                new YmText(() -> Component.literal(YmColors.toHex(pickedColor))).dim()));
    }

    private void expanderPage(YmContainer page) {
        page.add(new YmHeading(tr("tab.expandable")));
        page.add(new YmText(tr("expandable.help")).wrap().dim());
        YmExpander spot = page.add(new YmExpander(tr("expandable.spot"), spotOpen, open -> spotOpen = open));
        spot.setIcon(new ItemStack(Items.SPYGLASS)).setSummary(() -> tr("expandable.spot.summary", lamp.angle,
                Math.round(lamp.focus * 100)));
        spot.body().add(YmSlider.ofInt(tr("cone_angle"), 5, 90, lamp.angle, v -> lamp.angle = v));
        spot.body().add(new YmSlider(tr("focus"), 0.0, 1.0, 0.05, lamp.focus, v -> lamp.focus = v));
        spot.body().add(new YmSwitch(tr("light_beam"), lamp.beam, v -> lamp.beam = v));

        YmExpander advanced = page.add(new YmExpander(tr("expandable.advanced"), advancedOpen,
                open -> advancedOpen = open));
        advanced.body().add(new YmText(tr("expandable.advanced.text")).wrap());
        YmContainer buttons = advanced.body().add(new YmContainer(0, 20));
        buttons.setLayout(YmLayout.rowEqual(4));
        buttons.add(new YmButton(tr("action.reset"), b -> toast(tr("action.reset"))));
        buttons.add(new YmButton(tr("action.copy"), b -> toast(tr("action.copy"))));
        buttons.add(new YmButton(tr("action.paste"), b -> toast(tr("action.paste"))));
    }

    private void lampPage(YmContainer page) {
        page.add(new YmFormRow(tr("lamp.type"),
                new YmSelect<>(List.of(LampType.values()), lampType, t -> tr("lamp.type." + t.key), t -> {
                    lampType = t;
                    rebuild();
                })));
        page.add(new YmText(tr("lamp.help")).wrap().dim());
        LAMP_MODULES.build(page, this);
    }

    private void stylePage(YmContainer page) {
        page.add(new YmHeading(tr("style.heading")));
        page.add(new YmText(tr("style.help")).wrap());
        page.add(new YmFormRow(tr("style.style"),
                new YmSegmented<>(List.of(YmKitSettings.Style.values()), YmKitSettings.style(),
                        st -> tr("style." + st.name().toLowerCase()), st -> {
                    YmKitSettings.setStyle(st);
                    rebuild();
                })));
        page.add(new YmText(tr("style.saved")).wrap().dim());

        page.add(new YmHeading(tr("style.tints")));
        page.add(new YmText(tr("style.tints.help")).wrap().dim());
        YmExpander tintA = page.add(new YmExpander(tr("style.tint_a"), tintAOpen, open -> tintAOpen = open));
        tintA.setSummary(() -> Component.literal(YmColors.toHex(YmKitSettings.portalTintA())));
        tintA.body().add(new YmColorPicker(YmKitSettings.portalTintA(), c -> {
            YmKitSettings.setPortalTints(c, YmKitSettings.portalTintB());
            applyStyle();
        }));
        YmExpander tintB = page.add(new YmExpander(tr("style.tint_b"), tintBOpen, open -> tintBOpen = open));
        tintB.setSummary(() -> Component.literal(YmColors.toHex(YmKitSettings.portalTintB())));
        tintB.body().add(new YmColorPicker(YmKitSettings.portalTintB(), c -> {
            YmKitSettings.setPortalTints(YmKitSettings.portalTintA(), c);
            applyStyle();
        }));
        page.add(new YmButton(tr("style.default_tints"), b -> {
            YmKitSettings.setPortalTints(YmPortalStyle.DEFAULT_TINT_A, YmPortalStyle.DEFAULT_TINT_B);
            rebuild();
        }));
    }

    private void dialogsPage(YmContainer page) {
        page.add(new YmHeading(tr("dialogs.heading")));
        page.add(new YmText(tr("dialogs.help")).wrap().dim());
        page.add(new YmButton(tr("dialogs.confirm"), b -> YmDialog.confirm(this, tr("dialogs.confirm.title"),
                tr("dialogs.confirm.message"), tr("action.reset"), () -> toast(tr("dialogs.confirm.done")))));
        page.add(new YmButton(tr("dialogs.prompt"), b -> YmDialog.prompt(this, tr("dialogs.prompt.title"),
                tr("dialogs.prompt.label"), name, text -> {
                    name = text;
                    toast(tr("dialogs.prompt.done", text));
                })));
        page.add(new YmButton(tr("dialogs.color"), b -> YmDialog.pickColor(this, tr("dialogs.color"), pickedColor,
                c -> {
                    pickedColor = c;
                    toast(Component.literal(YmColors.toHex(c)), 0xFF000000 | c);
                })));
        page.add(new YmHeading(tr("dialogs.toasts")));
        YmContainer row = page.add(new YmContainer(0, 20));
        row.setLayout(YmLayout.rowEqual(4));
        row.add(new YmButton(tr("dialogs.toast_info"), b -> toast(tr("dialogs.toast_info.text"))));
        row.add(new YmButton(tr("dialogs.toast_error"), b -> toast(tr("dialogs.toast_error.text"), 0xFFE04040)));
    }

    private void segmentsPage(YmContainer page) {
        page.add(new YmHeading(tr("segments.heading")));
        page.add(new YmSegmented<>(List.of(Quality.values()), quality, q -> tr("quality." + q.name().toLowerCase()),
                q -> quality = q));
        page.add(new YmHeading(tr("segments.reset")));
        page.add(new YmText(tr("segments.reset.help")).wrap().dim());
        page.add(new YmFormRow(tr("brightness"),
                YmSlider.ofInt(Component.empty(), 0, 15, brightness, v -> brightness = v))
                .resettable(() -> brightness != DEFAULT_BRIGHTNESS, () -> {
                    brightness = DEFAULT_BRIGHTNESS;
                    rebuild();
                }));
        page.add(new YmFormRow(tr("mode"),
                new YmSegmented<>(List.of(Mode.values()), mode, KitDemoScreen::modeName, m -> mode = m))
                .resettable(() -> mode != DEFAULT_MODE, () -> {
                    mode = DEFAULT_MODE;
                    rebuild();
                }));
        page.add(new YmFormRow(tr("power"), new YmSwitch(power, v -> power = v))
                .resettable(() -> power != DEFAULT_POWER, () -> {
                    power = DEFAULT_POWER;
                    rebuild();
                }));
    }

    private void presetsPage(YmContainer page) {
        page.add(new YmHeading(tr("presets.heading")));
        page.add(new YmText(tr("presets.help")).wrap().dim());
        List<YmTileGrid.Tile> tiles = new ArrayList<>();
        for (Preset preset : presets) {
            tiles.add(YmTileGrid.Tile.color(Component.literal(preset.name()), preset.color()));
        }
        page.add(new YmTileGrid(tiles, selectedPreset, index -> {
            selectedPreset = index;
            toast(tr("presets.applied", presets.get(index).name()), 0xFF000000 | presets.get(index).color());
        }).setContextMenu((index, menu) -> menu
                .entry(tr("presets.rename"), () -> YmDialog.prompt(this, tr("presets.rename"), tr("presets.name"),
                        presets.get(index).name(), text -> {
                            presets.set(index, new Preset(text, presets.get(index).color()));
                            rebuild();
                        }))
                .entry(tr("presets.recolor"), () -> YmDialog.pickColor(this, tr("presets.recolor"),
                        presets.get(index).color(), c -> {
                            presets.set(index, new Preset(presets.get(index).name(), c));
                            rebuild();
                        }))
                .separator()
                .entry(tr("presets.delete"), () -> YmDialog.confirm(this, tr("presets.delete"),
                        tr("presets.delete.message", presets.get(index).name()), tr("presets.delete"), () -> {
                            presets.remove((int) index);
                            selectedPreset = Math.min(selectedPreset, presets.size() - 1);
                            rebuild();
                        })))
                .setOnAdd(() -> {
                    presets.add(new Preset(tr("presets.new", presets.size() + 1).getString(), pickedColor));
                    rebuild();
                }));
    }

    private void previewPage(YmContainer page) {
        page.add(new YmHeading(tr("preview.block")));
        page.add(new YmFormRow(tr("preview.block_choice"), new YmItemSlot(tr("preview.block_choice"), previewBlock,
                YmItemPicker.BLOCKS, stack -> previewBlock = stack.isEmpty() ? new ItemStack(Items.STONE) : stack)
                .setClearable(false)));
        page.add(new YmFormRow(tr("preview.spin"), new YmSwitch(previewSpin, v -> {
            previewSpin = v;
            rebuild();
        })));
        page.add(YmPreview.block(this::previewState).setAutoRotate(previewSpin).setSize(0, 110));
        page.add(new YmText(tr("preview.help")).wrap().dim());

        page.add(new YmHeading(tr("preview.custom")));
        page.add(new YmText(tr("preview.custom.help")).wrap().dim());
        YmContainer row = page.add(new YmContainer(0, 100));
        row.setLayout(YmLayout.rowEqual(6));
        row.add(YmPreview.item(() -> pickedItem.isEmpty() ? new ItemStack(Items.DIAMOND_PICKAXE) : pickedItem)
                .setAutoRotate(true, 45.0F));
        row.add(new YmPreview(KitDemoScreen::renderOrbit).setAutoRotate(true, 20.0F));
    }

    private BlockState previewState() {
        if (previewBlock.getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            // Show the redstone lamp lit, it looks nicer.
            return state.hasProperty(RedstoneLampBlock.LIT) ? state.setValue(RedstoneLampBlock.LIT, true) : state;
        }
        return Blocks.STONE.defaultBlockState();
    }

    /** Custom preview content: a lit lamp with small glowing cubes circling around it (like particles). */
    private static void renderOrbit(PoseStack pose, MultiBufferSource buffers, float partialTick) {
        var blocks = Minecraft.getInstance().getBlockRenderer();
        pose.pushPose();
        pose.translate(0.25F, 0.25F, 0.25F);
        pose.scale(0.5F, 0.5F, 0.5F);
        blocks.renderSingleBlock(Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true),
                pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        double time = Util.getMillis() / 1000.0;
        for (int i = 0; i < 8; i++) {
            double angle = time * 1.4 + i * Math.PI / 4.0;
            float px = 0.5F + (float) Math.cos(angle) * 0.42F;
            float pz = 0.5F + (float) Math.sin(angle) * 0.42F;
            float py = 0.5F + (float) Math.sin(time * 2.0 + i) * 0.22F;
            float size = 0.07F;
            pose.pushPose();
            pose.translate(px - size / 2, py - size / 2, pz - size / 2);
            pose.scale(size, size, size);
            blocks.renderSingleBlock(i % 2 == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
                            : Blocks.GLOWSTONE.defaultBlockState(), pose, buffers, 0xF000F0,
                    OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    private void itemsPage(YmContainer page) {
        page.add(new YmHeading(tr("items.heading")));
        page.add(new YmText(tr("items.help")).wrap().dim());
        page.add(new YmFormRow(tr("items.any"), new YmItemSlot(tr("items.pick_item"), pickedItem, YmItemPicker.ALL,
                stack -> pickedItem = stack)));
        page.add(new YmFormRow(tr("items.block"), new YmItemSlot(tr("items.pick_block"), pickedBlock,
                YmItemPicker.BLOCKS, stack -> pickedBlock = stack)));
    }

    private void curvePage(YmContainer page) {
        page.add(new YmHeading(tr("curve.heading")));
        page.add(new YmText(tr("curve.help")).wrap().dim());
        YmCurveEditor editor = new YmCurveEditor(flicker, flickerMode, points -> flicker = new ArrayList<>(points))
                .setPlayback(flickerMillis);
        page.add(new YmFormRow(tr("curve.mode"),
                new YmSegmented<>(List.of(YmCurveEditor.Mode.values()), flickerMode,
                        m -> tr("curve.mode." + m.name().toLowerCase()), m -> {
                    flickerMode = m;
                    editor.setMode(m);
                })));
        editor.setSize(0, 80);
        page.add(editor);
        page.add(YmSlider.ofInt(tr("curve.duration"), 200, 5000, flickerMillis, v -> {
            flickerMillis = v;
            editor.setPlayback(v);
        }));
        page.add(new YmFormRow(tr("curve.lamp"),
                new YmColorSwatch(() -> YmStyle.scale(0xFFAA33, editor.playheadValue()) & 0xFFFFFF, null)
                        .setSize(60, 16),
                new YmText(() -> Component.literal(Math.round(editor.playheadValue() * 100) + "%")).dim()));
    }

    private void configPage(YmContainer page) {
        page.add(new YmHeading(tr("config.heading")));
        page.add(new YmText(tr("config.help")).wrap().dim());
        page.add(new YmButton(tr("config.open"), b -> minecraft.setScreen(new YmConfigScreen(tr("config.title"),
                this, DemoConfig.get(), DemoConfig::save))).primary());
        page.add(new YmText(() -> tr("config.current", DemoConfig.get().brightness,
                YmColors.toHex(DemoConfig.get().color), DemoConfig.get().quality.name())).dim());
    }

    private void emptyPage(YmContainer page) {
        page.add(new YmHeading(tr("config.heading")));
    }
}
