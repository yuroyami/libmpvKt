#include <jni.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <mpv/client.h>
#include <mpv/render.h>
#include <mpv/render_gl.h>
#include <condition_variable>
#include <mutex>
#include <pthread.h>

#define FN(name) JNIEXPORT JNICALL Java_io_github_yuroyami_libmpvkt_canvas_MpvRenderNative_##name
#define SLOTS 4

namespace {

typedef EGLClientBuffer (*PFN_getNativeClientBuffer)(const AHardwareBuffer*);
typedef EGLImageKHR (*PFN_createImage)(EGLDisplay, EGLContext, EGLenum, EGLClientBuffer, const EGLint*);
typedef EGLBoolean (*PFN_destroyImage)(EGLDisplay, EGLImageKHR);
typedef void (*PFN_imageTargetTexture)(GLenum, GLeglImageOES);

struct Slot {
    AHardwareBuffer* buffer = nullptr;
    EGLImageKHR image = EGL_NO_IMAGE_KHR;
    GLuint texture = 0;
    GLuint fbo = 0;
};

struct Renderer {
    mpv_handle* mpv = nullptr;
    mpv_render_context* rc = nullptr;
    EGLDisplay display = EGL_NO_DISPLAY;
    EGLContext context = EGL_NO_CONTEXT;
    EGLSurface pbuffer = EGL_NO_SURFACE;
    pthread_t owner{};
    PFN_getNativeClientBuffer getNativeClientBuffer = nullptr;
    PFN_createImage createImage = nullptr;
    PFN_destroyImage destroyImage = nullptr;
    PFN_imageTargetTexture imageTargetTexture = nullptr;
    Slot slots[SLOTS];
    int width = 0, height = 0;
    bool readback = false;
    std::mutex mutex;
    std::condition_variable updated;
    bool pending = false;
};

Renderer* get(jlong h) { return reinterpret_cast<Renderer*>(h); }

void throwRenderer(JNIEnv* env, const char* step) {
    jclass cls = env->FindClass("io/github/yuroyami/libmpvkt/canvas/MpvRendererException");
    if (cls) env->ThrowNew(cls, step);
}

bool onOwnerThread(JNIEnv* env, Renderer* r) {
    if (pthread_equal(pthread_self(), r->owner)) return true;
    throwRenderer(env, "called off the render thread");
    return false;
}

void onUpdate(void* ctx) {
    auto* r = static_cast<Renderer*>(ctx);
    { std::lock_guard<std::mutex> lock(r->mutex); r->pending = true; }
    r->updated.notify_one();
}

void* getProcAddress(void*, const char* name) { return reinterpret_cast<void*>(eglGetProcAddress(name)); }

void freeSlot(Renderer* r, Slot& s) {
    if (s.fbo) glDeleteFramebuffers(1, &s.fbo);
    if (s.texture) glDeleteTextures(1, &s.texture);
    if (s.image != EGL_NO_IMAGE_KHR) r->destroyImage(r->display, s.image);
    if (s.buffer) AHardwareBuffer_release(s.buffer);
    s = Slot{};
}

bool allocSlot(Renderer* r, Slot& s, int w, int h) {
    AHardwareBuffer_Desc desc{};
    desc.width = w; desc.height = h; desc.layers = 1;
    desc.format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
    desc.usage = AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT | AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE;
    if (r->readback) desc.usage |= AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN;
    if (AHardwareBuffer_allocate(&desc, &s.buffer) != 0) return false;
    EGLClientBuffer client = r->getNativeClientBuffer(s.buffer);
    const EGLint attrs[] = { EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE };
    s.image = r->createImage(r->display, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, client, attrs);
    if (s.image == EGL_NO_IMAGE_KHR) return false;
    glGenTextures(1, &s.texture);
    glBindTexture(GL_TEXTURE_2D, s.texture);
    r->imageTargetTexture(GL_TEXTURE_2D, s.image);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glGenFramebuffers(1, &s.fbo);
    glBindFramebuffer(GL_FRAMEBUFFER, s.fbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, s.texture, 0);
    bool ok = glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    return ok;
}

} // namespace

extern "C" {

jlong FN(create)(JNIEnv* env, jobject, jlong mpvHandle, jboolean readback) {
    auto* r = new Renderer();
    r->mpv = reinterpret_cast<mpv_handle*>(mpvHandle);
    r->owner = pthread_self();
    r->readback = readback;
    r->display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (r->display == EGL_NO_DISPLAY || !eglInitialize(r->display, nullptr, nullptr)) { delete r; throwRenderer(env, "eglInitialize"); return 0; }
    const EGLint configAttrs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_NONE };
    EGLConfig config; EGLint count = 0;
    if (!eglChooseConfig(r->display, configAttrs, &config, 1, &count) || count == 0) { delete r; throwRenderer(env, "eglChooseConfig"); return 0; }
    const EGLint pbufferAttrs[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
    r->pbuffer = eglCreatePbufferSurface(r->display, config, pbufferAttrs);
    const EGLint contextAttrs[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    r->context = eglCreateContext(r->display, config, EGL_NO_CONTEXT, contextAttrs);
    if (r->pbuffer == EGL_NO_SURFACE || r->context == EGL_NO_CONTEXT || !eglMakeCurrent(r->display, r->pbuffer, r->pbuffer, r->context)) {
        delete r; throwRenderer(env, "eglMakeCurrent"); return 0;
    }
    r->getNativeClientBuffer = (PFN_getNativeClientBuffer) eglGetProcAddress("eglGetNativeClientBufferANDROID");
    r->createImage = (PFN_createImage) eglGetProcAddress("eglCreateImageKHR");
    r->destroyImage = (PFN_destroyImage) eglGetProcAddress("eglDestroyImageKHR");
    r->imageTargetTexture = (PFN_imageTargetTexture) eglGetProcAddress("glEGLImageTargetTexture2DOES");
    if (!r->getNativeClientBuffer || !r->createImage || !r->destroyImage || !r->imageTargetTexture) { delete r; throwRenderer(env, "EGL image extensions"); return 0; }
    mpv_opengl_init_params glInit{};
    glInit.get_proc_address = getProcAddress;
    int advanced = 1;
    mpv_render_param params[] = {
        { MPV_RENDER_PARAM_API_TYPE, const_cast<char*>(MPV_RENDER_API_TYPE_OPENGL) },
        { MPV_RENDER_PARAM_OPENGL_INIT_PARAMS, &glInit },
        { MPV_RENDER_PARAM_ADVANCED_CONTROL, &advanced },
        { MPV_RENDER_PARAM_INVALID, nullptr },
    };
    int err = mpv_render_context_create(&r->rc, r->mpv, params);
    if (err < 0) { delete r; throwRenderer(env, mpv_error_string(err)); return 0; }
    mpv_render_context_set_update_callback(r->rc, onUpdate, r);
    return reinterpret_cast<jlong>(r);
}

void FN(resize)(JNIEnv* env, jobject, jlong h, jint width, jint height) {
    auto* r = get(h);
    if (!onOwnerThread(env, r)) return;
    for (auto& s : r->slots) freeSlot(r, s);
    r->width = width; r->height = height;
    if (width <= 0 || height <= 0) return;
    for (auto& s : r->slots) {
        if (!allocSlot(r, s, width, height)) { for (auto& t : r->slots) freeSlot(r, t); r->width = r->height = 0; throwRenderer(env, "AHardwareBuffer slot"); return; }
    }
}

jobject FN(slotBuffer)(JNIEnv* env, jobject, jlong h, jint slot) {
    auto* r = get(h);
    AHardwareBuffer* b = r->slots[slot].buffer;
    return b ? AHardwareBuffer_toHardwareBuffer(env, b) : nullptr;
}

jint FN(waitUpdate)(JNIEnv* env, jobject, jlong h, jint timeoutMs) {
    auto* r = get(h);
    if (!onOwnerThread(env, r)) return 0;
    {
        std::unique_lock<std::mutex> lock(r->mutex);
        r->updated.wait_for(lock, std::chrono::milliseconds(timeoutMs), [r] { return r->pending; });
        if (!r->pending) return 0;
        r->pending = false;
    }
    return (jint) mpv_render_context_update(r->rc);
}

jboolean FN(render)(JNIEnv* env, jobject, jlong h, jint slot) {
    auto* r = get(h);
    if (!onOwnerThread(env, r)) return JNI_FALSE;
    Slot& s = r->slots[slot];
    if (!s.fbo) return JNI_FALSE;
    mpv_opengl_fbo fbo{ (int) s.fbo, r->width, r->height, 0 };
    // No FLIP_Y: that suits a window with its origin at the bottom left, and Android reads these buffers top row first.
    mpv_render_param params[] = {
        { MPV_RENDER_PARAM_OPENGL_FBO, &fbo },
        { MPV_RENDER_PARAM_INVALID, nullptr },
    };
    mpv_render_context_render(r->rc, params);
    // HWUI samples the buffer without a fence; the write has to be done before Kotlin publishes the slot.
    glFinish();
    mpv_render_context_report_swap(r->rc);
    return JNI_TRUE;
}

void FN(readPixels)(JNIEnv* env, jobject, jlong h, jint slot, jobject dst) {
    auto* r = get(h);
    if (!onOwnerThread(env, r)) return;
    Slot& s = r->slots[slot];
    void* out = env->GetDirectBufferAddress(dst);
    if (!s.fbo || !out || env->GetDirectBufferCapacity(dst) < (jlong) r->width * r->height * 4) { throwRenderer(env, "readPixels buffer"); return; }
    glBindFramebuffer(GL_FRAMEBUFFER, s.fbo);
    glReadPixels(0, 0, r->width, r->height, GL_RGBA, GL_UNSIGNED_BYTE, out);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
}

void FN(wake)(JNIEnv*, jobject, jlong h) { onUpdate(get(h)); }

void FN(destroy)(JNIEnv* env, jobject, jlong h) {
    auto* r = get(h);
    if (!onOwnerThread(env, r)) return;
    mpv_render_context_set_update_callback(r->rc, nullptr, nullptr);
    mpv_render_context_free(r->rc);
    for (auto& s : r->slots) freeSlot(r, s);
    eglMakeCurrent(r->display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(r->display, r->context);
    eglDestroySurface(r->display, r->pbuffer);
    eglTerminate(r->display);
    delete r;
}

} // extern "C"
