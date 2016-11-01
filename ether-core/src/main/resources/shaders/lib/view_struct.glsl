#ifndef VIEW_STRUCT_GLSL
#define VIEW_STRUCT_GLSL 1

struct View {
	mat4 viewMatrix;
	mat4 viewProjMatrix;
	mat4 projMatrix;
	mat3 normalMatrix;
};

#endif // VIEW_STRUCT_GLSL
