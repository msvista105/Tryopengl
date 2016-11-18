#include <string>
#include <stdint.h>
#include "qtiblur.h"
#include "ProgramCache.h"

qtiblur::qtiblur(ProgramCache* cache)
    : mProgramCache(cache) {
}

qtiblur::~qtiblur() {
    if (mProgramCache != NULL) {
        delete mProgramCache;
    }
}

qtiblur* qtiblur::initBlurToken() {
    return new qtiblur(new ProgramCache());
}

void qtiblur::releaseBlurToken(void* obj) {
    qtiblur* thiz = static_cast<qtiblur*>(obj);
    if (thiz != NULL) {
        delete thiz;
    }
}

int qtiblur::blur(void* obj, int level,
                  unsigned int inId,  unsigned int  inWidth,   unsigned int  inHeight,
                  unsigned int outId, unsigned int* outWidth , unsigned int* outHeight) {
    qtiblur* thiz = static_cast<qtiblur*>(obj);
    if (thiz == NULL || thiz->mProgramCache == NULL) {
        return -1;
    }

    return thiz->mProgramCache->blurTexure(
            level, inId, inWidth, inHeight, outId, outWidth, outHeight);
}

