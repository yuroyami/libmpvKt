#include "stream_cb.h"

#include <mpv/stream_cb.h>
#include <pthread.h>

#include <cstdint>
#include <cstring>

static JavaVM *g_vm;
static jmethodID m_provider_open;
static jmethodID m_stream_read, m_stream_seek, m_stream_size, m_stream_close, m_stream_cancel, m_stream_seekable;
static pthread_key_t g_detach_key;

/* A thread that attached itself detaches when it exits, through the key's destructor. */
static void detach_on_exit(void *) { g_vm->DetachCurrentThread(); }

static JNIEnv *env_for_this_thread() {
    JNIEnv *env = nullptr;
    if (g_vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK) return env;
    if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return nullptr;
    pthread_setspecific(g_detach_key, env);
    return env;
}

bool stream_cb_init(JavaVM *vm, JNIEnv *env) {
    g_vm = vm;
    pthread_key_create(&g_detach_key, detach_on_exit);
    jclass provider = env->FindClass("io/github/yuroyami/libmpvkt/stream/MpvStreamProvider");
    jclass stream = env->FindClass("io/github/yuroyami/libmpvkt/stream/MpvStream");
    if (!provider || !stream) return false;
    m_provider_open = env->GetMethodID(provider, "open", "(Ljava/lang/String;)Lio/github/yuroyami/libmpvkt/stream/MpvStream;");
    m_stream_read = env->GetMethodID(stream, "read", "(Ljava/nio/ByteBuffer;)I");
    m_stream_seek = env->GetMethodID(stream, "seek", "(J)J");
    m_stream_size = env->GetMethodID(stream, "size", "()J");
    m_stream_close = env->GetMethodID(stream, "close", "()V");
    m_stream_cancel = env->GetMethodID(stream, "cancel", "()V");
    m_stream_seekable = env->GetMethodID(stream, "isSeekable", "()Z");
    return m_provider_open && m_stream_read && m_stream_seek && m_stream_size && m_stream_close && m_stream_cancel && m_stream_seekable;
}

struct StreamCookie {
    jobject stream; // global reference
};

static int64_t cb_read(void *cookie, char *buf, uint64_t nbytes) {
    JNIEnv *env = env_for_this_thread();
    if (!env) return MPV_ERROR_GENERIC;
    jobject buffer = env->NewDirectByteBuffer(buf, (jlong) nbytes);
    if (!buffer) return MPV_ERROR_NOMEM;
    jint n = env->CallIntMethod(((StreamCookie *) cookie)->stream, m_stream_read, buffer);
    env->DeleteLocalRef(buffer);
    if (env->ExceptionCheck()) { env->ExceptionClear(); return MPV_ERROR_GENERIC; }
    return n;
}

static int64_t cb_seek(void *cookie, int64_t offset) {
    JNIEnv *env = env_for_this_thread();
    if (!env) return MPV_ERROR_GENERIC;
    jlong pos = env->CallLongMethod(((StreamCookie *) cookie)->stream, m_stream_seek, (jlong) offset);
    if (env->ExceptionCheck()) { env->ExceptionClear(); return MPV_ERROR_GENERIC; }
    return pos < 0 ? MPV_ERROR_UNSUPPORTED : pos;
}

static int64_t cb_size(void *cookie) {
    JNIEnv *env = env_for_this_thread();
    if (!env) return MPV_ERROR_UNSUPPORTED;
    jlong size = env->CallLongMethod(((StreamCookie *) cookie)->stream, m_stream_size);
    if (env->ExceptionCheck()) { env->ExceptionClear(); return MPV_ERROR_UNSUPPORTED; }
    return size < 0 ? MPV_ERROR_UNSUPPORTED : size;
}

static void cb_close(void *cookie) {
    StreamCookie *c = (StreamCookie *) cookie;
    JNIEnv *env = env_for_this_thread();
    if (env) {
        env->CallVoidMethod(c->stream, m_stream_close);
        if (env->ExceptionCheck()) env->ExceptionClear();
        env->DeleteGlobalRef(c->stream);
    }
    delete c;
}

static void cb_cancel(void *cookie) {
    JNIEnv *env = env_for_this_thread();
    if (!env) return;
    env->CallVoidMethod(((StreamCookie *) cookie)->stream, m_stream_cancel);
    if (env->ExceptionCheck()) env->ExceptionClear();
}

static int cb_open(void *user_data, char *uri, mpv_stream_cb_info *info) {
    JNIEnv *env = env_for_this_thread();
    if (!env) return MPV_ERROR_LOADING_FAILED;
    jstring juri = env->NewStringUTF(uri);
    jobject stream = env->CallObjectMethod((jobject) user_data, m_provider_open, juri);
    env->DeleteLocalRef(juri);
    if (env->ExceptionCheck()) { env->ExceptionClear(); return MPV_ERROR_LOADING_FAILED; }
    if (!stream) return MPV_ERROR_LOADING_FAILED;
    jboolean seekable = env->CallBooleanMethod(stream, m_stream_seekable);
    StreamCookie *c = new StreamCookie{env->NewGlobalRef(stream)};
    env->DeleteLocalRef(stream);
    info->cookie = c;
    info->read_fn = cb_read;
    info->seek_fn = seekable ? cb_seek : nullptr;
    info->size_fn = cb_size;
    info->close_fn = cb_close;
    info->cancel_fn = cb_cancel;
    return 0;
}

int stream_cb_register(JNIEnv *env, mpv_handle *mpv, const char *protocol, jobject provider) {
    jobject global = env->NewGlobalRef(provider);
    return mpv_stream_cb_add_ro(mpv, protocol, global, cb_open);
}
