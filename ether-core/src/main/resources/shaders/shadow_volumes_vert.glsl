#version 330

#include <view_block.glsl>

struct VertexData {
	vec4 position;				// vertex position in eye space
};

in vec4 vertexPosition;

out VertexData vd;

void main() {
	vd.position = view.viewMatrix * vertexPosition;
	gl_Position = view.viewProjMatrix * vertexPosition;
}
