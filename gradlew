#!/bin/sh
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then
    exec java -cp "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
else
    echo "gradle-wrapper.jar not found. Using system gradle..."
    exec gradle "$@"
fi
