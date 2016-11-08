precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D s_texture;
void main() {
    float[49] weights;
    weights[0] = 0.00000067;
    weights[1] = 0.00002292;
    weights[2] = 0.00019117;
    weights[3] = 0.00038771;
    weights[4] = 0.00019117;
    weights[5] = 0.00002292;
    weights[6] = 0.00000067;
    weights[7] = 0.00002292;
    weights[8] = 0.00078634;
    weights[9] = 0.00655965;
    weights[10] = 0.01330373;
    weights[11] = 0.00655965;
    weights[12] = 0.00078633;
    weights[13] = 0.00002292;
    weights[14] = 0.00019117;
    weights[15] = 0.00655965;
    weights[16] = 0.05472157;
    weights[17] = 0.11098164;
    weights[18] = 0.05472157;
    weights[19] = 0.00655965;
    weights[20] = 0.00019117;
    weights[21] = 0.00038771;
    weights[22] = 0.01330373;
    weights[23] = 0.11098164;
    weights[24] = 0.22508352;
    weights[25] = 0.11098164;
    weights[26] = 0.01330373;
    weights[27] = 0.00038771;
    weights[28] = 0.00019117;
    weights[29] = 0.00655965;
    weights[30] = 0.05472157;
    weights[31] = 0.11098164;
    weights[32] = 0.05472157;
    weights[33] = 0.00655965;
    weights[34] = 0.00019117;
    weights[35] = 0.00002292;
    weights[36] = 0.00078633;
    weights[37] = 0.00655965;
    weights[38] = 0.01330373;
    weights[39] = 0.00655965;
    weights[40] = 0.00078633;
    weights[41] = 0.00002292;
    weights[42] = 0.00000067;
    weights[43] = 0.00002292;
    weights[44] = 0.00019117;
    weights[45] = 0.00038771;
    weights[46] = 0.00019117;
    weights[47] = 0.00002292;
    weights[48] = 0.00000067;

  vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
  float hscale = 1.0 / 1328.0;
  float vscale = 1.0 / 2000.0;

  for (int i = 0; i < 49; i++) {
    vec2 coords = v_texCoord + vec2((float(i-i/7) - 3.0) * hscale, (float(i/7) - 3.0) * vscale);
    color += texture2D(s_texture, coords) * weights[i];
  }

  gl_FragColor = color;
}