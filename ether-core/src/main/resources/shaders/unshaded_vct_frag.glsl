#version 330

uniform sampler2D colorMap;

uniform bool useTexture;

in vec4 vsColor;
in vec2 vsTexCoord;

out vec4 fragColor;

void main() {
	fragColor = useTexture ? vsColor * texture(colorMap, vsTexCoord) : vsColor;
}
