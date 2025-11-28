#include "git2.h"
#include "ppgit_common.h"
#include <errno.h>
#include <android/log.h>
#include <jni.h>
#include <assert.h>
#include <string.h>
#include <stdbool.h>

#define LOG_TAG "JNI_libgit_two"

JNIEnv * globalEnv=NULL;

JNIEXPORT jstring JNICALL J_MAKE_METHOD(LibgitTwo_hello)(JNIEnv *env, jclass type, jint a, jint b) {
    //如果要算a+b需要创建一个char数组，然后把下面的字符串和计算结果装到里面，再返回char数组，太麻烦了，算了。
    return (*env)->NewStringUTF(env, "Hello from NDK,sum is:");
}


/*  辅助方法 start */
//这个需要释放本地引用，最好释放，不释放应该也会自动释放，但自己控制更好，避免自动释放太慢导致变量表占满
static jclass findClass(JNIEnv *env, const char *name) {
    jclass localClass = (*env)->FindClass(env, name);
    if (!localClass) {
        ALOGE("Failed to find class '%s'", name);
        abort();
    }
    return localClass;

    //如果启用，每次调用此方法都会创建全局引用，若不释放，浪费内存，而且全局引用有数量限制，满了再建就报错了
//    jclass globalClass = (*env)->NewGlobalRef(env, localClass);
//    (*env)->DeleteLocalRef(env, localClass);
//    if (!globalClass) {
//        ALOGE("Failed to create a global reference for '%s'", name);
//        abort();
//    }
//    return globalClass;
}

//这个不需要释放
static jfieldID findField(JNIEnv *env, jclass clazz, const char *name, const char *signature) {
    jfieldID field = (*env)->GetFieldID(env, clazz, name, signature);
    if (!field) {
        ALOGE("Failed to find field '%s' '%s'", name, signature);
        abort();
    }
    return field;
}

//这个不需要释放
static jmethodID findMethod(JNIEnv *env, jclass clazz, const char *name, const char *signature) {
    jmethodID method = (*env)->GetMethodID(env, clazz, name, signature);
    if (!method) {
        ALOGE("Failed to find method '%s' '%s'", name, signature);
        abort();
    }
    return method;
}


static void throwException(JNIEnv *env, jclass exceptionClass, jmethodID constructor3,
                           jmethodID constructor2, const char *functionName, int error) {
    jthrowable cause = NULL;
    if ((*env)->ExceptionCheck(env)) {
        cause = (*env)->ExceptionOccurred(env);
        (*env)->ExceptionClear(env);
    }
    jstring detailMessage = (*env)->NewStringUTF(env, functionName);
    if (!detailMessage) {
        // Not really much we can do here. We're probably dead in the water,
        // but let's try to stumble on...
        (*env)->ExceptionClear(env);
    }
    jobject exception;
    if (cause) {
        exception = (*env)->NewObject(env, exceptionClass, constructor3, detailMessage, error,
                                      cause);
    } else {
        exception = (*env)->NewObject(env, exceptionClass, constructor2, detailMessage, error);
    }
    (*env)->Throw(env, exception);
    if (detailMessage) {
        (*env)->DeleteLocalRef(env, detailMessage);
    }
}
static jclass getLibgitTwoExceptionClass(JNIEnv *env) {
    static jclass exceptionClass = NULL;
    if (!exceptionClass) {
        exceptionClass = findClass(env, J_CLZ_PREFIX "LibGitTwoException");
    }
    return exceptionClass;
}

__attribute__((unused))  static void throwLibgitTwoException(JNIEnv* env, const char* functionName) {
    int error = errno;  //获取当前的errno值，好像是每个进程有各自的errno？类似 java 的ThreadLocal
    static jmethodID constructor3 = NULL;
    if (!constructor3) {
        constructor3 = findMethod(env, getLibgitTwoExceptionClass(env), "<init>",
                                  "(Ljava/lang/String;ILjava/lang/Throwable;)V");
    }
    static jmethodID constructor2 = NULL;
    if (!constructor2) {
        constructor2 = findMethod(env, getLibgitTwoExceptionClass(env), "<init>",
                                  "(Ljava/lang/String;I)V");
    }
    throwException(env, getLibgitTwoExceptionClass(env), constructor3, constructor2, functionName,
                   error);
}

//拷贝字符串并在其末尾加个\0
static char *j_strcopy(const char* src) {
    char *copy=NULL;
    size_t len = strlen(src)+1;
    if(src) {
        copy = malloc(len);
        if(copy) {
            strncpy(copy, src, len);
        }
    }
    return copy;
}

static char *j_copy_of_jstring(JNIEnv *env, jstring jstr, bool nullable) {
    if(!nullable) {
        //如果jstr为假，打印&&后面的字符串
        assert(jstr && "Cannot cast null to c string");
    }
    //如果jstr为无效内存地址0，返回0
    if(!jstr) {
        return NULL;
    }
    //最后一个参数是个 jboolean指针，如果其指向的地址不是NULL，则会对其赋值：创建了拷贝，设为真，否则设为假
    const char *c_str = (*env)->GetStringUTFChars(env,jstr,NULL);
    char *copy = j_strcopy(c_str);
    (*env)->ReleaseStringUTFChars(env, jstr, c_str);
    return copy;
}

/*  辅助方法 end */
JNIEXPORT void JNICALL J_MAKE_METHOD(LibgitTwo_jniLibgitTwoInit)(JNIEnv *env, jclass callerJavaClass) {
    git_libgit2_init();

    //TODO 不知道为什么，下面返回值就会报错，稍后再弄
//    int ret = git_libgit2_init();
//    if(ret!=0) {
//        //        throwLibgitTwoException(env,"jniClone");
//        const git_error *err = git_error_last();
//        ALOGE("jniLibgitTwoInit::ERROR '%s'", err->message);
//
//        return JNI_ERR;
//    }
//    return (jlong)ret;
}
//用https时，openssl验证证书失败也直接放行，解决证书无效的方法，若有其他解决方案则可删
int passCertCheck(git_cert *cert, int valid, const char *host, void *payload) {
/* 返回值说明：0允许连接；小于0拒绝连接；大于0表示这个callback不做决策，遵循其他验证器的结果。
    参见：https://libgit2.org/libgit2/#HEAD/group/callback/git_transport_certificate_check_cb
    */
    return 0;

}
JNIEXPORT void JNICALL J_MAKE_METHOD(LibgitTwo_jniSetCertCheck)(JNIEnv *env, jclass callerJavaClass, jlong remoteCallbacksPtr) {
    ((git_remote_callbacks *)remoteCallbacksPtr) ->certificate_check = passCertCheck;
}

JNIEXPORT jlong JNICALL J_MAKE_METHOD(LibgitTwo_jniSetCertFileAndOrPath)(JNIEnv *env, jclass callerJavaClass, jstring certFile, jstring certPath) {
    if(!certFile) {
        ALOGI("certFile is NULL");
    }
    if(!certPath) {
        ALOGI("certPath is NULL");
    }
    char *c_certFile = j_copy_of_jstring(env, certFile, true);
    char *c_certPath = j_copy_of_jstring(env, certPath, true);

    int error=git_libgit2_opts(GIT_OPT_SET_SSL_CERT_LOCATIONS,c_certFile,c_certPath);
//    ALOGE("errsetgitops::::%d", error);
    free(c_certFile);
    free(c_certPath);
    return error;
}

JNIEXPORT jlong JNICALL J_MAKE_METHOD(LibgitTwo_jniClone)(JNIEnv *env, jclass callerJavaClass, jstring url, jstring local_path, jlong jniOptionsPtr, jboolean allowInsecure) {
    git_repository *cloned_repo=NULL;
    char *c_url=j_copy_of_jstring(env, url, true);
    char *c_local_path=j_copy_of_jstring(env, local_path, true);
    git_clone_options cloneOptions = GIT_CLONE_OPTIONS_INIT;  //jniOptionsPtr是个地址，直接强转一下就行
    git_checkout_options checkout_opts = GIT_CHECKOUT_OPTIONS_INIT;
    checkout_opts.checkout_strategy = GIT_CHECKOUT_SAFE;
    cloneOptions.checkout_opts = checkout_opts;
    char *certfile="/storage/emulated/0/Android/data/com.catpuppyapp.puppygit/files/cafolder/399e7759.0";
    char *capath="/storage/emulated/0/Android/data/com.catpuppyapp.puppygit/files/cafolder";
    char *syscapath="/system/etc/security/cacerts/";
    char *syscapath2="/system/etc/security/cacerts_google";
    char *gitlabca="/storage/emulated/0/Android/data/com.catpuppyapp.puppygit/files/cafolder/gitlab.crt";

    ALOGE("before set cert path in libgit");

    int error = 0;
    //能添加多个证书或证书目录
//    error = git_libgit2_opts(GIT_OPT_SET_SSL_CERT_LOCATIONS,certfile,capath);
//     error = git_libgit2_opts(GIT_OPT_SET_SSL_CERT_LOCATIONS,gitlabca,NULL);
     error=git_libgit2_opts(GIT_OPT_SET_SSL_CERT_LOCATIONS,NULL,syscapath);
//    git_libgit2_opts(GIT_OPT_SET_SSL_CERT_LOCATIONS,gitlabca,NULL);
//    error = git_libgit2_opts(GIT_OPT_SET_SSL_CERT_LOCATIONS,NULL,syscapath2);
    if(error!=0) {
//        ALOGE("jniClonecertifile::ERROR '%s'", certfile);
//        ALOGE("jniClonecertpath::ERROR '%s'", capath);
        ALOGE("jniClonecertpath::ERROR '%d'", error);

    }


    if(allowInsecure){
        cloneOptions.fetch_opts.callbacks.certificate_check = passCertCheck;
    }
    int ret = git_clone(&cloned_repo, c_url, c_local_path, &cloneOptions);
    free(c_url);
    free(c_local_path);
    if(ret!=0) {
//        throwLibgitTwoException(env,"jniClone");
        const git_error *err = git_error_last();
        ALOGE("jniClone::ERROR '%s'", err->message);

        return JNI_ERR;
    }
    return (jlong)cloned_repo;
}

int cred_acquire_cb(git_credential **out, const char *url, const char *username_from_url, unsigned int allowed_types, void *payload) {
//    JNIEnv *env = globalEnv;
//    (*env)->CallObjectMethod(env, (jobject)payload, jniConstants->remote.midAcquireCred, (jstring)jniUrl, (jstring)usernameFromUrl, (jint)allowed_types);
    return git_credential_userpass_plaintext_new(out, "testusername", "testpassword");
}

JNIEXPORT void JNICALL J_MAKE_METHOD(LibgitTwo_jniSetCredentialCbTest)(JNIEnv *env, jclass callerJavaClass, jlong remoteCallbacks) {
    ALOGD("LibgitTwo_jniSetCredentialCbTest::");
    globalEnv = env;
    git_remote_callbacks *ptr = (git_remote_callbacks *)remoteCallbacks;
    ptr->credentials = cred_acquire_cb;
}
JNIEXPORT jlong JNICALL J_MAKE_METHOD(LibgitTwo_jniCreateCloneOptions)(JNIEnv *env, jclass callerJavaClass, jint version) {

    //分配内存
    git_clone_options *clone_options = (git_clone_options *)malloc(sizeof(git_clone_options));
    //初始化options结构体的值
    int ret = git_clone_init_options(clone_options,(unsigned int)version);
    //设置当前对象的字段 jniCRepoPtr 值为clone_options指针的值
    if(ret!=0) {
        //TODO 这个函数找不到对象，检查下类的构造器，然后把libgit2的错误信息附加的字符串里
//        throwLibgitTwoException(env,"jniCreateCloneOptions");
        return JNI_ERR;
    }
    //返回指针给java存上
    return (jlong)clone_options;
}

JNIEXPORT jlong JNICALL J_MAKE_METHOD(LibgitTwo_jniTestClone)(JNIEnv *env, jclass callerJavaClass, jstring url, jstring local_path, jlong jniOptionsPtr) {
    //分配内存
    git_clone_options *clone_options = (git_clone_options *)malloc(sizeof(git_clone_options));
    //初始化options结构体的值
    int ret = git_clone_init_options(clone_options,1);
    //设置当前对象的字段 jniCRepoPtr 值为clone_options指针的值
    if(ret!=0) {
//        throwLibgitTwoException(env,"jniCreateCloneOptions");
        return JNI_ERR;
    }

    git_repository *repo=NULL;
    char *c_url=j_copy_of_jstring(env, url, true);
    char *c_local_path=j_copy_of_jstring(env, local_path, true);

    int ret2 = git_clone(&repo, c_url, c_local_path, clone_options);
    free(c_url);
    free(c_local_path);
    if(ret || ret2) {
//        throwLibgitTwoException(env,"jniClone");
        return JNI_ERR;
    }
    return (jlong)repo;
}

JNIEXPORT jstring JNICALL J_MAKE_METHOD(LibgitTwo_jniLineGetContent)(JNIEnv *env, jclass callerJavaClass, jlong linePtr)
{
    ALOGD("ccode: jniLineGetContent() start\n");
    jstring s = (*env)->NewStringUTF(env, ((git_diff_line *)linePtr)->content);
    ALOGD("ccode: jniLineGetContent() end\n");
    return s;
}


JNIEXPORT void JNICALL J_MAKE_METHOD(LibgitTwo_jniTestAccessExternalStorage)(JNIEnv *env, jclass callerJavaClass)
{
    ALOGD("ccode: LibgitTwo_jniTestAccessExternalStorage() start\n");
    FILE* file = fopen("/sdcard/puppygit-repos/hello.txt","w+");

    if (file != NULL)
    {
        fputs("HELLO WORLD!\n", file);
        fflush(file);
        fclose(file);
    }

    ALOGD("ccode: LibgitTwo_jniTestAccessExternalStorage() end\n");
}

JNIEXPORT jobject JNICALL J_MAKE_METHOD(LibgitTwo_jniEntryByName)(JNIEnv *env, jclass callerJavaClass, jlong treePtr, jstring filename)
{
    char *c_filename = j_copy_of_jstring(env, filename, true);
//    char *c_filename = "settings.cpp";
//    ALOGD("filename in c: %s", c_filename);
    const git_tree_entry *r = git_tree_entry_byname((git_tree *)treePtr, c_filename);
//    ALOGD("tree ptr in c: %p", (void *)treePtr);
//    ALOGD("entry ptr in c: %p", r);
    free(c_filename);
    return (jobject)r;
}


void bytesToHexString(const unsigned char *bytes, size_t length, char *hexString) {
    for (size_t i = 0; i < length; i++) {
        // 使用 sprintf 将每个字节转换为两位十六进制数
        sprintf(hexString + (i * 2), "%02x", bytes[i]);
    }
    hexString[length * 2] = '\0'; // 确保字符串以 null 结尾
}


/**
 * see: https://libgit2.org/libgit2/#v1.7.2/type/git_cert_hostkey
 */
jobject createSshCert(git_cert_hostkey *certHostKey, jstring hostname, JNIEnv *env) {
    // 获取 SshCert 类
    jclass sshCertClass = findClass(env, J_CLZ_PREFIX "SshCert");


    // 获取构造函数
    jmethodID constructor = findMethod(env, sshCertClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    // 每个字节需要2个字符 + 1 个 null 终止符('\0')
    char *md5Str[16*2+1];
    char *sha1Str[20*2+1];
    char *sha256Str[32*2+1];

    char *hostKeyStr[certHostKey->hostkey_len + 1];

    // unsigned char [16]
    if(certHostKey->type&GIT_CERT_SSH_MD5) {
        bytesToHexString(certHostKey->hash_md5, 16, md5Str);
    }
    // unsigned char [20]
    if(certHostKey->type&GIT_CERT_SSH_SHA1) {
        bytesToHexString(certHostKey->hash_sha1, 20, sha1Str);
    }
    // unsigned char [32]
    if(certHostKey->type&GIT_CERT_SSH_SHA256) {
        bytesToHexString(certHostKey->hash_sha256, 32, sha256Str);
    }

    //const char *hostkey
    //size_t hostkey_len
    if(certHostKey->type&GIT_CERT_SSH_RAW) {
        strncpy(hostKeyStr, certHostKey->hostkey, certHostKey->hostkey_len);
        hostKeyStr[certHostKey->hostkey_len] = '\0';
    }

    // 创建 SshCert 对象
    jobject sshCertObject = (*env)->NewObject(
            env,
            sshCertClass,
            constructor,

            hostname,
            (*env)->NewStringUTF(env, (const char *) md5Str),
            (*env)->NewStringUTF(env, (const char *) sha1Str),
            (*env)->NewStringUTF(env, (const char *) sha256Str),
            (*env)->NewStringUTF(env, (const char *) hostKeyStr)
    );

    (*env)->DeleteLocalRef(env, sshCertClass);

    return sshCertObject;
}



JNIEXPORT jobject JNICALL J_MAKE_METHOD(LibgitTwo_jniGetDataOfSshCert)(JNIEnv *env, jclass callerJavaClass, jlong cretprt, jstring hostname)
{
    git_cert_t type = ((git_cert*)cretprt)->cert_type;
    if(type == GIT_CERT_HOSTKEY_LIBSSH2) {
        return createSshCert((git_cert_hostkey *)cretprt, hostname, env);
    }else {
        ALOGW("is not a ssh cert, type=%d", type);
        return NULL;
    }

}


JNIEXPORT jint JNICALL J_MAKE_METHOD(LibgitTwo_jniSaveBlobToPath)(JNIEnv *env, jclass callerJavaClass, jlong blobPtr, jstring savePath)
{
    git_blob* blob = (git_blob *)blobPtr;

    // 字节流
    //这blob data不用释放文档说的：“this pointer is owned internally by the object and shall not be free'd” ,
    // 参见：https://libgit2.org/docs/reference/main/blob/git_blob_rawcontent.html
    const char* blob_data = git_blob_rawcontent(blob);
    size_t blob_size = git_blob_rawsize(blob);

    //转换路径为c字符串
    char* c_savePath = j_copy_of_jstring(env, savePath, true);
    if(c_savePath == NULL) {
        free(c_savePath);
        return -1;
    }

    //写入文件
    FILE *output_file = fopen(c_savePath, "wb");
    fwrite(blob_data, 1, blob_size, output_file);


    // clean up
    fclose(output_file);
    free(c_savePath);

    return 0;
}

JNIEXPORT jlongArray JNICALL J_MAKE_METHOD(LibgitTwo_jniGetStatusEntryRawPointers)(JNIEnv *env, jclass obj, jlong statusListPtr) {
    git_status_list* listPtr = (git_status_list *)statusListPtr;
    size_t length = git_status_list_entrycount(listPtr);

    // create jlongArray
    jlongArray resultArray = (*env)->NewLongArray(env, length);
    if (resultArray == NULL) {
        return NULL; // mem assign failed
    }

    jsize jlongSize = sizeof(jlong);

    for(int i=0; i < length; i++) {
        (*env)->SetLongArrayRegion(env, resultArray, i, jlongSize, git_status_byindex(listPtr, i));
    }

    // 返回数组
    return resultArray;
}



jobject createStatusEntryDto(
        JNIEnv *env,
        jclass statusEntryDtoClass,
        jmethodID constructor,

        jstring indexToWorkDirOldFilePath,
        jstring indexToWorkDirNewFilePath,
        jstring headToIndexOldFilePath,
        jstring headToIndexNewFilePath,

        jlong indexToWorkDirOldFileSize,
        jlong indexToWorkDirNewFileSize,
        jlong headToIndexOldFileSize,
        jlong headToIndexNewFileSize,

        jint statusFlag
) {

    // 创建 StatusEntryDto 对象
    return (*env)->NewObject(
            env,
            statusEntryDtoClass,
            constructor,

            indexToWorkDirOldFilePath,
            indexToWorkDirNewFilePath,
            headToIndexOldFilePath,
            headToIndexNewFilePath,

            indexToWorkDirOldFileSize,
            indexToWorkDirNewFileSize,
            headToIndexOldFileSize,
            headToIndexNewFileSize,

            statusFlag
    );
}

jclass statusEntryDtoClassCache = NULL;

JNIEXPORT jobjectArray JNICALL J_MAKE_METHOD(LibgitTwo_jniGetStatusEntries)(JNIEnv *env, jclass obj, jlong statusListPtr) {
    git_status_list* listPtr = (git_status_list *)statusListPtr;
    size_t length = git_status_list_entrycount(listPtr);

    // 获取 StatusEntryDto 类的引用
    jclass statusEntryDtoClass = findClass(env, J_CLZ_PREFIX "StatusEntryDto");

    // 获取构造函数 ID
    jmethodID constructor = findMethod(env, statusEntryDtoClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJJJI)V");

    // 创建 StatusEntryDto 对象的数组
    jobjectArray statusEntryDtoArray = (*env)->NewObjectArray(env, length, statusEntryDtoClass, NULL);
    if (statusEntryDtoArray == NULL) {
        return NULL; // 数组创建失败
    }





    //填充数组
    for(int i=0; i < length; i++) {
        git_status_entry* entry = git_status_byindex(listPtr, i);

        // 如果光查 index to worktree，那head to index就是NULL，反之亦然
        git_diff_delta * head2IndexDelta = entry->head_to_index;
        git_diff_delta * index2WorkDirDelta = entry->index_to_workdir;

        jstring index2WorkDirDeltaOldFilePath = NULL;
        jstring index2WorkDirDeltaNewFilePath = NULL;
        jlong index2WorkDirDeltaOldFileSize = 0;
        jlong index2WorkDirDeltaNewFileSize = 0;
        if(index2WorkDirDelta != NULL) {
            index2WorkDirDeltaOldFilePath = (*env)->NewStringUTF(env, index2WorkDirDelta->old_file.path);
            index2WorkDirDeltaNewFilePath = (*env)->NewStringUTF(env, index2WorkDirDelta->new_file.path);
            index2WorkDirDeltaOldFileSize = index2WorkDirDelta->old_file.size;
            index2WorkDirDeltaNewFileSize = index2WorkDirDelta->new_file.size;
        }

        jstring head2IndexDeltaOldFilePath = NULL;
        jstring head2IndexDeltaNewFilePath = NULL;
        jlong head2IndexDeltaOldFileSize = 0;
        jlong head2IndexDeltaNewFileSize = 0;
        if(head2IndexDelta != NULL) {
            head2IndexDeltaOldFilePath = (*env)->NewStringUTF(env, head2IndexDelta->old_file.path);
            head2IndexDeltaNewFilePath = (*env)->NewStringUTF(env, head2IndexDelta->new_file.path);
            head2IndexDeltaOldFileSize = head2IndexDelta->old_file.size;
            head2IndexDeltaNewFileSize = head2IndexDelta->new_file.size;
        }


        (*env)->SetObjectArrayElement(
                env,
                statusEntryDtoArray,
                i,
                createStatusEntryDto(
                    env,
                    statusEntryDtoClass,
                    constructor,

                    index2WorkDirDeltaOldFilePath,
                    index2WorkDirDeltaNewFilePath,
                    head2IndexDeltaOldFilePath,
                    head2IndexDeltaNewFilePath,

                    index2WorkDirDeltaOldFileSize,
                    index2WorkDirDeltaNewFileSize,
                    head2IndexDeltaOldFileSize,
                    head2IndexDeltaNewFileSize,

                    entry->status
                )
        );

    }

    (*env)->DeleteLocalRef(env, statusEntryDtoClass);

    //构造器属于method，method和field都不用释放，若释放会报错
//    (*env)->DeleteLocalRef(env, constructor);

    // 返回数组
    return statusEntryDtoArray;
}

