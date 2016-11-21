#define LOG_TAG "BLUR"

#include "log.h"
#include "Blur.h"
#include <GLES/gl.h>
#include <GLES2/gl2.h>
#include <unistd.h>

#include <utils/Errors.h>

#include <gui/ISurfaceComposer.h>
#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>
#include <GLES/egl.h>
#include <GLES/glext.h>
#include <EGL/eglext.h>
#include <ui/DisplayInfo.h>
#include <private/gui/LayerState.h>

using namespace android;

typedef struct _PrivInfo
{
    int mWidth;
    int mHeight;
    EGLDisplay mDisplay;
    EGLContext mContext;
    EGLSurface mSurface;
    sp<Surface>                 mFlingerSurface;
    sp<SurfaceControl>          mFlingerSurfaceControl;
    sp<SurfaceComposerClient>   mSession;
} PrivInfo;
static int opengles_init(PrivInfo* priv) {
    bool bETC1Movie = true;

    priv->mSession = new SurfaceComposerClient();

    sp<IBinder> dtoken(SurfaceComposerClient::getBuiltInDisplay(
            ISurfaceComposer::eDisplayIdMain));
    DisplayInfo dinfo;
    status_t status = SurfaceComposerClient::getDisplayInfo(dtoken, &dinfo);
    if (status)
        return -1;
    /// M: The tablet rotation maybe 90/270 degrees, so set the lcm config for tablet
    SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientationDefault, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));

    // create the native surface
    sp<SurfaceControl> control = priv->mSession->createSurface(String8("BootAnimation"),
            dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);

    SurfaceComposerClient::openGlobalTransaction();
    control->setLayer(0x2000010);
    SurfaceComposerClient::closeGlobalTransaction();

    sp<Surface> s = control->getSurface();
/*  M: we do this if use android movie(),but ETC1Movie not use this @{

    // initialize opengl and egl
    const EGLint attribs[] = {
            EGL_RED_SIZE,   8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE,  8,
            EGL_DEPTH_SIZE, 0,
            EGL_NONE
    };
@}  */
    EGLint w, h;
    EGLint numConfigs;
    EGLConfig config;
    EGLSurface surface;
    EGLContext context;

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    LOGD("initialize opengl and egl");
    EGLBoolean eglret = eglInitialize(display, 0, 0);
    if (eglret == EGL_FALSE) {
        LOGE("eglInitialize(display, 0, 0) return EGL_FALSE");
    }
    if (!bETC1Movie) {
        const EGLint attribs[] = {
                EGL_RED_SIZE,   8,
                EGL_GREEN_SIZE, 8,
                EGL_BLUE_SIZE,  8,
                EGL_DEPTH_SIZE, 0,
                EGL_NONE
        };
        eglChooseConfig(display, attribs, &config, 1, &numConfigs);
        context = eglCreateContext(display, config, NULL, NULL);
     } else {
        const EGLint attribs[] = {
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE,   5,
            EGL_GREEN_SIZE, 6,
            EGL_BLUE_SIZE,  5,
            EGL_DEPTH_SIZE, 16,
            EGL_NONE
        };
        eglChooseConfig(display, attribs, &config, 1, &numConfigs);
        int attrib_list[] = {EGL_CONTEXT_CLIENT_VERSION, 2,
                       EGL_NONE, EGL_NONE};
        context = eglCreateContext(display, config, EGL_NO_CONTEXT, attrib_list);
    }

    surface = eglCreateWindowSurface(display, config, s.get(), NULL);
    eglret = eglQuerySurface(display, surface, EGL_WIDTH, &w);
    if (eglret == EGL_FALSE) {
        LOGE("eglQuerySurface(display, surface, EGL_WIDTH, &w) return EGL_FALSE");
    }
    eglret = eglQuerySurface(display, surface, EGL_HEIGHT, &h);
    if (eglret == EGL_FALSE) {
        LOGE("eglQuerySurface(display, surface, EGL_HEIGHT, &h) return EGL_FALSE");
    }

    if (eglMakeCurrent(display, surface, surface, context) == EGL_FALSE) {
        LOGE("eglMakeCurrent(display, surface, surface, context) return EGL_FALSE");
        return NO_INIT;
    }

    priv->mDisplay = display;
    priv->mContext = context;
    priv->mSurface = surface;
    priv->mWidth = w;
    priv->mHeight = h;
    priv->mFlingerSurfaceControl = control;
    priv->mFlingerSurface = s;
    return NO_ERROR;
}

int main() {
    PrivInfo pInfo;
    opengles_init(&pInfo);

    int level = 127;
    int inWidth = 720;
    int inheight = 1280;

    sleep(5);

    size_t outWidth = 0;
    size_t outHeight = 0;
    GLuint textures[2] = {0, 0};
    glGenTextures(2, textures);
    printf("glGetError = %x\n", glGetError());
    for (int i = 0; i < 2; i++) {
        printf("texture[%d] = [%x]\n", i, textures[i]);
        glBindTexture(GL_TEXTURE_2D, textures[i]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, inWidth, inheight, 0,
                GL_RGB, GL_UNSIGNED_SHORT_5_6_5, NULL);
    }
    GLuint inId = textures[0];
    GLuint outId = textures[1];

    LayerBlur* g_blur;
    g_blur = new LayerBlur(inWidth, inheight);

    for (;;) {
        g_blur->blurTexture(level, inId, inWidth, inheight, outId, &outWidth, &outHeight);
        sleep(1);
    }
    //int ret = 
    //printf("blurTexture level[%d], inId[%d], w[%d] h[%d] outid[%d], ret=%d outw[%d], outh[%d]\n",
    //         level, inId, inWidth, inheight, outId, ret, outWidth, outHeight);
    return 0;
}
