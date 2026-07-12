#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CPP="$ROOT/app/src/main/cpp"
GLIBC_OUT="$ROOT/app/src/main/assets/glibc-x86_64"
X86_OUT="$ROOT/app/src/main/assets/x86_64-libs"

mkdir -p "$GLIBC_OUT" "$X86_OUT"

CFLAGS="-O2 -fPIC -shared"
LDFLAGS="-ldl -lpthread"
build() {
  local src="$1" out="$2" soname="${3:-}"
  local so_opt=""
  [[ -n "$soname" ]] && so_opt="-Wl,-soname,$soname"
  echo "[shim] $src -> $(basename "$out")${soname:+  (soname $soname)}"
  gcc $CFLAGS -o "$out" "$CPP/$src" $so_opt $LDFLAGS
}

build dbus_stub.c             "$GLIBC_OUT/libdbus-1.so.3"         libdbus-1.so.3
build pulse_stub.c             "$GLIBC_OUT/libpulse.so.0"          libpulse.so.0
build pulse_simple_stub.c      "$GLIBC_OUT/libpulse-simple.so.0"   libpulse-simple.so.0
build fmod_sched.c             "$GLIBC_OUT/libfmod_sched.so"
build connect_redirect.c       "$GLIBC_OUT/libconnect_redirect.so"

build dns_resolver.c           "$X86_OUT/libdns_resolver.so"
build unity_crash_fix.c        "$X86_OUT/libunity_crash_fix.so"
build godot_audio.c            "$X86_OUT/libasound.so.2"           libasound.so.2
build ctype_fix.c              "$X86_OUT/libctype_fix.so"
build godot_ctype_fix.c        "$X86_OUT/libgodot_ctype_patch.so"
build pthread_recursive_fix.c  "$X86_OUT/libpthread_recursive_fix.so"
build eaccess.c                "$X86_OUT/libeaccess_shim.so"
