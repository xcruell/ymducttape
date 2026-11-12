package com.yesmenn.ducttape.client.kit.style;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.yesmenn.ducttape.YMDuctTape;
import com.yesmenn.ducttape.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Player's YM kit settings (which style all YM screens use, portal layer colors), stored in
 * {@code config/ymducttape-client.json}. Screens get their style from {@link #createStyle(int)} with their own accent.
 * Changes are kept in memory at once and written by {@link #save()} (e.g. when a settings screen closes).
 */
public final class YmKitSettings {
    public enum Style {
        METAL, PORTAL
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "ymducttape-client.json";

    /** Stored fields (Gson). */
    private static final class Data {
        Style style = Style.METAL;
        String portalTintA = YmColors.toHex(YmPortalStyle.DEFAULT_TINT_A);
        String portalTintB = YmColors.toHex(YmPortalStyle.DEFAULT_TINT_B);
    }

    private static Data data;
    private static boolean dirty;

    private YmKitSettings() {
    }

    private static Data data() {
        if (data == null) {
            data = load();
        }
        return data;
    }

    // ---- values ----

    public static Style style() {
        return data().style == null ? Style.METAL : data().style;
    }

    public static void setStyle(Style style) {
        data().style = style;
        dirty = true;
    }

    public static int portalTintA() {
        return colorOr(data().portalTintA, YmPortalStyle.DEFAULT_TINT_A);
    }

    public static int portalTintB() {
        return colorOr(data().portalTintB, YmPortalStyle.DEFAULT_TINT_B);
    }

    public static void setPortalTints(int tintA, int tintB) {
        data().portalTintA = YmColors.toHex(tintA);
        data().portalTintB = YmColors.toHex(tintB);
        dirty = true;
    }

    /** The player's style with the given accent color (each screen keeps its own accent). */
    public static YmStyle createStyle(int accent) {
        return switch (style()) {
            case PORTAL -> new YmPortalStyle(accent, portalTintA(), portalTintB());
            case METAL -> YmStyle.DEFAULT.withAccent(accent);
        };
    }

    private static int colorOr(String hex, int fallback) {
        int rgb = hex == null ? -1 : YmColors.parseHex(hex);
        return rgb < 0 ? fallback : rgb;
    }

    // ---- file ----

    private static Path path() {
        return Platform.configDir().resolve(FILE);
    }

    private static Data load() {
        Path path = path();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException | JsonParseException e) {
                YMDuctTape.LOGGER.warn("Could not read {}, using defaults", path, e);
            }
        }
        return new Data();
    }

    /** Writes the settings if something changed since the last save. */
    public static void save() {
        if (!dirty) {
            return;
        }
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(data(), writer);
            }
            dirty = false;
        } catch (IOException e) {
            YMDuctTape.LOGGER.warn("Could not write {}", path, e);
        }
    }
}
