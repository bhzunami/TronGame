#ifndef VIEW_BLOCK_GLSL
#define VIEW_BLOCK_GLSL 1

#include <view_struct.glsl>

layout (std140) uniform viewBlock {
	View view;
};

#endif // VIEW_BLOCK_GLSL
