#!/usr/bin/env bash
# Baixa o núcleo FBNeo (libretro) para Android arm64 e instala como lib do app.
# Rode no SEU computador (precisa de internet): ./scripts/fetch_core.sh
set -euo pipefail
cd "$(dirname "$0")/.."
URL="https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/fbneo_libretro_android.so.zip"
OUT="app/src/main/jniLibs/arm64-v8a"
mkdir -p "$OUT"
tmp="$(mktemp -d)"
curl -fL "$URL" -o "$tmp/core.zip"
unzip -o "$tmp/core.zip" -d "$tmp"
# Android só empacota arquivos lib*.so
mv "$tmp/fbneo_libretro_android.so" "$OUT/libfbneo_libretro_android.so"
rm -rf "$tmp"
echo "Núcleo instalado em $OUT/libfbneo_libretro_android.so"
