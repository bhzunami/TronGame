#ifndef SPECULAR_MODELS_GLSL
#define SPECULAR_MODELS_GLSL

//---- blinn-phong specular factor
// modification: we multiply with diffuse factor in order not to get hard edges

#ifdef SPECULAR_BLINN_PHONG

float calculateSpecularFactor(vec3 position, vec3 normal, vec3 lightDirection, float ndotl, float shininess, float strength) {
	vec3 eyeDirection = -normalize(position);
	vec3 halfVector = normalize(lightDirection + eyeDirection); 
	return ndotl * pow(max(0.0, dot(normal, halfVector)), shininess) * strength;
}

#endif


//---- phong specular factor

#ifdef SPECULAR_PHONG

float calculateSpecularFactor(vec3 position, vec3 normal, vec3 lightDirection, float ndotl, float shininess, float strength) {
	vec3 eyeDirection = -normalize(position);
	vec3 reflectionVector = reflect(-lightDirection, normal);
	return pow(max(0.0, dot(reflectionVector, eyeDirection)), shininess) * strength;
}

#endif


//---- cook-torrance specular factor (http://ruh.li/GraphicsCookTorrance.html)

#ifdef SPECULAR_COOK_TORRANCE

const float roughness = 0.5;
const float fresnel = 0.8;

float calculateSpecularFactor(vec3 position, vec3 normal, vec3 lightDirection, float ndotl, float shininess, float strength) {
    vec3 eyeDirection = -normalize(position);
    vec3 halfVector = normalize(lightDirection + eyeDirection);

    // calculate intermediary values
    float ndotl = diffuse;
    float ndoth = max(dot(normal, halfVector), 0.0);
    float ndotv = max(dot(normal, eyeDirection), 0.0);
    float vdoth = max(dot(eyeDirection, halfVector), 0.0);
    float roughness2 = roughness * roughness;
    
    // geometric attenuation factor
    float g1 = 2.0 * ndoth * ndotv / vdoth;
    float g2 = 2.0 * ndoth * ndotl / vdoth;
    float attenuationFactor = min(1.0, min(g1, g2));
    
    // roughness factor (beckmann distribution function)
    float r1 = 1.0 / ( 4.0 * roughness2 * pow(ndoth, 4.0));
    float r2 = (ndoth * ndoth - 1.0) / (roughness2 * ndoth * ndoth);
    float roughnessFactor = r1 * exp(r2);
    
    // fresnel factor (schlick approximation)
    float fresnelFactor = pow(1.0 - vdoth, 5.0);
    fresnelFactor *= (1.0 - fresnel);
    fresnelFactor += fresnel;
    
    float specular = (roughnessFactor * attenuationFactor * fresnelFactor) / (ndotv * 3.14159265359);
    return specular;
}

#endif


#endif // SPECULAR_MODELS_GLSL
