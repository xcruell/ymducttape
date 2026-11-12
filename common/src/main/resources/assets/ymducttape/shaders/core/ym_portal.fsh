#version 150

// YM kit "portal" window body: a deep starfield made of several star layers over a dark sky. Near layers move more
// with the cursor than far ones (parallax), and all layers drift slowly.

uniform sampler2D Sampler0; // sky texture
uniform sampler2D Sampler1; // star texture
uniform float YmTime;       // seconds
uniform vec2 YmParallax;    // cursor offset from the body center, -1..1
uniform vec4 YmRect;        // body x, y, width, height in GUI units
uniform vec3 YmTintA;       // layer colors: each layer mixes these two
uniform vec3 YmTintB;
uniform vec4 ColorModulator; // screen fade (YmScreen#setFadeWhileDragging)

in vec2 guiPos;

out vec4 fragColor;

const int LAYERS = 10;
const float GOLDEN_ANGLE = 2.39996;
const float STAR_SCALE = 100.0; // GUI units per texture repeat, about the height of a main window

mat2 rotation(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c);
}

void main() {
    // Fixed scale in GUI units (not the window size), so small dialogs show the same star size as big windows.
    vec2 uv = (guiPos - YmRect.xy) / STAR_SCALE;

    vec3 color = texture(Sampler0, fract(uv * 0.6 + YmParallax * 0.01)).rgb * 0.16;
    for (int i = 0; i < LAYERS; i++) {
        float depth = float(i) / float(LAYERS - 1); // 0 = far, 1 = near
        float scale = mix(2.8, 0.75, depth);
        vec2 p = rotation(0.7 + float(i) * GOLDEN_ANGLE) * (uv * scale);
        p += vec2(YmTime * mix(0.003, 0.018, depth), YmTime * 0.002 * float(i % 3 + 1));
        p += YmParallax * mix(0.015, 0.16, depth);
        vec3 tint = mix(YmTintA, YmTintB, fract(float(i) * 0.618));
        color += texture(Sampler1, fract(p)).rgb * tint * mix(0.45, 1.0, depth);
    }
    // Slightly darker towards the edges, so header and footer bars stand out.
    vec2 centered = (guiPos - YmRect.xy) / YmRect.zw - 0.5;
    color *= 1.0 - dot(centered, centered) * 0.9;
    fragColor = vec4(color, 1.0) * ColorModulator;
}
