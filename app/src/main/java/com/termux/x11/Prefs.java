package com.termux.x11;

import java.util.HashMap;
import java.util.Map;
public class Prefs {
    public final PrefValue<String> touchMode = new PrefValue<>("3"); // TOUCH (direct)
    public final PrefValue<String> displayResolutionMode = new PrefValue<>("native");
    public final PrefValue<Integer> displayScale = new PrefValue<>(100);
    public final PrefValue<String> displayResolutionExact = new PrefValue<>("1280x720");
    public final PrefValue<String> displayResolutionCustom = new PrefValue<>("1280x720");
    public final PrefValue<Boolean> displayStretch = new PrefValue<>(true);
    public final PrefValue<Boolean> adjustResolution = new PrefValue<>(false);
    public final PrefValue<Boolean> fullscreen = new PrefValue<>(true);
    public final PrefValue<Boolean> clipboardEnable = new PrefValue<>(false);
    public final PrefValue<Boolean> hardwareKbdScancodesWorkaround = new PrefValue<>(false);
    public final PrefValue<Boolean> enforceCharBasedInput = new PrefValue<>(false);
    public final PrefValue<Boolean> showMouseHelper = new PrefValue<>(false);
    public final PrefValue<Boolean> tapToMove = new PrefValue<>(false);
    public final PrefValue<Boolean> preferScancodes = new PrefValue<>(false);
    public final PrefValue<Boolean> pointerCapture = new PrefValue<>(false);
    public final PrefValue<Boolean> scaleTouchpad = new PrefValue<>(false);
    public final PrefValue<Integer> capturedPointerSpeedFactor = new PrefValue<>(100);
    public final PrefValue<Boolean> dexMetaKeyCapture = new PrefValue<>(false);
    public final PrefValue<Boolean> stylusIsMouse = new PrefValue<>(false);
    public final PrefValue<Boolean> stylusButtonContactModifierMode = new PrefValue<>(false);
    public final PrefValue<Boolean> pauseKeyInterceptingWithEsc = new PrefValue<>(false);
    public final PrefValue<String> transformCapturedPointer = new PrefValue<>("no");

    public final Map<String, Object> keys = new HashMap<>();

    public static class PrefValue<T> {
        private T value;

        public PrefValue(T defaultValue) {
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public void put(T value) {
            this.value = value;
        }
    }
}
