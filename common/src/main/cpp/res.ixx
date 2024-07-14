module;
#define RES extern "C"
export module Resource;

export namespace Resources
{
	RES unsigned long long cs_size;
	RES char cs;
	RES unsigned long long cs_dxc_size;
	RES char cs_dxc;
}