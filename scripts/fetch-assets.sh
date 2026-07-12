#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ASSETS="$ROOT/app/src/main/assets"
mkdir -p "$ASSETS"

ROOTFS_URL="https://github.com/cetotos/PolyDroid2/releases/download/rootfs-1/rootfs.tar.xz"
ROOTFS_SHA="2a61930a4c2a8efe780a935f84df947640407594b8f9ec8bba960afb5bcd7d34"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "Fetching rootfs..."
curl --fail --location --retry 3 --retry-delay 5 -o "$TMP/rootfs.tar.xz" "$ROOTFS_URL"
echo "$ROOTFS_SHA  $TMP/rootfs.tar.xz" | sha256sum -c -
mv "$TMP/rootfs.tar.xz" "$ASSETS/rootfs.tar.xz"

echo "Done"
