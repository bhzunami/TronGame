#ifndef DIFFUSE_MODELS_GLSL
#define DIFFUSE_MODELS_GLSL

//---- lambert diffuse factor

#ifdef DIFFUSE_LAMBERT

float calculateDiffuseFactor(vec3 position, vec3 normal, vec3 lightDirection, float ndotl) {
	return max(ndotl, 0.0);
}

#endif


//---- oren nayar diffuse factor (http://ruh.li/GraphicsOrenNayar.html)

#ifdef DIFFUSE_OREN_NAYAR

const float roughness = 0.5;

float calculateDiffuseFactor(vec3 position, vec3 normal, vec3 lightDirection, float ndotl) {
    vec3 v=normalize(position);

	float vdotn = dot(v, normal);
	float cos_theta_r = vdotn;
	float cos_theta_i = ndotl;
	float cos_phi_diff = dot(normalize(v - normal * vdotn), normalize(lightDirection - normal * ndotl));
	float cos_alpha = min(cos_theta_i, cos_theta_r);
	float cos_beta = max(cos_theta_i, cos_theta_r);

	float r2 = roughness * roughness;
	float a = 1.0 - 0.5 * r2 / (r2 + 0.33);
	float b_term;
	if (cos_phi_diff >= 0.0) {
		float b = 0.45 * r2 / (r2 + 0.09);
		b_term = b * sqrt((1.0 - cos_alpha * cos_alpha) * (1.0 - cos_beta * cos_beta)) / cos_beta * cos_phi_diff;
	} else {
		b_term = 0.0;
	}

	float diffuse = cos_theta_i * (a + b_term);
	return diffuse;
}
#endif

#endif // DIFFUSE_MODELS_GLSL
