/**
 * SPDX-License-Identifier: (WTFPL OR CC0-1.0) AND Apache-2.0
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "wgl.h"

#ifndef GLAD_IMPL_UTIL_C_
#define GLAD_IMPL_UTIL_C_

#ifdef _MSC_VER
#define GLAD_IMPL_UTIL_SSCANF sscanf_s
#else
#define GLAD_IMPL_UTIL_SSCANF sscanf
#endif

#endif /* GLAD_IMPL_UTIL_C_ */

#ifdef __cplusplus
extern "C" {
#endif



int GLAD_WGL_VERSION_1_0 = 0;
int GLAD_WGL_ARB_extensions_string = 0;
int GLAD_WGL_EXT_extensions_string = 0;
int GLAD_WGL_NV_DX_interop = 0;
int GLAD_WGL_NV_DX_interop2 = 0;



PFNWGLDXCLOSEDEVICENVPROC glad_wglDXCloseDeviceNV = NULL;
PFNWGLDXLOCKOBJECTSNVPROC glad_wglDXLockObjectsNV = NULL;
PFNWGLDXOBJECTACCESSNVPROC glad_wglDXObjectAccessNV = NULL;
PFNWGLDXOPENDEVICENVPROC glad_wglDXOpenDeviceNV = NULL;
PFNWGLDXREGISTEROBJECTNVPROC glad_wglDXRegisterObjectNV = NULL;
PFNWGLDXSETRESOURCESHAREHANDLENVPROC glad_wglDXSetResourceShareHandleNV = NULL;
PFNWGLDXUNLOCKOBJECTSNVPROC glad_wglDXUnlockObjectsNV = NULL;
PFNWGLDXUNREGISTEROBJECTNVPROC glad_wglDXUnregisterObjectNV = NULL;
PFNWGLGETEXTENSIONSSTRINGARBPROC glad_wglGetExtensionsStringARB = NULL;
PFNWGLGETEXTENSIONSSTRINGEXTPROC glad_wglGetExtensionsStringEXT = NULL;


static void glad_wgl_load_WGL_ARB_extensions_string(GLADuserptrloadfunc load, void *userptr) {
    if(!GLAD_WGL_ARB_extensions_string) return;
    glad_wglGetExtensionsStringARB = (PFNWGLGETEXTENSIONSSTRINGARBPROC) load(userptr, "wglGetExtensionsStringARB");
}
static void glad_wgl_load_WGL_EXT_extensions_string(GLADuserptrloadfunc load, void *userptr) {
    if(!GLAD_WGL_EXT_extensions_string) return;
    glad_wglGetExtensionsStringEXT = (PFNWGLGETEXTENSIONSSTRINGEXTPROC) load(userptr, "wglGetExtensionsStringEXT");
}
static void glad_wgl_load_WGL_NV_DX_interop(GLADuserptrloadfunc load, void *userptr) {
    if(!GLAD_WGL_NV_DX_interop) return;
    glad_wglDXCloseDeviceNV = (PFNWGLDXCLOSEDEVICENVPROC) load(userptr, "wglDXCloseDeviceNV");
    glad_wglDXLockObjectsNV = (PFNWGLDXLOCKOBJECTSNVPROC) load(userptr, "wglDXLockObjectsNV");
    glad_wglDXObjectAccessNV = (PFNWGLDXOBJECTACCESSNVPROC) load(userptr, "wglDXObjectAccessNV");
    glad_wglDXOpenDeviceNV = (PFNWGLDXOPENDEVICENVPROC) load(userptr, "wglDXOpenDeviceNV");
    glad_wglDXRegisterObjectNV = (PFNWGLDXREGISTEROBJECTNVPROC) load(userptr, "wglDXRegisterObjectNV");
    glad_wglDXSetResourceShareHandleNV = (PFNWGLDXSETRESOURCESHAREHANDLENVPROC) load(userptr, "wglDXSetResourceShareHandleNV");
    glad_wglDXUnlockObjectsNV = (PFNWGLDXUNLOCKOBJECTSNVPROC) load(userptr, "wglDXUnlockObjectsNV");
    glad_wglDXUnregisterObjectNV = (PFNWGLDXUNREGISTEROBJECTNVPROC) load(userptr, "wglDXUnregisterObjectNV");
}



static int glad_wgl_has_extension(HDC hdc, const char *ext) {
    const char *terminator;
    const char *loc;
    const char *extensions;

    if(wglGetExtensionsStringEXT == NULL && wglGetExtensionsStringARB == NULL)
        return 0;

    if(wglGetExtensionsStringARB == NULL || hdc == INVALID_HANDLE_VALUE)
        extensions = wglGetExtensionsStringEXT();
    else
        extensions = wglGetExtensionsStringARB(hdc);

    if(extensions == NULL || ext == NULL)
        return 0;

    while(1) {
        loc = strstr(extensions, ext);
        if(loc == NULL)
            break;

        terminator = loc + strlen(ext);
        if((loc == extensions || *(loc - 1) == ' ') &&
            (*terminator == ' ' || *terminator == '\0'))
        {
            return 1;
        }
        extensions = terminator;
    }

    return 0;
}

static GLADapiproc glad_wgl_get_proc_from_userptr(void *userptr, const char* name) {
    return (GLAD_GNUC_EXTENSION (GLADapiproc (*)(const char *name)) userptr)(name);
}

static int glad_wgl_find_extensions_wgl(HDC hdc) {
    GLAD_WGL_ARB_extensions_string = glad_wgl_has_extension(hdc, "WGL_ARB_extensions_string");
    GLAD_WGL_EXT_extensions_string = glad_wgl_has_extension(hdc, "WGL_EXT_extensions_string");
    GLAD_WGL_NV_DX_interop = glad_wgl_has_extension(hdc, "WGL_NV_DX_interop");
    GLAD_WGL_NV_DX_interop2 = glad_wgl_has_extension(hdc, "WGL_NV_DX_interop2");
    return 1;
}

static int glad_wgl_find_core_wgl(void) {
    int major = 1, minor = 0;
    GLAD_WGL_VERSION_1_0 = (major == 1 && minor >= 0) || major > 1;
    return GLAD_MAKE_VERSION(major, minor);
}

int gladLoadWGLUserPtr(HDC hdc, GLADuserptrloadfunc load, void *userptr) {
    int version;
    wglGetExtensionsStringARB = (PFNWGLGETEXTENSIONSSTRINGARBPROC) load(userptr, "wglGetExtensionsStringARB");
    wglGetExtensionsStringEXT = (PFNWGLGETEXTENSIONSSTRINGEXTPROC) load(userptr, "wglGetExtensionsStringEXT");
    if(wglGetExtensionsStringARB == NULL && wglGetExtensionsStringEXT == NULL) return 0;
    version = glad_wgl_find_core_wgl();


    if (!glad_wgl_find_extensions_wgl(hdc)) return 0;
    glad_wgl_load_WGL_ARB_extensions_string(load, userptr);
    glad_wgl_load_WGL_EXT_extensions_string(load, userptr);
    glad_wgl_load_WGL_NV_DX_interop(load, userptr);


    return version;
}

int gladLoadWGL(HDC hdc, GLADloadfunc load) {
    return gladLoadWGLUserPtr(hdc, glad_wgl_get_proc_from_userptr, GLAD_GNUC_EXTENSION (void*) load);
}
 


#ifdef __cplusplus
}
#endif
