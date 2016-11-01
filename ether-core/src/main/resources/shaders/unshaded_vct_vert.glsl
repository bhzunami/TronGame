#version 330

#include <view_block.glsl>

uniform bool useVertexColors;
uniform bool useTexture;

uniform vec4 materialColor;

in vec4 vertexPosition;
in vec4 vertexColor;
in vec2 vertexTexCoord;

out vec4 vsColor;
out vec2 vsTexCoord;

void main() {
	vsColor = materialColor;
	if (useVertexColors)
		vsColor *= vertexColor;
		
	if (useTexture)
		vsTexCoord = vertexTexCoord;
	gl_Position = view.viewProjMatrix * vertexPosition;
}
