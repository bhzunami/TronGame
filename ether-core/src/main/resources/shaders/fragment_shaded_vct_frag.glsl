#version 330

#define DIFFUSE_LAMBERT 1
//#define DIFFUSE_OREN_NAYAR 1

#define SPECULAR_BLINN_PHONG 1
//#define SPECULAR_PHONG 1
//#define SPECULAR_COOK_TORRANCE 1

#define MAX_LIGHTS 8

#include <light_block.glsl>

#include <material_struct.glsl>

#include <diffuse_models.glsl>
#include <specular_models.glsl>


struct VertexData {
	vec4 position;				// vertex position in eye space
	vec3 normal;				// vertex normal in eye space
	vec2 texCoord;				// texture coordinate of color map
};


uniform sampler2D colorMap;
uniform bool useColorMap;

uniform Material material;

in VertexData vd;

out vec4 fragColor;

// generic main function

void main() {
	vec3 emittedLight = material.emissionColor;
	vec3 scatteredLight = vec3(0);
    vec3 reflectedLight = vec3(0);

	vec3 position = vd.position.xyz;
	// FIXME haven't really studied the one / sided lighting issue yet.
	vec3 normal = normalize(gl_FrontFacing ? vd.normal : -vd.normal);

	for (int i = 0; i < MAX_LIGHTS; ++i) {
		float type = lights[i].trss.x;
		if (type == 0)
			continue;

		vec3 lightDirection;
		float attenuation;
	    // for local lights, compute per-fragment direction, and attenuation
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
		float specularFactor = diffuseFactor > 0.000001 ? calculateSpecularFactor(position, normal, lightDirection, ndotl, material.shininess, material.strength) : 0.0;

		scatteredLight += material.ambientColor * lights[i].ambientColor * attenuation + material.diffuseColor * lights[i].color * diffuseFactor * attenuation;
		reflectedLight += material.specularColor * lights[i].color * specularFactor * attenuation;
	}

	float alpha = material.alpha;

	if (useColorMap) {
		vec4 t = texture(colorMap, vd.texCoord);
		scatteredLight *= t.rgb;
		alpha *= t.a;
	}

	vec4 rgba = vec4(min(emittedLight + scatteredLight + reflectedLight, vec3(1.0)), alpha);

	fragColor = rgba;
}
