package com.termux.x11;

import android.app.Activity;
import android.content.ContextWrapper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.inputmethod.InputMethodManager;
public class MainActivity extends ContextWrapper {
    public static final String ACTION_CUSTOM = "com.termux.x11.ACTION_CUSTOM";

    private static MainActivity instance;
    private final Activity hostActivity;
    private LorieView lorieView;

    public boolean useTermuxEKBarBehaviour = false;
    public Object mExtraKeys = null;

    public MainActivity(Activity hostActivity) {
        super(hostActivity);
        this.hostActivity = hostActivity;
        instance = this;
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public Activity getHostActivity() {
        return hostActivity;
    }

    public LorieView getLorieView() {
        return lorieView;
    }

    public void setLorieView(LorieView view) {
        this.lorieView = view;
    }

    public void clientConnectedStateChanged() {
        hostActivity.runOnUiThread(() -> {
            boolean connected = LorieView.connected();
            android.util.Log.i("PolyDroid2", "X client connection state changed: " + (connected ? "connected" : "disconnected"));
            if (!connected) {
                // reconnect
                LorieView.requestConnection();
            }
        });
    }

    public void setExternalKeyboardConnected(boolean connected) {}

    public void toggleExtraKeys() {}

    public boolean handleKey(android.view.KeyEvent event) {
        // Let the input handler in GameActivity handle it
        return false;
    }

    public void runOnUiThread(Runnable action) {
        hostActivity.runOnUiThread(action);
    }

    public static void toggleKeyboardVisibility(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void setCapturingEnabled(boolean enabled) {
        if (instance != null && instance.lorieView != null) {
            if (enabled)
                instance.lorieView.requestPointerCapture();
            else
                instance.lorieView.releasePointerCapture();
        }
    }

    public static void getRealMetrics(DisplayMetrics metrics) {
        if (instance != null && instance.hostActivity != null) {
            Display display = instance.hostActivity.getWindowManager().getDefaultDisplay();
            display.getRealMetrics(metrics);
        }
    }

    public static Prefs prefs = new Prefs();

    public static Prefs getPrefs() {
        return prefs;
    }

    public static boolean isConnected() {
        return instance != null && instance.lorieView != null && LorieView.connected();
    }

    public void finish() {
        hostActivity.finish();
    }

    public String getPackageName() {
        return hostActivity.getPackageName();
    }

    public android.content.res.Resources getResources() {
        return hostActivity.getResources();
    }

    public android.content.ComponentName getComponentName() {
        return hostActivity.getComponentName();
    }
    public void startActivity(android.content.Intent intent) {
        hostActivity.startActivity(intent);
    }

    public Object getSystemService(String name) {
        return hostActivity.getSystemService(name);
    }

    public boolean hasWindowFocus() {
        return hostActivity.hasWindowFocus();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        // no-op
    }

    public android.view.Window getWindow() {
        return hostActivity.getWindow();
    }
}
