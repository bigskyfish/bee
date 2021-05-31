package com.floatcloud.beefz.util;

import com.jcraft.jsch.SftpProgressMonitor;

public class MyProgressMonitor implements SftpProgressMonitor {
    private long transferIndex;
    @Override
    public void init(int op, String src, String dest, long max) {
        System.out.println("transferring begin ...");
    }

    @Override
    public boolean count(long count) {
        transferIndex += count;
        System.out.printf("当前转化大小%s", transferIndex);
        return true;
    }

    @Override
    public void end() {
        System.out.println("transferring end ...");
    }
}
