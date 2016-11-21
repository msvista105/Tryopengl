#define LOG_TAG "BLUR"

#include <dlfcn.h>
#include "Blur.h"
#include "log.h"
#include <GLES/gl.h>
#include <GLES2/gl2.h>

using namespace android;

// Automatically disables scissor test and restores it when destroyed
class ScopedScissorDisabler {
    bool scissorEnabled;
public:
    ScopedScissorDisabler(bool enabled) : scissorEnabled(enabled) {
        if(scissorEnabled) {
            glDisable(GL_SCISSOR_TEST);
        }
    }
    ~ScopedScissorDisabler() {
        if(scissorEnabled) {
            glEnable(GL_SCISSOR_TEST);
        }
    };
};


LayerBlur::LayerBlur(uint32_t w, uint32_t h)
    : mWidth(w)
    , mHeight(w) {
}

LayerBlur::~LayerBlur() {
}

status_t LayerBlur::blurTexture(int level, uint32_t inId, size_t inWidth, size_t inHeight,
            uint32_t outId, size_t* outWidth, size_t* outHeight) {
    /////
    // NOTE:
    //
    // Scissor test has been turned on by SurfaceFlinger for NON-primary display
    // We need to turn off the scissor test during our fbo drawing
    GLboolean isScissorEnabled = false;
    glGetBooleanv(GL_SCISSOR_TEST, &isScissorEnabled);
    ScopedScissorDisabler _(isScissorEnabled);
    //
    /////
    int ret = mBlurImpl.blur(level, inId, inWidth, inHeight, outId, outWidth, outHeight);
    return ret;
}

// ---------------------------------------------------------------------------

void* LayerBlur::BlurImpl::sLibHandle = NULL;
bool LayerBlur::BlurImpl::sUnsupported = false;

LayerBlur::BlurImpl::initBlurTokenFn LayerBlur::BlurImpl::initBlurToken = NULL;
LayerBlur::BlurImpl::releaseBlurTokenFn LayerBlur::BlurImpl::releaseBlurToken = NULL;
LayerBlur::BlurImpl::blurFn LayerBlur::BlurImpl::doBlur = NULL;
//Mutex LayerBlur::BlurImpl::sLock;

void LayerBlur::BlurImpl::closeBlurImpl() {
    if (sLibHandle != NULL) {
        dlclose(sLibHandle);
        sLibHandle = NULL;
    }
}

status_t LayerBlur::BlurImpl::initBlurImpl() {
    if (sLibHandle != NULL) {
        return OK;
    }
    if (sUnsupported) {
        return NO_INIT;
    }

    sLibHandle = dlopen("libuiblur.so", RTLD_NOW);
    if (sLibHandle == NULL) {
        sUnsupported = true;
        return NO_INIT;
    }

    // happy happy joy joy!

    initBlurToken = (initBlurTokenFn)dlsym(sLibHandle,
            "_ZN7qtiblur13initBlurTokenEv");
         // qtiblur::initBlurToken()
    releaseBlurToken = (releaseBlurTokenFn)dlsym(sLibHandle,
            "_ZN7qtiblur16releaseBlurTokenEPv");
         //qtiblur::releaseBlurToken(void*)

    if (sizeof(size_t) == 4) {
        LOGI("here, load 32bit so");
        doBlur = (blurFn)dlsym(sLibHandle,
                     "_ZN7qtiblur4blurEPvijjjjPjS1_");
        //qtiblur::blur(void*, int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int*, unsigned int*)
    } else if (sizeof(size_t) == 8) {
        LOGI("here, load 64bit so");
        doBlur = (blurFn)dlsym(sLibHandle,
                     "_ZN7qtiblur4blurEPvijmmjPmS1_");
        //qtiblur::blur(void*, int, unsigned int, unsigned long, unsigned long, unsigned int, unsigned long*, unsigned long*)
    }

    if (!initBlurToken || !releaseBlurToken || !doBlur) {
        LOGE("dlsym failed for blur impl!: %s", dlerror());
        closeBlurImpl();
        sUnsupported = true;
        return NO_INIT;
    }

    return OK;
}

LayerBlur::BlurImpl::BlurImpl() : mToken(NULL) {
//    Mutex::Autolock _l(sLock);
    if (initBlurImpl() == OK) {
        mToken = initBlurToken();
    }
}

LayerBlur::BlurImpl::~BlurImpl() {
//    Mutex::Autolock _l(sLock);
    if (mToken != NULL) {
        releaseBlurToken(mToken);
    }
}

status_t LayerBlur::BlurImpl::blur(int level, uint32_t inId, size_t inWidth, size_t inHeight,
        uint32_t outId, size_t* outWidth, size_t* outHeight) {
//    Mutex::Autolock _l(sLock);
    if (mToken == NULL) {
        return NO_INIT;
    }
    return doBlur(mToken, level, inId, inWidth, inHeight,
                  outId, outWidth, outHeight) ? OK : NO_INIT;
}

