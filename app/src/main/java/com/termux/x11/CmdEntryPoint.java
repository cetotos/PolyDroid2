package com.termux.x11;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Keep;

@Keep
public class CmdEntryPoint extends ICmdEntryInterface.Stub {
    private static final String TAG = "CmdEntryPoint";

    public static native boolean start(String[] args);

    @Override
    public native ParcelFileDescriptor getXConnection();

    @Override
    public ParcelFileDescriptor getLogcatOutput() {
        // Not used in embedded mode
        return null;
    }

    public static native boolean connected();
    @SuppressWarnings("unused")
    private void sendBroadcast() {
        Log.d(TAG, "GUI client connected to X server");
    }

    public native void listenForConnections();

    public void spawnListeningThread() {
        new Thread(this::listenForConnections, "xlorie-listener").start();
    }

    static {
        try {
            System.loadLibrary("Xlorie");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load libXlorie.so", e);
        }
    }
}
