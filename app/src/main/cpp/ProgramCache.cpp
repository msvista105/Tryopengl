#define LOG_TAG "libuiblur"

#include <string.h>
#include "ProgramCache.h"
#include "Program.h"

static const char* VertexShaderStr =
"precision mediump float;\n"
"//uniform mat4 u_ProjView;\n"
"attribute vec4 a_Position;\n"
"attribute vec2 a_TexCoordinate;\n"
"varying vec2 v_TexCoordinate;\n"
"void main() {\n"
"  // Set the position.\n"
"  //gl_Position = u_ProjView * a_Position;\n"
"  gl_Position = a_Position;\n"
"  // Pass through the texture coordinate.\n"
"  v_TexCoordinate = a_TexCoordinate;\n"
"}\n";

static const char* FragmentShaderStr =
"precision mediump float;\n"
"uniform sampler2D u_Texture;\n"
"uniform vec2 u_Scale;\n"
"varying vec2 v_TexCoordinate;\n"
"void main() {\n"
"  float weights[11];\n"
"  weights[0] = 0.047748641153356156;\n"
"  weights[1] = 0.05979670798364139;\n"
"  weights[2] = 0.07123260215138659;\n"
"  weights[3] = 0.08071711293576822;\n"
"  weights[4] = 0.08700369673862933;\n"
"  weights[5] = 0.08920620580763855;\n"
"  weights[6] = 0.08700369673862933;\n"
"  weights[7] = 0.08071711293576822;\n"
"  weights[8] = 0.07123260215138659;\n"
"  weights[9] = 0.05979670798364139;\n"
"  weights[10] = 0.047748641153356156;\n"
"  vec4 color = vec4(0.0, 0.0, 0.0, 0.0);\n"
"  for (int i = 0; i < 11; i++) {\n"
"    vec2 coords = v_TexCoordinate.xy + ((float(i) - 5.0) * u_Scale);\n"
"    color += texture2D(u_Texture, coords) * weights[i];\n"
"  }\n"
"  gl_FragColor = color;\n"
"  gl_FragColor.a = 1.0;\n"
"}\n";

static const float VERTEX[] = {   // in counterclockwise order:
    1.0f, 1.0f, 0.0f,  // top right
   -1.0f, 1.0f, 0.0f,  // top left
   -1.0f,-1.0f, 0.0f,  // bottom left
    1.0f,-1.0f, 0.0f,  // bottom right
};

static const short VERTEX_INDEX[] = { 0, 1, 2, 2, 0, 3 };
static const int VERTEX_INDEX_NUM = sizeof(VERTEX_INDEX)/sizeof(VERTEX_INDEX[0]);

static const float UV_TEX_VERTEX[] = {   // in clockwise order:
    1.0f, 0.0f,  // bottom right
    0.0f, 0.0f,  // bottom left
    0.0f, 1.0f,  // top left
    1.0f, 1.0f,  // top right
};

ProgramCache::ProgramCache()
    : mProgram(NULL)
    , mFboId(-1)
    , mTexture(-1)
    , mWidth(1)
    , mHeight(1) {
    setup();
}

ProgramCache::~ProgramCache() {
    if (mProgram != NULL) {
        delete mProgram;
    }

    if (mFboId != -1) {
        glDeleteFramebuffers(1, &mFboId);
    }

    if (mTexture != -1) {
        glDeleteTextures(1, &mTexture);
    }
}

std::string ProgramCache::generateVertexShader() {
    return std::string(VertexShaderStr);
}

std::string ProgramCache::generateFragmentShader() {
    return std::string(FragmentShaderStr);
}

Program* ProgramCache::generateProgram() {
    // vertex shader
    std::string vs = generateVertexShader();

    // fragment shader
    std::string fs = generateFragmentShader();

    Program* program = new Program(vs.c_str(), fs.c_str());
    return program;
}

int ProgramCache::setupFbo() {
    if (mFboId != -1)
        return 0;

    glGenFramebuffers(1, &mFboId);
    return 0;
}

int ProgramCache::setupTextures() {
    glGenTextures(1, &mTexture);
    glBindTexture(GL_TEXTURE_2D, mTexture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    //glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, mWidth, mHeight, 0,
    //        GL_RGB, GL_UNSIGNED_SHORT_5_6_5, NULL);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, mWidth, mHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
    return 0;
}

int ProgramCache::setup() {
    //create program
    mProgram = generateProgram();
    if (!mProgram->isValid()) {
        return -1;
    }

    //create fbo and textures
    setupFbo();
    setupTextures();

    //use program
    mProgram->use();

    return 0;
}

void ProgramCache::ensureTextures(int width, int height) {
    if (mWidth != width || mHeight != height) {
        //we need recreate the textures.
        mWidth = width;
        mHeight = height;

        glDeleteTextures(1, &mTexture);
        setupTextures();
    }
}

static const float LEVEL_MAX = 255.0f;
static const float BLUR_RADIUS_MAX = 10.0f;

float ProgramCache::getBlurXScale(uint8_t level) {
    return level * BLUR_RADIUS_MAX / LEVEL_MAX / mWidth;
}

float ProgramCache:: getBlurYScale(uint8_t level) {
    return level * BLUR_RADIUS_MAX / LEVEL_MAX / mHeight;
}

int ProgramCache::blurTexure(int level, GLuint inTexture, int inW, int inH,
        GLuint outTexture, unsigned int* outW, unsigned int* outH) {
    ensureTextures(inW, inH);
    float xscale = getBlurXScale(level);
    float yscale = getBlurYScale(level);

    //FIXME: ensure inTexture
    glBindTexture(GL_TEXTURE_2D,  inTexture);//mTexture
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    //FIXME: ensure outTexture exsit?
    glBindTexture(GL_TEXTURE_2D, outTexture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, mWidth, mHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);

    if (outW != NULL) *outW= inW;
    if (outH != NULL) *outH= inH;

    //save the origin fbo
    GLint savedFramebuffer = 0;
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &savedFramebuffer);

    // set up uniforms and attributes
    mProgram->use();
    GLuint program = mProgram->getSelf();
    GLint textureUniformHandle = glGetUniformLocation(program, "u_Texture");
    GLint scaleUniformHandle = glGetUniformLocation(program, "u_Scale");
    GLint positionHandle = glGetAttribLocation(program, "a_Position");
    GLint texCoordHandle = glGetAttribLocation(program, "a_TexCoordinate");
    //int projViewUniformHandle = glGetUniformLocation(program, "u_ProjView");
    glEnableVertexAttribArray(positionHandle);
    glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, VERTEX);
    glEnableVertexAttribArray(texCoordHandle);
    glVertexAttribPointer(texCoordHandle, 2, GL_FLOAT, false, 0, UV_TEX_VERTEX);

    glUniform1i(textureUniformHandle,  0);
    glBindFramebuffer(GL_FRAMEBUFFER, mFboId);

    //blur x
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTexture, 0);
    glBindTexture(GL_TEXTURE_2D, inTexture);

    glUniform2f(scaleUniformHandle, 0, yscale);
    //glDrawArrays(GL_TRIANGLES, 0, FBO_NUM_VERTICES);
    glDrawElements(GL_TRIANGLES, VERTEX_INDEX_NUM, GL_UNSIGNED_SHORT, VERTEX_INDEX);

    //blur y
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, outTexture, 0);
    glBindTexture(GL_TEXTURE_2D, mTexture);

    glUniform2f(scaleUniformHandle, xscale, 0);
    //glDrawArrays(GL_TRIANGLES, 0, FBO_NUM_VERTICES);
    glDrawElements(GL_TRIANGLES, VERTEX_INDEX_NUM, GL_UNSIGNED_SHORT, VERTEX_INDEX);

    //restore the origin fbo
    glBindFramebuffer(GL_FRAMEBUFFER, savedFramebuffer);

    //TODO: call glError()
    return 0;
}

