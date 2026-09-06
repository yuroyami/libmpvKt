# buildscripts

The scripts that cross-compile mpv and its dependencies for Android. `docs/building-natives.md`
explains how to run them. The short version:

    export ANDROID_NDK_HOME=/path/to/ndk/29.0.14206865
    ./download-deps.sh
    ./buildall.sh --arch arm64

Output lands in `../libmpvkt/native-libs/<abi>/`.
