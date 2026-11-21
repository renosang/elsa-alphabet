#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
platform=${ANDROID_PLATFORM_JAR:-"$sdk/platforms/android-35/android.jar"}
tools=${ANDROID_BUILD_TOOLS:-"$sdk/build-tools/35.0.0"}
output=${1:-"$PWD/build/vuong-quoc-tuyet.apk"}
if [[ ! -r "$platform" || ! -x "$tools/aapt2" ]]; then
  echo "Set ANDROID_SDK_ROOT to a local SDK with platform 35 and Build Tools 35.0.0." >&2
  exit 1
fi

work=$(mktemp -d "$PWD/.build.XXXXXXXX")
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes" "$work/dex" "$(dirname "$output")" build
"$tools/aapt2" compile --dir res -o "$work/resources.zip"
"$tools/aapt2" link -I "$platform" --manifest AndroidManifest.xml -A assets \
  --java "$work" -o "$work/base.apk" "$work/resources.zip"
find src "$work/vn" -name '*.java' -print > "$work/sources"
classpath="$platform"
if [[ -n "${ANDROID_ECJ_JAR:-}" ]]; then
  java -jar "$ANDROID_ECJ_JAR" -source 8 -target 8 -encoding UTF-8 \
    -classpath "$classpath" -d "$work/classes" @"$work/sources"
else
  javac --release 8 -encoding UTF-8 -classpath "$classpath" -d "$work/classes" @"$work/sources"
fi
(cd "$work/classes" && zip -qr "$work/code.jar" .)
"$tools/d8" --release --min-api 23 --lib "$platform" --output "$work/dex" \
  "$work/code.jar"
(cd "$work/dex" && zip -q -6 "$work/base.apk" classes.dex)
"$tools/zipalign" -f -P 16 4 "$work/base.apk" "$work/aligned.apk"
keystore=${APK_KEYSTORE:-"$PWD/build/debug.keystore"}
if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
    -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000
fi
"$tools/apksigner" sign --ks "$keystore" --ks-key-alias androiddebugkey --ks-pass pass:android \
  --v4-signing-enabled false --out "$output" "$work/aligned.apk"
"$tools/apksigner" verify --verbose "$output"
