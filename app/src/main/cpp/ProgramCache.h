#pragma once

#include <GLES2/gl2.h>
#include <stdint.h>
#include <string>

class Program;

class ProgramCache {
public:
    ProgramCache();
    virtual ~ProgramCache();

    int blurTexure(int level, GLuint inTexture, int inW, int inH,
            GLuint outTexture, unsigned int* outW, unsigned int* outH);
private:
    // generates a program from the Key
    static Program* generateProgram(/*const Key& needs*/);
    // generates the vertex shader from the Key
    static std::string generateVertexShader(/*const Key& needs*/);
    // generates the fragment shader from the Key
    static std::string generateFragmentShader(/*const Key& needs*/);

    int setupTextures();
    int setupFbo();
    int setup();
    void ensureTextures(int width, int height);
    float getBlurXScale(uint8_t level);
    float getBlurYScale(uint8_t level);

    Program* mProgram;
    GLuint mFboId;
    GLuint mTexture;

    int mWidth;
    int mHeight;
};
