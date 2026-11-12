package com.yesmenn.ducttape.client.kit.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public field of a config class for {@link YmConfigScreen}. Supported types: boolean, int, long, float,
 * double, String and enums. The control follows the type: a switch for booleans, a slider for numbers with a range,
 * a number field without one, a color field for ints with {@link #color()}, segments or a dropdown for enums.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface YmOption {
    /** Sidebar tab; a translation key if it contains a dot, otherwise shown as written. */
    String category() default "General";

    /** Translation key of the label; empty = the field name in words ("maxDistance" -> "Max distance"). */
    String name() default "";

    /** Translation key (if it contains a dot) or text of the tooltip; empty = none. */
    String tooltip() default "";

    double min() default Double.NaN;

    double max() default Double.NaN;

    /** Slider step; 0 = 1 for whole numbers, 0.01 otherwise. */
    double step() default 0.0;

    /** For int fields: an RGB color (0xRRGGBB) edited with a color picker. */
    boolean color() default false;
}
