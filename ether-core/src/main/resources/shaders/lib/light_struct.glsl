#ifndef LIGHT_STRUCT_GLSL
#define LIGHT_STRUCT_GLSL 1

struct Light {
	// trss.x = type (0 = off, 1 = directional, 2 = point, 3 = spot)
	// trss.y = range
	// trss.z = spot cos cutoff
	// trss.w = spot exponent
	vec4 trss;
	vec3 position;
	//float pad;
	vec3 ambientColor;
	//float pad;
	vec3 color;
	//float pad;
	vec3 spotDirection;
	//float pad;
};

#endif // LIGHT_STRUCT_GLSL
