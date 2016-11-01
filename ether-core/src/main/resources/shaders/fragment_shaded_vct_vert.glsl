#version 330

#include <view_block.glsl>

struct VertexData {
	vec4 position;				// vertex position in eye space
	vec3 normal;				// vertex normal in eye space
	vec2 texCoord;				// texture coordinate of color map
};

uniform bool useColorMap;

in vec4 vertexPosition;
in vec4 vertexNormal;
in vec2 vertexTexCoord;

out VertexData vd;

void main() {
	vd.position = view.viewMatrix * vertexPosition;
	vd.normal = normalize(view.normalMatrix * vertexNormal.xyz);

	if (useColorMap)
		vd.texCoord = vertexTexCoord;

	gl_Position = view.viewProjMatrix * vertexPosition;
}
