package com.yesmenn.ducttape.client.kit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.yesmenn.ducttape.YMDuctTape;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads and writes a config object as JSON (all its public fields). Missing or broken files give the defaults. */
public final class YmConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private YmConfigFile() {
    }

    /** Loads the file, or returns a new instance (defaults) if it is missing or unreadable. */
    public static <T> T load(Path path, Class<T> type) {
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                T loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException | JsonParseException e) {
                YMDuctTape.LOGGER.warn("Could not read {}, using defaults", path, e);
            }
        }
        return newInstance(type);
    }

    public static void save(Path path, Object config) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            YMDuctTape.LOGGER.warn("Could not write {}", path, e);
        }
    }

    /** New instance with the default values (the class needs a no-argument constructor). */
    public static <T> T newInstance(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new IllegalArgumentException("Config class needs a public no-argument constructor: " + type, e);
        }
    }
}
