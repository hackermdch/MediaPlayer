#include <jni.h>
#include "jnipp.h"

import Media;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
	jni::init(vm);
	JNIEnv* env = nullptr;
	if (vm->GetEnv((void**)&env, JNI_VERSION_21) != JNI_OK) return JNI_EVERSION;
	if (MediaPlayer::RegisterMethods(env) != JNI_OK) return JNI_ERR;
	return JNI_VERSION_21;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved)
{
}