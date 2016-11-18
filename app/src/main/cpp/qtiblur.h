#pragma once

class ProgramCache;
class qtiblur {
public:
    static qtiblur* initBlurToken();
    static void releaseBlurToken(void* thiz); //qtiblur* thiz
    static int blur(void* thiz, int level,    //qtiblur* thiz
                    unsigned int inId,  unsigned int  inWidth,   unsigned int  inHeight,
                    unsigned int outId, unsigned int* outWidth , unsigned int* outHeight);
    virtual ~qtiblur();
private:
    ProgramCache* mProgramCache;
    qtiblur(ProgramCache* cache);
};
