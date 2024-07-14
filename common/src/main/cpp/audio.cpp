module;
#include <vector>
#include <memory>
#include <jni.h>
#include "jnipp.h"
extern "C" {
#include <libavutil/avutil.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
}
module Media;

#define ThrowOnFailed(X) if((X)<0) throw (X)

import Resource;

using namespace jni;
using namespace MediaPlayer;

AudioDecoder::AudioDecoder(const std::string& url)
{
	format = avformat_alloc_context();
	ThrowOnFailed(avformat_open_input(&format, url.data(), nullptr, nullptr));
	ThrowOnFailed(avformat_find_stream_info(format, nullptr));
	index = av_find_best_stream(format, AVMEDIA_TYPE_AUDIO, -1, -1, nullptr, 0);
	if (index < 0) av_log(nullptr, AV_LOG_ERROR, "Can't find audio stream in input file\n");
	auto origin_par = format->streams[index]->codecpar;
	codec = const_cast<AVCodec*>(avcodec_find_decoder(origin_par->codec_id));
	context = avcodec_alloc_context3(codec);
	if (avcodec_parameters_to_context(context, origin_par) < 0) av_log(nullptr, AV_LOG_ERROR, "Error initializing the decoder context.\n");
	ThrowOnFailed(avcodec_open2(context, codec, nullptr));
	packet = av_packet_alloc();
	frame = av_frame_alloc();
}

AudioDecoder::~AudioDecoder()
{
	av_frame_free(&frame);
	av_packet_free(&packet);
	avcodec_free_context(&context);
	avformat_free_context(format);
}

int AudioDecoder::Decode(std::vector<std::tuple<size_t, AVFrame*>>& vec, size_t& osize)
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
		auto trans = av_frame_alloc();
		auto samples = av_rescale_rnd(swr_get_delay(swr, context->sample_rate) + frame->nb_samples, 44100, frame->sample_rate, AV_ROUND_UP);
		av_samples_alloc(trans->data, trans->linesize, layout.nb_channels, samples, AV_SAMPLE_FMT_S16, 0);
		auto count = swr_convert(swr, trans->data, samples, frame->data, frame->nb_samples);
		auto size = av_samples_get_buffer_size(trans->linesize, layout.nb_channels, count, AV_SAMPLE_FMT_S16, 1);
		vec.emplace_back(size, trans);
		osize += size;
		av_frame_unref(frame);
		success = true;
	}
	return success ? 0 : result;
}

jobject AudioDecoder::Decode(const bool mono)
{
	av_channel_layout_from_mask(&layout, mono ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO);
	swr_alloc_set_opts2(&swr, &layout, AV_SAMPLE_FMT_S16, 44100, &context->ch_layout, context->sample_fmt, context->sample_rate, 0, nullptr);
	swr_init(swr);
	std::vector<std::tuple<size_t, AVFrame*>> vec;
	size_t size = 0, offset = 0;
	while (Decode(vec, size) != AVERROR_EOF)(void)0;
	auto data = new uint8_t[size];
	for (auto& [size, frame] : vec)
	{
		memcpy(data + offset, frame->data[0], size);
		av_frame_free(&frame);
		offset += size;
	}
	swr_free(&swr);
	return Class("net/hacker/mediaplayer/Audio").newInstance("(Ljava/nio/ByteBuffer;I)V", env()->NewDirectByteBuffer(data, size), mono ? 1 : 2).makeLocalReference();
}

int AudioDecoder::RegisterMethods(JNIEnv* env)
{
	std::vector<JNINativeMethod> methods;
	methods.emplace_back(JNIMethod("open", "(Ljava/lang/String;)J", Open));
	methods.emplace_back(JNIMethod("decode", "(Z)Lnet/hacker/mediaplayer/Audio;", (jobject(*)(JNIEnv*, jobject, bool))Decode));
	methods.emplace_back(JNIMethod("release", "(J)V", Release));
	return env->RegisterNatives(env->FindClass("net/hacker/mediaplayer/AudioDecoder"), methods.data(), methods.size());
}

AudioDecoder* AudioDecoder::Open(JNIEnv* env, jobject, const jstring path)
{
	try {
		return new AudioDecoder(toString(path));
	}
	catch (...)
	{
		Throw(env, "Open failed");
		return nullptr;
	}
}

jobject AudioDecoder::Decode(JNIEnv*, const jobject obj, const bool mono)
{
	return GetPtr<AudioDecoder>(obj)->Decode(mono);
}

void AudioDecoder::Release(JNIEnv*, jclass, const jlong ptr)
{
	delete (AudioDecoder*)ptr;
}
