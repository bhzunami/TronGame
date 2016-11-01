#version 330

#define DIFFUSE_LAMBERT 1
//#define DIFFUSE_OREN_NAYAR 1

#define MAX_LIGHTS 8

#include <view_block.glsl>

#include <light_block.glsl>

#include <material_struct.glsl>

#include <diffuse_models.glsl>

struct VertexData {
	vec4 color;					// vertex diffuse color
	vec2 texCoord;				// texture coordinate of color map
};

uniform bool useColorMap;

uniform Material material;

in vec4 vertexPosition;
in vec4 vertexNormal;
in vec2 vertexTexCoord;

out VertexData vd;

void main() {
	vec3 emittedLight = material.emissionColor;
	vec3 scatteredLight = vec3(0);
	
	vec3 position = vec4(view.viewMatrix * vertexPosition).xyz;
	vec3 normal = normalize(view.normalMatrix * vertexNormal.xyz);

	for (int i = 0; i < MAX_LIGHTS; ++i) {
		float type = lights[i].trss.x;
		if (type == 0)
			continue;

		vec3 lightDirection;
		float attenuation;
	    // for local lights, compute per-vertex direction, and attenuation
		if (type > 1) {
			float range = lights[i].trss.y;
		
			lightDirection = -(position - lights[i].position);
			float lightDistance = length(lightDirection);
			
			lightDirection = lightDirection / lightDistance;
			attenuation = 1 - smoothstep(0, range, lightDistance);
	
			if (type > 2) {
				float spotCosCutoff = lights[i].trss.z;
				float spotExponent = lights[i].trss.w;

				float spotCos = dot(lightDirection, -normalize(lights[i].spotDirection)); 
				if (spotCos < spotCosCutoff)
	            	attenuation = 0.0;
				else
					attenuation *= pow(spotCos, spotExponent);
			}
		} else {
			lightDirection = lights[i].position;
			attenuation = 1.0;
		}

		float ndotl = dot(normal, lightDirection);
		float diffuseFactor = calculateDiffuseFactor(position, normal, lightDirection, ndotl);

		scatteredLight += material.ambientColor * lights[i].ambientColor * attenuation + material.diffuseColor * lights[i].color * diffuseFactor * attenuation;
	}

	vec4 rgba = vec4(min(emittedLight + scatteredLight, vec3(1.0)), material.alpha);
	vd.color = rgba;
	
	if (useColorMap)
		vd.texCoord = vertexTexCoord;

	gl_Position = view.viewProjMatrix * vertexPosition;
}
