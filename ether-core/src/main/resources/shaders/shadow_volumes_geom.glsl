#version 330

#define MAX_LIGHTS 8

#include <view_block.glsl>

#include <light_block.glsl>


struct VertexData {
	vec4 position;				// vertex position in eye space
};

uniform int lightIndex;
uniform float extrudeDistance;

in VertexData vd[3];

layout(triangles) in;
layout (triangle_strip, max_vertices=14) out;

void main() {
	Light light = lights[lightIndex];
	float lightType = light.trss.x;
	float lightRange = light.trss.y;

	vec4 t0 = vd[0].position;
	vec4 t1 = vd[1].position;
	vec4 t2 = vd[2].position;
	vec3 norm = cross((t1 - t0).xyz, (t2 - t0).xyz);

	vec4 u0;
	vec4 u1;
	vec4 u2;
	
	if (lightType == 1.0) {
		if (dot(light.position, norm) < 0)
			return;
		vec4 d = normalize(vec4(-light.position, 0));
		u0 = t0 + extrudeDistance * d;
		u1 = t1 + extrudeDistance * d;
		u2 = t2 + extrudeDistance * d;
	} else {
		if (dot(light.position - t0.xyz, norm) < 0)
			return;
		vec4 lp = vec4(light.position, 1);
		vec4 d0 = t0 - lp;
		vec4 d1 = t1 - lp;
		vec4 d2 = t2 - lp;
		float l0 = length(d0);
		float l1 = length(d1);
		float l2 = length(d2);
		if (l0 > lightRange && l1 > lightRange && l2 > lightRange)
			return;
		d0 /= l0;
		d1 /= l1;
		d2 /= l2;
		u0 = t0 + extrudeDistance * d0;
		u1 = t1 + extrudeDistance * d1;
		u2 = t2 + extrudeDistance * d2;
	}
	
	t0 = view.projMatrix * t0;
	t1 = view.projMatrix * t1;
	t2 = view.projMatrix * t2;
	u0 = view.projMatrix * u0;
	u1 = view.projMatrix * u1;
	u2 = view.projMatrix * u2;

	// volume with caps (triangle strip)

	// top
	gl_Position = t0;	EmitVertex();
	gl_Position = t1;	EmitVertex();
	gl_Position = t2;	EmitVertex();
	EndPrimitive();

	// bottom
	gl_Position = u0;	EmitVertex();
	gl_Position = u2;	EmitVertex();
	gl_Position = u1;	EmitVertex();
	EndPrimitive();

	// sides
	gl_Position = t0;	EmitVertex();
	gl_Position = u0;	EmitVertex();
	gl_Position = t1;	EmitVertex();
	gl_Position = u1;	EmitVertex();
	gl_Position = t2;	EmitVertex();
	gl_Position = u2;	EmitVertex();
	gl_Position = t0;	EmitVertex();
	gl_Position = u0;	EmitVertex();
	EndPrimitive();
}
