#!/usr/bin/env bash
set -euo pipefail

# Prevent MSYS2 from converting Docker paths on Windows
export MSYS_NO_PATHCONV=1

# TG WS Proxy Go cross-compilation script for Android ABIs (c-shared)
# Usage: ./build-go.sh [output_dir]
# Requires: Docker
# The script downloads Android NDK automatically inside the container.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${1:-./app/src/main/jniLibs}"
GO_FILE="${SCRIPT_DIR}/tg-ws-proxy.go"

if [ ! -f "$GO_FILE" ]; then
    echo "ERROR: $GO_FILE not found"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

NDK_VERSION="r26b"
NDK_ZIP="android-ndk-${NDK_VERSION}-linux.zip"
NDK_URL="https://dl.google.com/android/repository/${NDK_ZIP}"

echo "=== Building inside Docker (golang:1.23-bookworm) ==="

docker run --rm \
    -v "${SCRIPT_DIR}:/workspace:ro" \
    -v "${OUTPUT_DIR}:/output" \
    -e "NDK_URL=${NDK_URL}" \
    -e "NDK_ZIP=${NDK_ZIP}" \
    -e "NDK_VERSION=${NDK_VERSION}" \
    golang:1.23-bookworm bash -c '
set -euo pipefail

# Install unzip & ca-certificates
apt-get update -qq && apt-get install -y -qq unzip ca-certificates >/dev/null 2>&1

# Download NDK if not cached
NDK_DIR="/tmp/android-ndk-${NDK_VERSION}"
if [ ! -d "$NDK_DIR" ]; then
    echo "=== Downloading Android NDK ${NDK_VERSION} (~600 MB) ==="
    curl -sL -o "/tmp/${NDK_ZIP}" "$NDK_URL"
    unzip -q "/tmp/${NDK_ZIP}" -d /tmp
    rm -f "/tmp/${NDK_ZIP}"
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
    rm -f "${out%.so}.h"
    echo "Built: $out"
}

build_abi arm64 arm64-v8a aarch64-linux-android33-clang
build_abi arm   armeabi-v7a armv7a-linux-androideabi33-clang
build_abi amd64 x86_64   x86_64-linux-android33-clang

echo "=== All binaries built in /output ==="
ls -la /output/*/
'

echo "=== Done ==="
ls -la "${OUTPUT_DIR}"/*/
