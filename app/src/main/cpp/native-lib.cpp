#include <jni.h>
#include <string>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <errno.h>

#include "include/opus/opus.h"
#include "include/LogUtils.h"
#define TAG "jni_ptt"                                         // 定义TAG,以便区分不同文件打印


// JNI 全局变量
JavaVM *g_jvm;                                                // 全局的JavaVM指针
jclass g_class;                                               // 全局Java类引用
jobject g_object_opus;                                        // 全局Java端opus的引用
static jmethodID g_callback_methodID;                         // 缓存回调方法的id

// 默认参数
int g_sample_rate = 8000;                                     // 默认采样率(Hz)
int g_channel = 2;                                            // 默认通道数
int g_lsb_depth = 16;                                         // 默认量化位数(bit)
int g_duration = 20;                                          // 默认帧时长(ms)
int g_frame_size = g_sample_rate * g_duration / 1000;         // 默认样本数

// socket 全局变量, 本机做服务端
// 服务端配置
int g_s_fd;                                                   // 服务端socket fd
struct sockaddr_in g_s_addr;                                  // 服务端socket address
pthread_t g_s_tid;                                            // 服务端接收线程ID

// 客户端配置
int g_c_fd;                                                   // 客户端socket fd
struct sockaddr_in g_c_addr;                                  // 客户端socket address


#define MAX_PACKAGE_LENGTH (1024)                           // 最大包大小
unsigned char g_s_rcv_buf[MAX_PACKAGE_LENGTH];                // 服务端接收数据缓冲区

// opus全局变量
OpusDecoder *g_decoder = nullptr;                             // 解码器引擎
OpusEncoder *g_encoder = nullptr;                             // 编码器引擎

// DirectBuffer
opus_int16 *g_opus_buffer = nullptr;                          // 编码后缓冲区
opus_int16 *g_decoder_buffer = nullptr;                       // 解码缓冲区


/**
 * 解码线程
 * @param arg   线程参数
 * @return null
 */
void* s_rcv(void *arg){
    int num = -1;
    int ret = -1;
    // 获取JNIEnv
    JNIEnv *env;
    // 将线程附加到JVM,并获取env
    if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK){
        LOGE("%s(), AttachCurrentThread failed\n", __func__ );
        return nullptr;
    }

    while (1){
        num = recvfrom(g_s_fd, g_s_rcv_buf, sizeof(g_s_rcv_buf), 0, 0, 0);
        // 解码
        ret = opus_decode(g_decoder, g_s_rcv_buf, num, g_decoder_buffer, g_frame_size, 0);
        // 通知Java解码播放
        env->CallVoidMethod(g_object_opus, g_callback_methodID, ret * g_channel * (g_lsb_depth/8));
    }
    // 将线程从JVM分离
    g_jvm->DetachCurrentThread();

    return nullptr;
}

/**
 * opus的初始化方法
 * @param env             JNI指针
 * @param thiz            Java类对象引用
 * @param sample_rate     采样率
 * @param channels        通道数
 * @param lsb_depth       量化位数
 */
void native_opus_init(JNIEnv* env, jobject thiz, jint sample_rate, jint channels, jint lsb_depth){
    // 重新初始化参数
    g_sample_rate = sample_rate;
    g_channel = channels;
    g_lsb_depth = lsb_depth;
    g_frame_size = g_sample_rate * g_duration / 1000;

    int err;
    // 初始化编码器
    g_encoder = opus_encoder_create(g_sample_rate, g_channel, OPUS_APPLICATION_VOIP, &err);
    if (err != OPUS_OK || g_encoder == NULL){
        LOGE("%s(), opus encoder create error\n", __func__ );
        return ;
    }
    // 参数设置
    //固定码率
    opus_encoder_ctl(g_encoder, OPUS_SET_VBR(0));
    //设置码率
    opus_encoder_ctl(g_encoder, OPUS_SET_BITRATE(96000));
    //设置算法复杂度
    opus_encoder_ctl(g_encoder, OPUS_SET_COMPLEXITY(8));
    //仅传输音频
    opus_encoder_ctl(g_encoder, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
    //量化位数
    opus_encoder_ctl(g_encoder, OPUS_SET_LSB_DEPTH(g_lsb_depth));
    //不使用DTX不连续传输(DTX降低编码器的功耗和网络带宽占用)
    opus_encoder_ctl(g_encoder, OPUS_SET_DTX(0));
    //不使用前向纠错
    opus_encoder_ctl(g_encoder, OPUS_SET_INBAND_FEC(0));
    LOGE("%s(),  opus_encoder_create success", __func__ );

    // 初始化解码器
    g_decoder = opus_decoder_create(g_sample_rate, g_channel, &err);
    if (err != OPUS_OK || g_decoder == NULL){
        LOGE("%s(), opus decoder create error\n", __func__ );
        return ;
    }
    // 参数设置
    //量化位数
    opus_decoder_ctl(g_decoder, OPUS_SET_LSB_DEPTH(g_lsb_depth));
    LOGE("%s(), opus_decoder_create success", __func__ );
}

/**
 * 获取编码使用的directBuffer
 * @param env
 * @param thiz
 * @return
 */
jobject native_getEncoderDirectBuffer(JNIEnv* env, jobject thiz){
    return env->NewDirectByteBuffer(g_opus_buffer, MAX_PACKAGE_LENGTH);
}

/**
 * 获取解码使用的directBuffer
 * @param env
 * @param thiz
 * @return
 */
jobject native_getDecoderDirectBuffer(JNIEnv* env, jobject thiz){
    return env->NewDirectByteBuffer(g_decoder_buffer, MAX_PACKAGE_LENGTH);
}

/**
 * 编码
 * @param env           JNI指针
 * @param thiz          Java类对象引用
 * @return void
 */
void native_opus_encoder(JNIEnv* env, jobject thiz){
    unsigned char opus_buffer[MAX_PACKAGE_LENGTH];
    LOGE("%s(),   g_frame_size: %d     g_opus_buffer = %p  g_encoder = %p\n", __func__ , g_frame_size, g_opus_buffer, g_encoder);
    // 编码
    int data_length = opus_encode(g_encoder,g_opus_buffer, g_frame_size, opus_buffer, MAX_PACKAGE_LENGTH);
    // 发送到对端
    sendto(g_c_fd, opus_buffer, data_length, 0, (struct sockaddr *)&g_c_addr, sizeof(g_c_addr));
    LOGE("%s(), send to client ", __func__ );
}

/**
 * 初始化网络
 * @param env         JNI 指针
 * @param thiz        Java引用
 * @param local_ip    本地IP，用于创建监听
 * @param local_port  本地port 用于创建监听
 * @param remote_ip   对端ip，用于发送
 * @param remote_port 对端port，用于发送
 */
void native_init_network(JNIEnv* env, jobject thiz, jstring local_ip, jint local_port, jstring remote_ip, jint remote_port){

    // 创建全局引用，用于回调Java方法
    g_object_opus = env->NewGlobalRef(thiz);
    // 创建udp服务端，用于接收音频数据
    g_s_fd= socket(PF_INET, SOCK_DGRAM, 0);
    if (g_s_fd == -1){
        LOGE("%s(), socket create failed: %s\n", __func__, strerror(errno));
        return ;
    }
    const char *clocalip = env->GetStringUTFChars(local_ip, 0);
    g_s_addr.sin_family = AF_INET;
    g_s_addr.sin_port = htons(local_port);
    inet_pton(AF_INET, clocalip, &g_s_addr.sin_addr.s_addr);
    int ret = bind(g_s_fd, (struct sockaddr*)&g_s_addr, sizeof(g_s_addr));
    if (ret < 0){
        env->ReleaseStringUTFChars(local_ip, clocalip);
        LOGE("%s(), bind error: %s\n", __func__ , strerror(errno));
        return ;
    }
    // 创建接收线程
    pthread_create(&g_s_tid, NULL, s_rcv, NULL);
    // 分离线程
    pthread_detach(g_s_tid);

    // 客户端socket初始化
    g_c_fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (g_c_fd == -1){
        LOGE("%s(), socket create failed: %s\n", __func__, strerror(errno));
        return ;
    }
    const char *cremoteip = env->GetStringUTFChars(remote_ip, 0);
    g_c_addr.sin_family = AF_INET;
    g_c_addr.sin_port = htons(remote_port);
    inet_pton(AF_INET, cremoteip, &g_c_addr.sin_addr.s_addr);
    env->ReleaseStringUTFChars(local_ip, clocalip);
    env->ReleaseStringUTFChars(remote_ip, cremoteip);
}



// 本地方法数组，用来函数的动态注册
static const JNINativeMethod methods[] = {
        {"opus_init", "(III)V", (void *)(native_opus_init)},
        {"getEncodeDirectBuffer", "()Ljava/nio/ByteBuffer;", (void *)(native_getEncoderDirectBuffer)},
        {"getDecodeDirectBuffer", "()Ljava/nio/ByteBuffer;", (void *)(native_getDecoderDirectBuffer)},
        {"opus_encoder", "()V", (void*)(native_opus_encoder)},
        {"init_network", "(Ljava/lang/String;ILjava/lang/String;I)V", (void*)(native_init_network)},
};

/**
 * 库加载时调用的第一个方法
 * @param vm
 * @param reserved
 * @return
 */
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved){
    // 定义本地变量env
    JNIEnv *env;
    g_jvm = vm;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK){
        // 获取env失败
        return JNI_ERR;
    }

    // 查找类，并保存为全局。FindClass方法获取的类的引用在调用它的那个JNI本地线程中有效，
    jclass clazz = env->FindClass("com/example/ptt/Opus");
    if (NULL == clazz){
        // 查找类失败
        return JNI_ERR;
    }
    // 创建全局引用
    g_class = static_cast<jclass>(env->NewGlobalRef(clazz));
    g_callback_methodID = env->GetMethodID(clazz, "onPcmCallback", "(I)V");
    // 动态注册JNI方法
    int rc = env->RegisterNatives(clazz, methods, sizeof(methods)/sizeof(JNINativeMethod));
    if (rc != JNI_OK) return rc;

    // 申请缓冲区空间
    g_opus_buffer = (opus_int16 *)malloc(MAX_PACKAGE_LENGTH);
    g_decoder_buffer = (opus_int16 *)malloc(MAX_PACKAGE_LENGTH);
    // 返回JNI版本
    return JNI_VERSION_1_6;
}