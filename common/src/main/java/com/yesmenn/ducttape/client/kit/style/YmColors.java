package com.yesmenn.ducttape.client.kit.style;

/** RGB / HSV helpers. Colors are 0xRRGGBB, hue 0..360, saturation and value 0..1. */
public final class YmColors {
    private YmColors() {
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 60.0F;
        float c = value * saturation;
        float x = c * (1.0F - Math.abs(h % 2.0F - 1.0F));
        float m = value - c;
        float r;
        float g;
        float b;
        switch ((int) h) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return channel(r + m) << 16 | channel(g + m) << 8 | channel(b + m);
    }

    /** {hue, saturation, value}; the hue of grey is 0. */
    public static float[] rgbToHsv(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255.0F;
        float g = (rgb >> 8 & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0.0F;
        if (delta > 0.0F) {
            if (max == r) {
                hue = 60.0F * (((g - b) / delta) % 6.0F);
            } else if (max == g) {
                hue = 60.0F * ((b - r) / delta + 2.0F);
            } else {
                hue = 60.0F * ((r - g) / delta + 4.0F);
            }
        }
        if (hue < 0.0F) {
            hue += 360.0F;
        }
        return new float[]{hue, max == 0.0F ? 0.0F : delta / max, max};
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /** Parses "#RRGGBB" or "RRGGBB"; -1 if invalid. */
    public static int parseHex(String text) {
        String s = text.startsWith("#") ? text.substring(1) : text;
        if (s.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int channel(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255.0F)));
    }
}
