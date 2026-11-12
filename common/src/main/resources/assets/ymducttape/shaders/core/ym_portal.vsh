#version 150

// YM kit "portal" window body: passes the GUI position on to the fragment shader.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 guiPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    guiPos = Position.xy;
}
