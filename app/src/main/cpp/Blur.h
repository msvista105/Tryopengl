#pragma once

#include <stdlib.h>
#include <stdint.h>
#include <sys/types.h>
//#include "error.h"
#include <utils/Errors.h>

class LayerBlur
{
public:

    LayerBlur(uint32_t w, uint32_t h);
    virtual ~LayerBlur();

    android::status_t blurTexture(int level, uint32_t inId, size_t inWidth, size_t inheight,
            uint32_t outId, size_t* outWidth, size_t* outHeight);
private:
    class BlurImpl {
    public:

        BlurImpl();
        ~BlurImpl();

        android::status_t blur(int level, uint32_t inId, size_t inWidth, size_t inheight,
                uint32_t outId, size_t* outWidth, size_t* outHeight);

    protected:
        static android::status_t initBlurImpl();
        static void closeBlurImpl();
        static void* sLibHandle;
        static bool sUnsupported;

        typedef void* (*initBlurTokenFn)();
        typedef void* (*releaseBlurTokenFn)(void*);
        typedef void* (*blurFn)(void*, int, uint32_t, size_t, size_t, uint32_t, size_t*, size_t*);

        static initBlurTokenFn initBlurToken;
        static releaseBlurTokenFn releaseBlurToken;
        static blurFn doBlur;

        //static Mutex sLock;

    private:
        void* mToken;
    };

    BlurImpl mBlurImpl;
    int mWidth;
    int mHeight;
};
