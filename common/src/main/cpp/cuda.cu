#ifndef __CUDACC__
#define __CUDACC__ 1
#endif
#include <cuda_runtime.h>
#include <device_launch_parameters.h>

__device__ __forceinline uint8_t ClampColor(const int v)
{
	if (v <= 0) return 0;
	if (v >= 255) return 255;
	return v;
}

__global__ void Compute(const uint8_t* texY, const uint8_t* texUV, const cudaSurfaceObject_t output, const uint32_t width, const uint32_t height, const uint32_t stepY, const uint32_t stepUV)
{
	const auto idx = blockIdx.x * blockDim.x + threadIdx.x;
	const auto idy = blockIdx.y * blockDim.y + threadIdx.y;
	if (idx >= width || idy >= height) return;
	const auto indexY = idx + idy * stepY;
	uint32_t indexU, indexV;
	if (idx % 2 == 0)
	{
		indexU = idy / 2 * stepUV + idx;
		indexV = idy / 2 * stepUV + idx + 1;
	}
	else
	{
		indexV = idy / 2 * stepUV + idx;
		indexU = idy / 2 * stepUV + idx - 1;
	}
	const auto y = texY[indexY], u = texUV[indexU], v = texUV[indexV];
	uchar4 color;
	color.x = ClampColor(1.164383 * (y - 16) + 1.596027 * (v - 128));
	color.y = ClampColor(1.164383 * (y - 16) - 0.812968 * (v - 128) - 0.391762 * (u - 128));
	color.z = ClampColor(1.164383 * (y - 16) + 2.017232 * (u - 128));
	color.w = 255;
	surf2Dwrite(color, output, idx * 4, idy);
}

int RunCUDACompute(void* y, void* uv, void* output, void* stream, const uint32_t width, const uint32_t height, const uint32_t stepY, const uint32_t stepUV)
{
	dim3 gs((width + 31) / 32, (height + 31) / 32), bs(32, 32, 1);
	Compute<<<gs, bs, 0, (CUstream_st*)stream >>>((uint8_t*)y, (uint8_t*)uv, (cudaSurfaceObject_t)output, width, height, stepY, stepUV);
	return cudaStreamSynchronize((cudaStream_t)stream);
}

void* InitCUDA(void* array)
{
	cudaResourceDesc resDesc;
	resDesc.resType = cudaResourceTypeArray;
	resDesc.res.array.array = (cudaArray_t)array;
	cudaSurfaceObject_t surface;
	cudaCreateSurfaceObject(&surface, &resDesc);
	return (void*)surface;
}