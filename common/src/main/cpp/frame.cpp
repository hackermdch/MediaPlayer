module;
#include <d3d11_4.h>
#include <d3dcompiler.h>
#include <ffnvcodec/dynlink_loader.h>
#include <jni.h>
#include "jnipp.h"
#include "gl.h"
#include "wgl.h"
extern "C" {
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_d3d11va.h>
#include <libavutil/hwcontext_d3d12va.h>
#include <libavutil/hwcontext_cuda.h>
#include <libswscale/swscale.h>
}
module Media;

import Resource;

using namespace MediaPlayer;

extern "C" int cudaDestroySurfaceObject(void*);

static void SyncFence(ID3D12Fence* fence, uint64_t value, HANDLE event)
{
	for (int i = 0; i < 20; i++) if (fence->GetCompletedValue() >= value) return;
	fence->SetEventOnCompletion(value, event);
	WaitForSingleObject(event, INFINITE);
}

static auto ConvertRGBA(const AVFrame* frame)
{
	auto out = av_frame_alloc();
	auto sws = sws_getContext(frame->width, frame->height, (AVPixelFormat)frame->format, frame->width, frame->height, AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr);
	out->format = AV_PIX_FMT_RGBA;
	out->width = frame->width;
	out->height = frame->height;
	av_frame_get_buffer(out, 1);
	sws_scale(sws, frame->data, frame->linesize, 0, frame->height, out->data, out->linesize);
	sws_freeContext(sws);
	return out;
}

void VideoFrame::UpdateSW(const AVFrame* frame)
{
	auto rgba = ConvertRGBA(frame);
	glBindTexture(GL_TEXTURE_2D, hw_texture);
	if (!init) {
		init = true;
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, rgba->width, rgba->height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
	}
	glPixelStorei(GL_UNPACK_ROW_LENGTH, frame->linesize[0]);
	glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
	glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, rgba->width, rgba->height, GL_RGBA, GL_UNSIGNED_BYTE, rgba->data[0]);
	av_frame_free(&rgba);
}

void VideoFrame::UpdateD3D11(const AVFrame* frame)
{
	auto tex = (ID3D11Texture2D*)frame->data[0];
	auto index = (int64_t)frame->data[1];
	D3D11_TEXTURE2D_DESC desc;
	tex->GetDesc(&desc);
	ComPtr<ID3D11ShaderResourceView> s1, s2;
	D3D11_SHADER_RESOURCE_VIEW_DESC srvDesc{};
	srvDesc.Format = DXGI_FORMAT_R8_UNORM;
	srvDesc.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2DARRAY;
	srvDesc.Texture2DArray.MipLevels = 1;
	srvDesc.Texture2DArray.ArraySize = 1;
	srvDesc.Texture2DArray.FirstArraySlice = index;
	D3D11Device->CreateShaderResourceView(tex, &srvDesc, &s1);
	srvDesc.Format = DXGI_FORMAT_R8G8_UNORM;
	D3D11Device->CreateShaderResourceView(tex, &srvDesc, &s2);
	ID3D11ShaderResourceView* srvs[] = { s1.Get(),s2.Get() };
	if (ComputeResult == nullptr)
	{
		desc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
		desc.ArraySize = 1;
		desc.BindFlags = D3D11_BIND_UNORDERED_ACCESS;
		desc.Usage = D3D11_USAGE_DEFAULT;
		D3D11Device->CreateTexture2D(&desc, nullptr, &ComputeResult);
		D3D11_UNORDERED_ACCESS_VIEW_DESC ud{};
		ud.Format = desc.Format;
		ud.ViewDimension = D3D11_UAV_DIMENSION_TEXTURE2D;
		D3D11Device->CreateUnorderedAccessView(ComputeResult.Get(), &ud, &UAV);
		TextureObject = wglDXRegisterObjectNV(DXNVDevice, ComputeResult.Get(), hw_texture, GL_TEXTURE_2D, WGL_ACCESS_READ_ONLY_NV);
		wglDXLockObjectsNV(DXNVDevice, 1, &TextureObject);
	}
	wglDXUnlockObjectsNV(DXNVDevice, 1, &TextureObject);
	D3D11DeviceContext->CSSetShader(ComputeShader.Get(), nullptr, 0);
	D3D11DeviceContext->CSSetShaderResources(0, 2, srvs);
	D3D11DeviceContext->CSSetUnorderedAccessViews(0, 1, UAV.GetAddressOf(), nullptr);
	D3D11DeviceContext->Dispatch(ceil(desc.Width / 32.f), ceil(desc.Height / 32.f), 1);
	wglDXLockObjectsNV(DXNVDevice, 1, &TextureObject);
}

void VideoFrame::UpdateD3D12(const AVFrame* frame)
{
	auto df = (AVD3D12VAFrame*)frame->data[0];
	auto tex = df->texture;
	auto td = tex->GetDesc();
	D3D12_SHADER_RESOURCE_VIEW_DESC sd{};
	sd.Format = DXGI_FORMAT_R8_UNORM;
	sd.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
	sd.Shader4ComponentMapping = D3D12_ENCODE_SHADER_4_COMPONENT_MAPPING(0, 4, 4, 4);
	sd.Texture2D.MipLevels = 1;
	D3D12Device->CreateShaderResourceView(tex, &sd, SRV0.CPUHandle);
	sd.Format = DXGI_FORMAT_R8G8_UNORM;
	sd.Shader4ComponentMapping = D3D12_ENCODE_SHADER_4_COMPONENT_MAPPING(0, 1, 4, 4);
	sd.Texture2D.PlaneSlice = 1;
	D3D12Device->CreateShaderResourceView(tex, &sd, SRV1.CPUHandle);
	if (OutputBuffer == nullptr)
	{
		D3D12_HEAP_PROPERTIES prop{};
		D3D12_RESOURCE_DESC rd{};
		prop.Type = D3D12_HEAP_TYPE_DEFAULT;
		rd.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
		rd.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
		rd.Width = td.Width;
		rd.Height = td.Height;
		rd.DepthOrArraySize = 1;
		rd.MipLevels = 1;
		rd.SampleDesc.Count = 1;
		rd.Flags = D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;
		D3D12Device->CreateCommittedResource(&prop, D3D12_HEAP_FLAG_SHARED, &rd, D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(OutputBuffer.GetAddressOf()));
		D3D12_UNORDERED_ACCESS_VIEW_DESC ud{};
		ud.Format = rd.Format;
		ud.ViewDimension = D3D12_UAV_DIMENSION_TEXTURE2D;
		D3D12Device->CreateUnorderedAccessView(OutputBuffer.Get(), nullptr, &ud, UAV0.CPUHandle);
		D3D12Device->CreateSharedHandle(OutputBuffer.Get(), nullptr, GENERIC_ALL, nullptr, &SharedHandle);
		glCreateMemoryObjectsEXT(1, &memory);
		glImportMemoryWin32HandleEXT(memory, 0, GL_HANDLE_TYPE_D3D12_RESOURCE_EXT, SharedHandle);
		glTextureStorageMem2DEXT(hw_texture, 1, GL_RGBA8, td.Width, td.Height, memory, 0);
		CommandList->SetComputeRootSignature(RootSignature.Get());
		CommandList->SetDescriptorHeaps(1, DescriptorHeap.GetAddressOf());
		CommandList->SetComputeRootDescriptorTable(0, DescriptorHeap->GetGPUDescriptorHandleForHeapStart());
		CommandList->Dispatch(ceil(td.Width / 32.f), ceil(td.Height / 32.f), 1);
		CommandList->Close();
	}
	CommandQueue->Wait(df->sync_ctx.fence, df->sync_ctx.fence_value);
	CommandQueue->ExecuteCommandLists(1, (ID3D12CommandList**)CommandList.GetAddressOf());
	CommandQueue->Signal(fence.Get(), ++FenceValue);
	SyncFence(fence.Get(), FenceValue, FenceEvent);
}

void VideoFrame::UpdateCUDA(const AVFrame* frame)
{
	void* InitCUDA(void* array);
	int RunCUDACompute(void* y, void* uv, void* output, void* stream, uint32_t width, uint32_t height, uint32_t stepY, uint32_t stepUV);
	CUcontext _;
	cuda->cuCtxPushCurrent(cu_ctx);
	if (!cu_res) {
		glBindTexture(GL_TEXTURE_2D, hw_texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, frame->width, frame->height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
		CUarray array;
		cuda->cuGraphicsGLRegisterImage(&cu_res, hw_texture, GL_TEXTURE_2D, CU_GRAPHICS_REGISTER_FLAGS_SURFACE_LDST);
		cuda->cuGraphicsMapResources(1, &cu_res, stream);
		cuda->cuGraphicsSubResourceGetMappedArray(&array, cu_res, 0, 0);
		output = InitCUDA(array);
	}
	RunCUDACompute(frame->data[0], frame->data[1], output, stream, frame->width, frame->height, frame->linesize[0], frame->linesize[1]);
	cuda->cuCtxPopCurrent(&_);
}

VideoFrame::VideoFrame(const AVHWDeviceType type, const AVHWDeviceContext* hwctx, GLuint tex) : hwtype(type), hw_texture(tex), output(nullptr), cu_res(nullptr), TextureObject(nullptr), FenceValue(0), memory(0), SharedHandle(nullptr), hwaccel(false), init(false)
{
	switch (type)
	{
	case AV_HWDEVICE_TYPE_D3D11VA:
	{
		auto va = (AVD3D11VADeviceContext*)hwctx->hwctx;
		D3D11Device = va->device;
		D3D11DeviceContext = va->device_context;
		DXNVDevice = wglDXOpenDeviceNV(D3D11Device.Get());
		D3D11Device->CreateComputeShader(&Resources::cs, Resources::cs_size, nullptr, &ComputeShader);
		break;
	}
	case AV_HWDEVICE_TYPE_D3D12VA:
	{
		auto va = (AVD3D12VADeviceContext*)hwctx->hwctx;
		D3D12Device = va->device;
		{
			ComPtr<ID3DBlob> rs;
			D3DGetBlobPart(&Resources::cs_dxc, Resources::cs_dxc_size, D3D_BLOB_ROOT_SIGNATURE, 0, &rs);
			D3D12Device->CreateRootSignature(0, rs->GetBufferPointer(), rs->GetBufferSize(), IID_PPV_ARGS(RootSignature.GetAddressOf()));
			D3D12_COMPUTE_PIPELINE_STATE_DESC desc{};
			desc.CS.pShaderBytecode = &Resources::cs_dxc;
			desc.CS.BytecodeLength = Resources::cs_dxc_size;
			desc.pRootSignature = RootSignature.Get();
			D3D12Device->CreateComputePipelineState(&desc, IID_PPV_ARGS(PSO.GetAddressOf()));
		}
		{
			D3D12_COMMAND_QUEUE_DESC desc{};
			desc.Type = D3D12_COMMAND_LIST_TYPE_COMPUTE;
			D3D12Device->CreateCommandQueue(&desc, IID_PPV_ARGS(CommandQueue.GetAddressOf()));
			D3D12Device->CreateCommandAllocator(desc.Type, IID_PPV_ARGS(CommandAllocator.GetAddressOf()));
			D3D12Device->CreateCommandList(0, desc.Type, CommandAllocator.Get(), PSO.Get(), IID_PPV_ARGS(CommandList.GetAddressOf()));
			D3D12Device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(fence.GetAddressOf()));
			FenceEvent = CreateEventA(nullptr, false, false, nullptr);
		}
		{
			D3D12_DESCRIPTOR_HEAP_DESC desc{};
			desc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
			desc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
			desc.NumDescriptors = 3;
			D3D12Device->CreateDescriptorHeap(&desc, IID_PPV_ARGS(DescriptorHeap.GetAddressOf()));
			auto size = D3D12Device->GetDescriptorHandleIncrementSize(desc.Type);
			SRV0.CPUHandle = DescriptorHeap->GetCPUDescriptorHandleForHeapStart();
			SRV0.GPUHandle = DescriptorHeap->GetGPUDescriptorHandleForHeapStart();
			SRV1.CPUHandle = { SRV0.CPUHandle.ptr + size };
			SRV1.GPUHandle = { SRV0.GPUHandle.ptr + size };
			UAV0.CPUHandle = { SRV1.CPUHandle.ptr + size };
			UAV0.GPUHandle = { SRV1.GPUHandle.ptr + size };
		}
		break;
	}
	case AV_HWDEVICE_TYPE_CUDA:
	{
		cuda_load_functions(&cuda, nullptr);
		auto va = (AVCUDADeviceContext*)hwctx->hwctx;
		cu_ctx = va->cuda_ctx;
		stream = va->stream;
		break;
	}
	default:
		break;
	}
}

VideoFrame::~VideoFrame()
{
	if (TextureObject)
	{
		wglDXUnlockObjectsNV(DXNVDevice, 1, &TextureObject);
		wglDXUnregisterObjectNV(DXNVDevice, TextureObject);
		wglDXCloseDeviceNV(DXNVDevice);
	}
	if (memory)
	{
		glDeleteMemoryObjectsEXT(1, &memory);
		CloseHandle(SharedHandle);
		CloseHandle(FenceEvent);
	}
	if (cu_res)
	{
		cudaDestroySurfaceObject(output);
		cuda->cuGraphicsUnmapResources(1, &cu_res, stream);
		cuda->cuGraphicsUnregisterResource(cu_res);
	}
}

void VideoFrame::Update(const AVFrame* frame, bool hwaccel)
{
	this->hwaccel = hwaccel;
	if (hwaccel)
	{
		switch (hwtype)
		{
		case AV_HWDEVICE_TYPE_D3D11VA:
			UpdateD3D11(frame);
			break;
		case AV_HWDEVICE_TYPE_D3D12VA:
			UpdateD3D12(frame);
			break;
		case AV_HWDEVICE_TYPE_CUDA:
			UpdateCUDA(frame);
		default:
			break;
		}
	}
	else UpdateSW(frame);
}

void MediaPlayer::Init(JNIEnv* env, jclass, jlong proc)
{
	auto static init = false;
	if (init) return;
	init = true;
	if (!gladLoadGL((GLADloadfunc)proc)) Throw(env, "init failed");
	if (!gladLoadWGL(wglGetCurrentDC(), (GLADloadfunc)wglGetProcAddress)) Throw(env, "init failed");
}