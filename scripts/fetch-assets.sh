#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ASSETS="$ROOT/app/src/main/assets"
mkdir -p "$ASSETS"

CLIENT_URL="https://cdn.polytoria.com/releases/client/linux/osAnQymiD-qptghuXu-6yU-R2vVTEQoK.7z"
CLIENT_SHA="576cf8c21bb985476630de79d67a2b47ac6bcc87b3d9d382f932799a391162cd"

ROOTFS_URL="https://github.com/cetotos/PolyDroid2/releases/download/rootfs-1/rootfs.tar.xz"
ROOTFS_SHA="2a61930a4c2a8efe780a935f84df947640407594b8f9ec8bba960afb5bcd7d34"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "Fetching Client..."
curl --fail --location --retry 3 --retry-delay 5 -o "$TMP/client.7z" "$CLIENT_URL"
echo "$CLIENT_SHA  $TMP/client.7z" | sha256sum -c -

echo "Repacking Client..."
mkdir "$TMP/client"
7z x -y -o"$TMP/client" "$TMP/client.7z" > /dev/null
tar -cJf "$TMP/polytoria_client.txz" -C "$TMP/client" .
mv "$TMP/polytoria_client.txz" "$ASSETS/polytoria_client.txz"

echo "Fetching rootfs..."
curl --fail --location --retry 3 --retry-delay 5 -o "$TMP/rootfs.tar.xz" "$ROOTFS_URL"
echo "$ROOTFS_SHA  $TMP/rootfs.tar.xz" | sha256sum -c -
mv "$TMP/rootfs.tar.xz" "$ASSETS/rootfs.tar.xz"

echo "Done"
