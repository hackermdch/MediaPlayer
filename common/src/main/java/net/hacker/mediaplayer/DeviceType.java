package net.hacker.mediaplayer;

public enum DeviceType {
    NONE(0), CUDA(2), D3D11VA(7), D3D12VA(12);

    public final int value;

    DeviceType(int val) {
        value = val;
    }
}
