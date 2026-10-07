> [!IMPORTANT]
> PolyDroid 2 is **not** an official client, and it is still in beta! Expect bugs/missing features, and report them in the issues tab located [here](https://github.com/cetotos/PolyDroid2/issues).


# PolyDroid 2

[![Discord](https://img.shields.io/discord/1495088499383210226?label=Discord&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/9NK3zUVkZv)

PolyDroid 2 is an updated, more native rewrite of PolyDroid, which runs the Polytoria Client on Android.
While PolyDroid ran the game with the Windows client, using multiple translation layers like DXVK and Wine, PolyDroid 2 uses the Linux client and only 1 translation layer being [Box64](https://github.com/ptitSeb/box64) to translate x86-64 to ARM64.

## Installation

1. Download and install the APK from [GitHub Releases](https://github.com/cetotos/PolyDroid2/releases/latest)
2. Launch the app and login *(it will be logged in already if you have logged in on Chrome before)*
3. You will need to wait a bit for rootfs and client to extract and deploy.
4. Change the settings however you want
5. Launch any 1.0 or 2.0 game (2.0 recommended)

## FAQ

### Is this against Polytoria rules?

**No.** PolyDroid 2 doesn't modify the client in any way that gives you advantages.
Client is still technically modified to patch Unity itself, which isn't considered cheating.

### Is this a virus? I'm skeptical.

**No.** If you want, you can check the source code yourself.
Login is handled by Google Chrome, and the app no longer requires full file permissions as it doesn't copy to /sdcard

**Permissions the app has are:**

- Wake lock *(for keeping the screen on mid-game)*
- Basic internet access *(for the Polytoria Client itself)*

### Why is it so slow?

To optimize the game, you can lower the graphics settings either in-game or in the in-app settings menu.

Because Unity's Vulkan support is bad and Polytoria itself doesnt have a render distance, looking at a lot of 3D objects in 1.0, even if not visible on screen will tank performance. Heavy games with high part counts will run poorly!

### Why not just wait for the real mobile release?

This app was made before the real mobile release was announced, and it is true after 2.0 mobile comes out this app will be mostly abandoned, but 2.0 mobile is still months away.
Also, PolyDroid 2 can be used for 1.0 mobile until 1.0 games get deleted

### Credits

- Ubuntu Jammy RootFs (both ARM64 and x86) ([proot-me.github.io](https://proot-me.github.io/#downloads))
- Box64 ([github.com/ptitSeb/box64](https://github.com/ptitSeb/box64))
- Mesa/Turnip ([mesa3d.org](https://www.mesa3d.org/))
- Termux:X11 ([github.com/termux/termux-x11](https://github.com/termux/termux-x11))
- Winlator (used for reference) ([github.com/brunodev85/winlator](https://github.com/brunodev85/winlator))

*Special thanks to Polytoria community :)*
