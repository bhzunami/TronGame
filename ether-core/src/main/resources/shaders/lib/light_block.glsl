#ifndef LIGHT_BLOCK_GLSL
#define LIGHT_BLOCK_GLSL 1

#include <light_struct.glsl>

layout (std140) uniform lightBlock {
	Light lights[MAX_LIGHTS];
};

#endif // LIGHT_BLOCK_GLSL
