module;
#include <jni.h>
#include <string>
#include <vector>
#include <tuple>
#include <d3d12.h>
#include <d3d11_4.h>
#include <wrl.h>
#include <ffnvcodec/dynlink_loader.h>
#include "jnipp.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavcodec/bsf.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
}
export module Media;

using namespace Microsoft::WRL;

namespace MediaPlayer
{
	inline void Throw(JNIEnv* env, const char* msg)
	{
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), msg);
	}

	template <typename F> requires std::is_function_v<std::remove_pointer_t<F>>
	consteval JNINativeMethod JNIMethod(const char* name, const char* sig, F ptr)
	{
		return { const_cast<char*>(name),const_cast<char*>(sig), (void*)ptr };
	}

	template<typename T>
	T* GetPtr(const jni::Object& obj)
	{
		return (T*)obj.get<jlong>("ptr");
	}

	inline std::string toString(const jstring str)
	{
		std::string result;
		if (str != nullptr)
		{
			JNIEnv* env = jni::env();
			const char* chars = env->GetStringUTFChars(str, nullptr);
			result.assign(chars, env->GetStringUTFLength(str));
			env->ReleaseStringUTFChars(str, chars);
			env->DeleteLocalRef(str);
		}
		return result;
	}

	struct Descriptor
	{
		D3D12_CPU_DESCRIPTOR_HANDLE CPUHandle;
		D3D12_GPU_DESCRIPTOR_HANDLE GPUHandle;
	};
}

export namespace MediaPlayer
{
	class VideoFrame final
	{
		ComPtr<ID3D11Device> D3D11Device;
		ComPtr<ID3D11DeviceContext> D3D11DeviceContext;
		ComPtr<ID3D11Texture2D> ComputeResult;
		ComPtr<ID3D11ComputeShader> ComputeShader;
		ComPtr<ID3D11UnorderedAccessView> UAV;
		ComPtr<ID3D12Device> D3D12Device;
		ComPtr<ID3D12CommandQueue> CommandQueue;
		ComPtr<ID3D12CommandAllocator> CommandAllocator;
		ComPtr<ID3D12GraphicsCommandList> CommandList;
		ComPtr<ID3D12RootSignature> RootSignature;
		ComPtr<ID3D12PipelineState> PSO;
		ComPtr<ID3D12DescriptorHeap> DescriptorHeap;
		ComPtr<ID3D12Resource> OutputBuffer;
		ComPtr<ID3D12Fence> fence;
		HANDLE FenceEvent;
		HANDLE SharedHandle;
		uint32_t memory;
		uint64_t FenceValue;
		Descriptor SRV0, SRV1, UAV0;
		HANDLE DXNVDevice;
		void* TextureObject;
		CudaFunctions* cuda;
		CUcontext cu_ctx;
		CUstream stream;
		CUgraphicsResource cu_res;
		void* output;
		GLuint hw_texture;
		AVHWDeviceType hwtype;
		bool hwaccel, init;

		void UpdateSW(const AVFrame* frame);
		void UpdateD3D11(const AVFrame* frame);
		void UpdateD3D12(const AVFrame* frame);
		void UpdateCUDA(const AVFrame* frame);
	public:
		VideoFrame(AVHWDeviceType type, const AVHWDeviceContext* hwctx, GLuint tex);
		~VideoFrame();
		void Update(const AVFrame* frame, bool hwaccel = false);
	};

	class VideoDecoder final
	{
		VideoFrame* vframe;
		AVFormatContext* format;
		AVCodec* codec;
		AVCodecContext* context;
		AVHWDeviceType type;
		int index;
		AVPacket* packet;
		AVFrame* frame;

		int Decode();
	public:
		explicit VideoDecoder(const std::string& url, AVHWDeviceType type, GLuint tex);
		~VideoDecoder();
		static int RegisterMethods(JNIEnv* env);
		static VideoDecoder* Open(JNIEnv* env, jobject obj, jstring path, GLuint texture, AVHWDeviceType type);
		static void Decode(JNIEnv* env, jobject obj);
		static void Release(JNIEnv*, jclass, jlong ptr);
	};

	class AudioDecoder final
	{
		AVFormatContext* format;
		AVCodec* codec;
		AVCodecContext* context;
		int index;
		AVPacket* packet;
		AVFrame* frame;
		SwrContext* swr{};
		AVChannelLayout layout{};

		int Decode(std::vector<std::tuple<size_t, AVFrame*>>& vec, size_t& size);
		jobject Decode(bool mono);
	public:
		explicit AudioDecoder(const std::string& url);
		~AudioDecoder();
		static int RegisterMethods(JNIEnv* env);
		static AudioDecoder* Open(JNIEnv* env, jobject obj, jstring path);
		static jobject Decode(JNIEnv* env, jobject obj, bool mono);
		static void Release(JNIEnv*, jclass, jlong ptr);
	};

	void Init(JNIEnv* env, jclass, jlong proc);

	int RegisterMethods(JNIEnv* env)
	{
		av_log_set_level(AV_LOG_QUIET);
		std::vector<JNINativeMethod> methods;
		methods.emplace_back(JNIMethod("init", "(J)V", Init));
		return env->RegisterNatives(env->FindClass("net/hacker/mediaplayer/MediaPlayer"), methods.data(), methods.size()) + VideoDecoder::RegisterMethods(env) + AudioDecoder::RegisterMethods(env);
	}
}
