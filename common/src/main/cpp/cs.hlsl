Texture2D texY : register(t0);
Texture2D texUV : register(t1);
RWTexture2D<unorm float4> output : register(u0);

float3 YUVToRGB(float3 yuv)
{
    static const float3x3 mat =
    {
        1.164383f, 1.164383f, 1.164383f,
		0.000000f, -0.391762f, 2.017232f,
		1.596027f, -0.812968f, 0.000000f
    };
    yuv -= float3(0.062745f, 0.501960f, 0.501960f);
    yuv = mul(yuv, mat);
    return saturate(yuv);
}

[RootSignature("DescriptorTable(SRV(t0,numDescriptors=2,flags=DESCRIPTORS_VOLATILE),UAV(u0))")]
[numthreads(32, 32, 1)]
void main(uint3 tid : SV_DispatchThreadID)
{
    float y = texY[tid.xy];
    float2 uv = texUV[tid.xy / 2];
    output[tid.xy] = float4(YUVToRGB(float3(y, uv)), 1.0);
}