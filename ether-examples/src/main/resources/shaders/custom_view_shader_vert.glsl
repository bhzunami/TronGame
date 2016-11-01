#version 330

in vec4 vertexPosition;

uniform mat4 mvp;

out vec4 vsColor;

void main() {
	vsColor = vec4(1, 0, 0, 1);
	gl_Position = mvp * vertexPosition;
}
