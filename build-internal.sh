#!/usr/bin/env bash
set -euo pipefail

apt-get update -qq && apt-get install -y -qq unzip ca-certificates curl >/dev/null 2>&1

NDK_VERSION="r26b"
NDK_ZIP="android-ndk-${NDK_VERSION}-linux.zip"
NDK_URL="https://dl.google.com/android/repository/${NDK_ZIP}"
NDK_DIR="/ndk_cache/android-ndk-${NDK_VERSION}"

if [ ! -d "$NDK_DIR" ]; then
    echo "=== Downloading Android NDK ${NDK_VERSION} (~600 MB) ==="
    mkdir -p /ndk_cache
    curl -# -L -o "/ndk_cache/${NDK_ZIP}" "$NDK_URL"
    unzip -q "/ndk_cache/${NDK_ZIP}" -d /ndk_cache
    rm -f "/ndk_cache/${NDK_ZIP}"
else
    echo "=== Using cached Android NDK ${NDK_VERSION} ==="
fi

TOOLCHAIN="${NDK_DIR}/toolchains/llvm/prebuilt/linux-x86_64/bin"

build_abi() {
    local goarch="$1"
    local abi="$2"
    local clang="$3"

    local out="/output/${abi}/libtgwsproxy.so"
    mkdir -p "$(dirname "$out")"

    export CC="${TOOLCHAIN}/${clang}"
    export CXX="${TOOLCHAIN}/${clang}++"
    export CGO_ENABLED=1
    export GOOS=android
    export GOARCH="$goarch"

    echo "=== Building for $abi ($goarch) ==="
    go build -buildmode=c-shared -ldflags="-s -w" -o "$out" /workspace/tg-ws-proxy.go
    rm -f "${out%.so}.h" "/output/${abi}/libtgwsproxy.h"
    echo "Built: $out"
}

build_abi arm64 arm64-v8a aarch64-linux-android33-clang
build_abi arm   armeabi-v7a armv7a-linux-androideabi33-clang
build_abi amd64 x86_64   x86_64-linux-android33-clang

echo "=== All binaries built in /output ==="
ls -lh /output/*/libtgwsproxy.so
