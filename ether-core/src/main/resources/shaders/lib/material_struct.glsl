#ifndef MATERIAL_STRUCT_GLSL
#define MATERIAL_STRUCT_GLSL 1

struct Material {
	vec3 emissionColor;
	vec3 ambientColor;
	vec3 diffuseColor;
	vec3 specularColor;
	float shininess;
	float strength;
	float alpha;
};

#endif // MATERIAL_STRUCT_GLSL
