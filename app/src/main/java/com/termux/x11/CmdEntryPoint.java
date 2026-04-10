package com.termux.x11;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Keep;

/**
 * Stripped-down CmdEntryPoint for embedded use in PolyDroid2.
 * Only exposes the native start() method to launch the Xorg server in-process.
 * The full Termux:X11 version supports command-line invocation via app_process,
 * broadcast IPC, etc. — none of that is needed here.
 */
@Keep
public class CmdEntryPoint extends ICmdEntryInterface.Stub {
    private static final String TAG = "CmdEntryPoint";

    /**
     * Start the native X server. Call from a background thread.
     * The server creates its socket at $TMPDIR/.X11-unix/X<display>.
     * Ensure TMPDIR and XKB_CONFIG_ROOT are set before calling.
     *
     * @param args X server arguments, e.g. {":0"} for display 0
     * @return true if the server started successfully
     */
    public static native boolean start(String[] args);

    @Override
    public native ParcelFileDescriptor getXConnection();

    @Override
    public ParcelFileDescriptor getLogcatOutput() {
        // Not used in embedded mode
        return null;
    }

    /** Check if the GUI side is connected to the X server. */
    public static native boolean connected();

    /**
     * Called by native listenForConnections() when a GUI client connects.
     * In the full Termux:X11, this broadcasts an intent. We don't need that.
     */
    @SuppressWarnings("unused")
    private void sendBroadcast() {
        Log.d(TAG, "GUI client connected to X server");
    }

    /**
     * Listen for GUI connections on TCP port 7892.
     * The X server sends framebuffer/clipboard events through this connection,
     * and receives input events from the GUI (LorieView).
     * Must be called after start() — blocks until the server shuts down.
     */
    public native void listenForConnections();

    /**
     * Start listening for GUI connections in a background thread.
     */
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
