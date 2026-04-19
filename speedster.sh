#!/bin/bash
set -e

echo "🚀 Starting Phase 2 & 3: The Forge and The Crunch"

WORKDIR="dynamic_build"
CLASSES_DIR="$WORKDIR/classes"
DEX_DIR="$WORKDIR/dex"
KOTLIN_DIR="$WORKDIR/kotlinc"
LIBS_DIR="app/build/harvested_libs"

mkdir -p $CLASSES_DIR
mkdir -p $DEX_DIR

if [ ! -d "$KOTLIN_DIR" ]; then
    echo "⬇️ Downloading Kotlin Compiler 1.9.24..."
    wget -q https://github.com/JetBrains/kotlin/releases/download/v1.9.24/kotlin-compiler-1.9.24.zip -O $WORKDIR/kotlin.zip
    unzip -q $WORKDIR/kotlin.zip -d $WORKDIR/
fi

if [ ! -f "$WORKDIR/compose-compiler.jar" ]; then
    echo "⬇️ Downloading Compose Compiler 1.5.14..."
    wget -q https://dl.google.com/dl/android/maven2/androidx/compose/compiler/compiler-hosted/1.5.14/compiler-hosted-1.5.14.jar -O $WORKDIR/compose-compiler.jar
fi

echo "🔗 Building Classpath..."
CP=$(find $LIBS_DIR -name "*.jar" | tr '\n' ':')
ANDROID_SDK_ROOT="/usr/local/lib/android/sdk"
CP="$CP:${ANDROID_SDK_ROOT}/platforms/android-34/android.jar"

echo "⚔️ FORGING (Kotlin -> Bytecode)..."
# Note: We include the Shell's DynamicEntry.kt in the compilation so the payload knows the interface
$WORKDIR/kotlinc/bin/kotlinc \
    dynamic_src/ \
    app/src/main/java/com/speedster/DynamicEntry.kt \
    -cp "$CP" \
    -Xplugin=$WORKDIR/compose-compiler.jar \
    -d $CLASSES_DIR

echo "🔩 CRUNCHING (Bytecode -> Dex)..."
BUILD_TOOLS_DIR=$(ls -d ${ANDROID_SDK_ROOT}/build-tools/34.* | head -1)

$BUILD_TOOLS_DIR/d8 \
    --output $DEX_DIR/ \
    --lib ${ANDROID_SDK_ROOT}/platforms/android-34/android.jar \
    $(find $CLASSES_DIR -name "*.class")

echo "✅ PAYLOAD READY: $DEX_DIR/classes.dex"