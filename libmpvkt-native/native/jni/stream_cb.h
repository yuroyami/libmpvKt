#pragma once
#include <jni.h>
#include <mpv/client.h>

/* Called from JNI_OnLoad: caches the method ids of MpvStreamProvider and MpvStream. */
bool stream_cb_init(JavaVM *vm, JNIEnv *env);
/* mpv_stream_cb_add_ro with a Kotlin provider. Takes a global reference to it. */
int stream_cb_register(JNIEnv *env, mpv_handle *mpv, const char *protocol, jobject provider);
