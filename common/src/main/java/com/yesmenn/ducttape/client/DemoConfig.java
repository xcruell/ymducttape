package com.yesmenn.ducttape.client;

import com.yesmenn.ducttape.client.kit.config.YmConfigFile;
import com.yesmenn.ducttape.client.kit.config.YmOption;
import com.yesmenn.ducttape.platform.Platform;

import java.nio.file.Path;

/**
 * Example config for the generated config screen in the kit demo; stored in {@code config/ymducttape-demo.json}.
 * The field values are the defaults.
 */
public class DemoConfig {
    public enum Quality {
        LOW, MEDIUM, HIGH
    }

    public enum Shape {
        CONE, BEAM, SPHERE, RING, STAR
    }

    @YmOption(category = "ymducttape.demo.config.cat.light", name = "ymducttape.demo.config.enabled")
    public boolean enabled = true;

    @YmOption(category = "ymducttape.demo.config.cat.light", name = "ymducttape.demo.config.brightness",
            min = 0, max = 15)
    public int brightness = 12;

    @YmOption(category = "ymducttape.demo.config.cat.light", name = "ymducttape.demo.config.color", color = true)
    public int color = 0xFFAA33;

    @YmOption(category = "ymducttape.demo.config.cat.light", name = "ymducttape.demo.config.strength",
            min = 0, max = 2, step = 0.05, tooltip = "ymducttape.demo.config.strength.tooltip")
    public double strength = 1.0;

    @YmOption(category = "ymducttape.demo.config.cat.quality", name = "ymducttape.demo.config.quality")
    public Quality quality = Quality.MEDIUM;

    @YmOption(category = "ymducttape.demo.config.cat.quality", name = "ymducttape.demo.config.shape")
    public Shape shape = Shape.CONE;

    @YmOption(category = "ymducttape.demo.config.cat.quality", name = "ymducttape.demo.config.max_lights",
            min = 1, max = 100000)
    public int maxLights = 256;

    @YmOption(category = "ymducttape.demo.config.cat.other", name = "ymducttape.demo.config.name")
    public String name = "My lamp";

    @YmOption(category = "ymducttape.demo.config.cat.other", name = "ymducttape.demo.config.debug")
    public boolean debug;

    private static DemoConfig instance;

    private static Path path() {
        return Platform.configDir().resolve("ymducttape-demo.json");
    }

    public static DemoConfig get() {
        if (instance == null) {
            instance = YmConfigFile.load(path(), DemoConfig.class);
        }
        return instance;
    }

    public static void save() {
        YmConfigFile.save(path(), get());
    }
}
