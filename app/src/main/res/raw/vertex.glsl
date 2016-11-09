uniform mat4 u_ProjView;
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;
varying vec2 v_texCoord;
void main() {
  gl_Position = u_ProjView * a_Position;
  v_texCoord = a_TexCoordinate;
}