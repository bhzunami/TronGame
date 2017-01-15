#version 330

uniform sampler2D colorMap;
uniform sampler2D depthMap;

in vec2 vsTexCoord;

out vec4 fragColor;

vec4 blur13(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) {
  vec4 color = vec4(0.0);
  vec2 off1 = vec2(1.411764705882353) * direction;
  vec2 off2 = vec2(3.2941176470588234) * direction;
  vec2 off3 = vec2(5.176470588235294) * direction;
  color += texture(image, uv) * 0.1964825501511404;
  color += texture(image, uv + (off1 / resolution)) * 0.2969069646728344;
  color += texture(image, uv - (off1 / resolution)) * 0.2969069646728344;
  color += texture(image, uv + (off2 / resolution)) * 0.09447039785044732;
  color += texture(image, uv - (off2 / resolution)) * 0.09447039785044732;
  color += texture(image, uv + (off3 / resolution)) * 0.010381362401148057;
  color += texture(image, uv - (off3 / resolution)) * 0.010381362401148057;
  return color;
}

vec2 size = vec2(2000, 2000);
int samples = 5; // pixels per axis; higher = bigger glow, worse performance
float quality = 10; // lower = smaller glow, better quality

vec4 bloom(vec4 colour, sampler2D tex, vec2 tc)
{
  vec4 source = texture(tex, tc);
  vec4 sum = vec4(0);
  int diff = (samples - 1) / 2;
  vec2 sizeFactor = vec2(1) / size * quality;

  for (int x = -diff; x <= diff; x++)
  {
    for (int y = -diff; y <= diff; y++)
    {
      vec2 offset = vec2(x, y) * sizeFactor;
      sum += texture(tex, tc + offset);
    }
  }

  return ((sum / (samples * samples)) + source) * colour;
}

void main() {
//	fragColor = texture(colorMap, vsTexCoord);
	vec2 dim = textureSize(colorMap, 0);

	//fragColor = blur13(colorMap, vsTexCoord, dim, vec2(1, 1));


	fragColor = bloom(vec4(1), colorMap, vsTexCoord);
}
