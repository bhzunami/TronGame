#version 330

struct VertexData {
	vec4 color;					// vertex diffuse color
	vec2 texCoord;				// texture coordinate of color map
};

uniform sampler2D colorMap;
uniform bool useColorMap;

in VertexData vd;

out vec4 fragColor;

void main() {
	vec4 scatteredLight = vd.color;

	if (useColorMap) {
		vec4 t = texture(colorMap, vd.texCoord);
		scatteredLight * t;
	}

	fragColor = scatteredLight;
}
