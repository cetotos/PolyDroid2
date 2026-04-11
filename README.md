# PolyDroid 2

PolyDroid 2 is an updated, more native rewrite of PolyDroid, which runs the Polytoria Client on Android.
While PolyDroid ran the game with the Windows client, using multiple translation layers, PolyDroid 2 uses the Linux client and only 1 translation layer being Box64.

## Installation

1. Download and install the APK from [GitHub Releases](https://github.com/cetotos/PolyDroid2/releases/latest)
2. Launch the app, login (it will be logged in already if you logged in on Chrome before), and launch a game.
3. You will need to wait a bit for rootfs and client to extract and deploy (it will be on a black screen).

## FAQ

### Is this against Polytoria rules?

No. PolyDroid 2 doesn't modify the client in any way that gives you advantages.
Client is still technically modified to patch Unity itself, which isn't considered cheating.

### Is this a virus? / ip grabber

No. If you want, you can check the source code yourself.
Login is handled by Google Chrome, and the app no longer requires full file permissions as it doesn't copy to /sdcard

### Why won't it run? I'm stuck on a black screen.

Unlike PolyDroid, PolyDroid 2 is written from scratch. Because it's still in beta, it is not tested on many devices.
Over time more patches should come out (like Mali support), but it is still very fragile and you still need a good phone with a GPU that has good Vulkan support, and a good amount of RAM.

### Why is it so slow? It's slower than PolyDroid 1!

To optimize the game, go to Polytoria graphics settings and:

Turn on fullscreen (this gives a huge performance boost)
Turn off V-Sync
Turn off post-processing
You can also lower quality of everything else, but they don't do much of a difference

Because Unity's Vulkan support is bad and Polytoria itself doesnt have a render distance, looking at a lot of 3D objects, even if not visible on screen will tank performance. Heavy games with high part counts will run poorly!

### Credits

- Ubuntu Jammy RootFs (both ARM64 and x86) ([proot-me.github.io](https://proot-me.github.io/#downloads))
- Box64 ([github.com/ptitSeb/box64](https://github.com/ptitSeb/box64))
- Mesa/Turnip ([mesa3d.org](https://www.mesa3d.org/))
- Termux:X11 ([github.com/termux/termux-x11](https://github.com/termux/termux-x11))
- Winlator (used for reference) ([github.com/brunodev85/winlator](https://github.com/brunodev85/winlator))

Special thanks to Polytoria community :)
