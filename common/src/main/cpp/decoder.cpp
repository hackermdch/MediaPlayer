module;
#include <vector>
#include <memory>
#include <jni.h>
#include "jnipp.h"
extern "C" {
#include <libavutil/hwcontext.h>
#include <libavutil/avutil.h>
#include <libavcodec/avcodec.h>
#include <libavcodec/bsf.h>
#include <libavformat/avformat.h>
#include <libavutil/imgutils.h>
}
module Media;

#define ThrowOnFailed(X) if((X)<0) throw (X)

using namespace jni;
using namespace MediaPlayer;

static bool Support(const AVCodec* codec, const AVHWDeviceType type)
{
	int index = 0;
	bool support = false;
	while (true) {
		const AVCodecHWConfig* config = avcodec_get_hw_config(codec, index);
		if (!config) break;
		if (config->device_type == type) {
			support = true;
			break;
		}
		index++;
	}
	return support;
}

int VideoDecoder::RegisterMethods(JNIEnv* env)
{
	std::vector<JNINativeMethod> methods;
	methods.emplace_back(JNIMethod("open", "(Ljava/lang/String;II)J", Open));
	methods.emplace_back(JNIMethod("decode", "()V", (void(*)(JNIEnv*, jobject))Decode));
	methods.emplace_back(JNIMethod("release", "(J)V", Release));
	return env->RegisterNatives(env->FindClass("net/hacker/mediaplayer/VideoDecoder"), methods.data(), methods.size());
}

VideoDecoder* VideoDecoder::Open(JNIEnv* env, jobject obj, const jstring path, const GLuint texture, const AVHWDeviceType type)
{
	try {
		auto ptr = new VideoDecoder(toString(path), type, texture);
		auto [num, den] = ptr->format->streams[ptr->index]->avg_frame_rate;
		Object(obj).set("frameRate", den / (double)num);
		while (ptr->Decode() < 0)(void)0;
		return ptr;
	}
	catch (...)
	{
		Throw(env, "Open failed");
		return nullptr;
	}
}

void VideoDecoder::Decode(JNIEnv*, const jobject obj)
{
	GetPtr<VideoDecoder>(obj)->Decode();
}

void VideoDecoder::Release(JNIEnv*, jclass, const jlong ptr)
{
	delete (VideoDecoder*)ptr;
}

VideoDecoder::VideoDecoder(const std::string& url, const AVHWDeviceType hw_type, GLuint tex)
{
	format = avformat_alloc_context();
	ThrowOnFailed(avformat_open_input(&format, url.data(), nullptr, nullptr));
	ThrowOnFailed(avformat_find_stream_info(format, nullptr));
	index = av_find_best_stream(format, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
	if (index < 0) av_log(nullptr, AV_LOG_ERROR, "Can't find video stream in input file\n");
	auto origin_par = format->streams[index]->codecpar;
	codec = const_cast<AVCodec*>(avcodec_find_decoder(origin_par->codec_id));
	context = avcodec_alloc_context3(codec);
	type = hw_type;
	AVHWDeviceContext* device = nullptr;
	if (hw_type != AV_HWDEVICE_TYPE_NONE) {
		if (!Support(codec, type)) throw std::exception("error");
		AVBufferRef* hw_device_ctx;
		ThrowOnFailed(av_hwdevice_ctx_create(&hw_device_ctx, type, nullptr, nullptr, 0));
		context->hw_device_ctx = hw_device_ctx;
		device = (AVHWDeviceContext*)hw_device_ctx->data;
	}
	if (avcodec_parameters_to_context(context, origin_par) < 0) av_log(nullptr, AV_LOG_ERROR, "Error initializing the decoder context.\n");
	ThrowOnFailed(avcodec_open2(context, codec, nullptr));
	packet = av_packet_alloc();
	frame = av_frame_alloc();
	vframe = std::make_unique<VideoFrame>(type, device, tex);
}

VideoDecoder::~VideoDecoder()
{
	av_frame_free(&frame);
	av_packet_free(&packet);
	avcodec_free_context(&context);
	avformat_free_context(format);
}

int VideoDecoder::Decode()
{
start:
	int ret = av_read_frame(format, packet);
	if (ret != 0) {
		char buf[256];
		av_strerror(ret, buf, sizeof(buf));
		av_packet_unref(packet);
		return ret;
	}

	if (ret >= 0 && packet->stream_index != index) {
		av_packet_unref(packet);
		goto start;
	}

	int result = avcodec_send_packet(context, packet);
	av_packet_unref(packet);
	if (result < 0) {
		av_log(nullptr, AV_LOG_ERROR, "Error submitting a packet for decoding\n");
		goto start;
	}

	auto success = false;
	while (result >= 0) {
		result = avcodec_receive_frame(context, frame);
		if (result == AVERROR_EOF || result == AVERROR(EAGAIN)) break;
		if (result < 0) {
			av_log(nullptr, AV_LOG_ERROR, "Error decoding frame\n");
			av_frame_unref(frame);
			break;
		}
		vframe->Update(frame, context->hwaccel != nullptr);
		av_frame_unref(frame);
		success = true;
	}
	return success ? 0 : result;
}
